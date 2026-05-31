package com.tboat.service;

import com.tboat.ServerMain;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.BidResult;
import com.tboat.models.auction.History;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.network.Response;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.ClientSession;
import com.tboat.socket.EventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;

public class AuctionRoom {

    private static final Logger logger = LoggerFactory.getLogger(AuctionRoom.class);
    private static final double NO_AUTO_BID_MAX = -1.0;
    private static final int AUTO_BID_SAFETY_MULTIPLIER = 200;
    private static final int AUTO_BID_SAFETY_OFFSET = 10;
    private static final double BUY_NOW_DISABLED_PRICE = 0.0;
    private static final long SNIPE_THRESHOLD_SECONDS = 15;
    private static final int SNIPE_EXTENSION_SECONDS = 30;
    private static final double ADMIN_COMMISSION_RATE = 0.10;
    private static final String ADMIN_ACCOUNT = "admin";

    /*
     * Các lệnh auto-bid được xếp theo giá tối đa trước, rồi đến thời điểm đăng ký.
     * Cách này giúp chọn người đặt tự động ổn định khi nhiều user có cùng maxBid.
     */
    private record AutoBidEntry(String account, double maxBid, long registeredAt)
            implements Comparable<AutoBidEntry> {
        @Override
        public int compareTo(AutoBidEntry other) {
            int maxBidOrder = Double.compare(other.maxBid, this.maxBid);
            return maxBidOrder != 0 ? maxBidOrder : Long.compare(this.registeredAt, other.registeredAt);
        }
    }

    private final int    sessionId;
    private double       currentPrice;
    private String       lastBidder;
    private boolean      isFinished = false;

    private final Map<String, AutoBidEntry>           autoBidMap   = new HashMap<>();
    private final PriorityBlockingQueue<AutoBidEntry> autoBidQueue = new PriorityBlockingQueue<>();

    private final List<ClientSession> subscribers    = new CopyOnWriteArrayList<>();
    private final AuctionSessionDAO   sessionDAO     = new AuctionSessionDAO();
    private final UserDAO             userDAO        = new UserDAO();
    private final HistoryDAO          historyDAO     = new HistoryDAO();
    private final BiddingService      biddingService = new BiddingService();

    public AuctionRoom(int sessionId, double startingPrice) {
        this.sessionId    = sessionId;
        this.currentPrice = startingPrice;
    }

    /*
     * Bid thường phải được lưu vào cơ sở dữ liệu trước. Chỉ khi cơ sở dữ liệu chấp nhận,
     * phòng mới cập nhật trạng thái và cho auto-bid phản ứng với giá mới.
     */
    public synchronized BidResult placeBid(double newPrice, String bidderAccount) {
        if (isFinished) return BidResult.DB_ERROR;

        BidResult result = biddingService.placeBid(bidderAccount, sessionId, newPrice);
        if (result != BidResult.OK) return result;

        currentPrice = newPrice;
        lastBidder   = bidderAccount;
        checkAndExtendIfSnipe();
        return BidResult.OK;
    }

    public synchronized double getAutoBidMax(String account) {
        AutoBidEntry entry = autoBidMap.get(account);
        return entry != null ? entry.maxBid() : NO_AUTO_BID_MAX;
    }
    /*
     * Auto-bid chạy trong khóa của phòng. safetyLimit giúp tránh vòng lặp vô hạn
     * nếu dữ liệu trong room bị lệch trạng thái.
     */
    public synchronized void triggerAutoBids() {
        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null || isFinished) return;

        double bidStep = session.getBidIncrease();

        Set<String> outAccounts = new HashSet<>();
        String  lastAutoLeader = lastBidder;
        double  lastAutoPrice  = currentPrice;
        boolean hasAutoBid     = false;

        int safetyLimit = autoBidMap.size() * AUTO_BID_SAFETY_MULTIPLIER + AUTO_BID_SAFETY_OFFSET;

        while (!isFinished && safetyLimit-- > 0) {
            double nextPrice = currentPrice + bidStep;

            expireAutoBidsBelow(nextPrice, outAccounts, session);

            AutoBidEntry challenger = autoBidMap.values().stream()
                    .filter(autoBid -> !autoBid.account().equals(lastBidder)
                            && autoBid.maxBid() >= nextPrice)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            if (challenger == null) break;

            BidResult result = biddingService.placeBid(challenger.account(), sessionId, nextPrice);

            switch (result) {
                case OK -> {
                    String previousLeader = lastBidder;
                    currentPrice = nextPrice;
                    lastBidder   = challenger.account();
                    lastAutoLeader = challenger.account();
                    lastAutoPrice  = nextPrice;
                    hasAutoBid     = true;
                    checkAndExtendIfSnipe();
                    if (previousLeader != null && !previousLeader.isBlank()
                            && !previousLeader.equals(challenger.account())) {
                        NotificationService.getInstance()
                                .onAutoBidPlaced(session, challenger.account(), nextPrice);
                        NotificationService.getInstance()
                                .onOutbid(session, previousLeader, nextPrice);
                    }

                    if (session.getBuyNowPrice() > BUY_NOW_DISABLED_PRICE
                            && nextPrice >= session.getBuyNowPrice()) {
                        finishAuction();
                        return;
                    }
                }
                case INSUFFICIENT_BALANCE -> {
                    logger.info("[AutoBid] {} hết số dư, loại khỏi queue", challenger.account());
                    outAccounts.add(challenger.account());
                    notifyAutoBidOut(challenger.account(), session);
                    removeAutoBid(challenger.account());
                }
                default -> {
                    logger.warn("[AutoBid] Loại {} do result={}", challenger.account(), result);
                    removeAutoBid(challenger.account());
                }
            }
        }

        // Chỉ gửi một update sau vòng lặp để client không vẽ lại theo từng bước auto-bid.
        if (hasAutoBid) {
            String leaderName = userDAO.getNickname(lastAutoLeader);
            com.google.gson.JsonObject bidPayload = new com.google.gson.JsonObject();
            bidPayload.addProperty("newPrice",          lastAutoPrice);
            bidPayload.addProperty("newLeader",         lastAutoLeader);
            bidPayload.addProperty("newLeaderNickname", leaderName);
            bidPayload.addProperty("bidTime",           LocalDateTime.now().toString());
            broadcast(ServerEvent.NEW_BID, leaderName + " (auto) vừa đặt giá mới", bidPayload);
            logger.info("[AutoBid] Kết thúc loop — leader: {} @ {}", lastAutoLeader, lastAutoPrice);
        }
    }

    private void expireAutoBidsBelow(double nextPrice, Set<String> outAccounts, AuctionSession session) {
        List<String> expiredAccounts = autoBidMap.values().stream()
                .filter(autoBid -> autoBid.maxBid() < nextPrice
                        && !outAccounts.contains(autoBid.account()))
                .map(AutoBidEntry::account)
                .toList();

        for (String account : expiredAccounts) {
            outAccounts.add(account);
            notifyAutoBidOut(account, session);
            removeAutoBid(account);
        }
    }

    private void removeAutoBid(String account) {
        AutoBidEntry removed = autoBidMap.remove(account);
        if (removed != null) autoBidQueue.remove(removed);
    }

    private void notifyAutoBidOut(String account, AuctionSession session) {
        NotificationService.getInstance().onAutoBidOut(session, account, currentPrice);
        subscribers.stream()
                .filter(client -> account.equals(client.getClientId()))
                .findFirst()
                .ifPresent(client -> {
                    try {
                        client.sendSystemMessage(ServerEvent.AUTO_BID_OUT,
                                "Giá hiện tại đã vượt mức tối đa của bạn. Auto-bid đã dừng.", null);
                    } catch (Exception e) {
                        logger.warn("[AutoBid] Không thể notify out cho {}", account);
                    }
                });
    }

    public synchronized void registerAutoBid(String account, double maxBid) {
        if (isFinished) return;

        AutoBidEntry oldEntry = autoBidMap.get(account);
        if (oldEntry != null) autoBidQueue.remove(oldEntry);

        AutoBidEntry newEntry = new AutoBidEntry(account, maxBid, System.nanoTime());
        autoBidMap.put(account, newEntry);
        autoBidQueue.add(newEntry);

        logger.info("[AutoBid] {} đăng ký auto-bid maxBid={} cho phiên {}", account, maxBid, sessionId);
        triggerAutoBids();
    }

    /*
     * Chống đặt giá sát giờ chót: nếu có bid ở vài giây cuối, kéo dài phiên để
     * những người khác vẫn có cơ hội phản hồi.
     */
    private void checkAndExtendIfSnipe() {
        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null || session.getEndTime() == null) return;

        long secondsLeft = java.time.Duration.between(
                LocalDateTime.now(), session.getEndTime()).getSeconds();

        if (secondsLeft < SNIPE_THRESHOLD_SECONDS) {
            AuctionTimerService.getInstance().extendAuction(sessionId, SNIPE_EXTENSION_SECONDS);
        }
    }

    /*
     * Kết thúc phiên được thiết kế để gọi lặp vẫn cho cùng kết quả: khi isFinished đã true thì mọi lần gọi sau
     * sẽ trả về ngay. Điều này cần thiết vì timer, mua ngay và thao tác tay có thể chạy gần nhau.
     */
    public synchronized void finishAuction() {
        if (isFinished) return;
        isFinished = true;
        autoBidQueue.clear();
        autoBidMap.clear();

        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null) {
            logger.error("[CRITICAL] Không tìm thấy phiên {} trong DB!", sessionId);
            AuctionManager.getInstance().removeRoom(sessionId);
            return;
        }

        if (lastBidder != null) finishWithWinner(session);
        else                    finishWithNoWinner();

        AuctionManager.getInstance().removeRoom(sessionId);
    }

    private void finishWithWinner(AuctionSession session) {
        String seller         = session.getSellerAccountName();
        String winnerNickname = resolveNickname(lastBidder);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            boolean isSaved = sessionDAO.updateSessionStatus(connection, sessionId, StatusOfAuction.ENDED)
                    && historyDAO.addHistory(connection,
                    new History(sessionId, lastBidder, currentPrice, LocalDateTime.now()))
                    && divideMoney(connection, seller, currentPrice);

            if (isSaved) {
                connection.commit();
                onWinnerCommitted(session, winnerNickname);
            } else {
                connection.rollback();
                logger.error("[CRITICAL] Lỗi chốt phiên {}, đã Rollback!", sessionId);
            }

        } catch (Exception e) {
            logger.error("[CRITICAL] Ngoại lệ khi chốt phiên {}", sessionId, e);
        }
    }

    private void onWinnerCommitted(AuctionSession session, String winnerNickname) {
        Map<String, Object> finishPayload = new HashMap<>();
        finishPayload.put("winner",         lastBidder);
        finishPayload.put("winnerNickname", winnerNickname);
        finishPayload.put("finalPrice",     currentPrice);

        broadcast(ServerEvent.AUCTION_FINISHED, "Phiên đấu giá kết thúc thành công!", finishPayload);
        NotificationService.getInstance().onAuctionWon(session, lastBidder, currentPrice);
        NotificationService.getInstance().onAuctionSold(session, lastBidder, currentPrice);
        broadcastGlobalFinished(finishPayload);

        logger.info("[Server] Chốt phiên {} — người thắng: {} ({})",
                sessionId, winnerNickname, lastBidder);
    }

    private void finishWithNoWinner() {
        sessionDAO.updateSessionStatus(sessionId, StatusOfAuction.ENDED);
        broadcast(ServerEvent.AUCTION_FINISHED, "Kết thúc, không có người thắng", null);
        broadcastGlobalFinished(null);
        logger.info("[Server] Phiên {} kết thúc, không có người thắng.", sessionId);
    }

    private void broadcastGlobalFinished(Object payload) {
        EventBroadcaster.getInstance().broadcastToAll(
                new Response<>(ServerEvent.AUCTION_FINISHED, ServerEvent.NOTIFY,
                        "Phiên " + sessionId + " đã kết thúc", payload));
        EventBroadcaster.getInstance().broadcastToAll(
                new Response<>(ServerEvent.RELOAD_AVAILABLE, ServerEvent.NOTIFY,
                        "Cập nhật danh sách phiên", sessionId));
        EventBroadcaster.getInstance().broadcastToAdmins(
                new Response<>(ServerEvent.RELOAD_ALL_ITEMS, ServerEvent.NOTIFY,
                        "Phiên " + sessionId + " vừa kết thúc", sessionId));
    }

    private String resolveNickname(String account) {
        try {
            String name = userDAO.getUser(account).getNickname();
            return (name == null || name.isBlank()) ? account : name;
        } catch (Exception e) { return account; }
    }

    /*
     * Chia tiền sau khi có người thắng: 10% cho admin, phần còn lại cho seller.
     */
    private boolean divideMoney(Connection connection, String sellerAccount,
                                double totalAmount) throws SQLException {
        double adminFee      = totalAmount * ADMIN_COMMISSION_RATE;
        double sellerRevenue = totalAmount - adminFee;
        boolean isPaid = userDAO.updateBalance(connection, ADMIN_ACCOUNT, adminFee)
                && userDAO.updateBalance(connection, sellerAccount, sellerRevenue);
        if (isPaid) logger.info("[Payment] Admin: +{} | Seller: +{}", adminFee, sellerRevenue);
        return isPaid;
    }

    public void broadcast(ServerEvent action, String message, Object payload) {
        for (ClientSession client : subscribers) {
            CompletableFuture.runAsync(() -> {
                try { client.sendSystemMessage(action, message, payload); }
                catch (Exception e) {
                    logger.error("Lỗi broadcast tới client trong phiên {}", sessionId);
                }
            }, ServerMain.broadcastExecutor);
        }
    }

    public void addSubscriber(ClientSession client)    { subscribers.add(client); }
    public void removeSubscriber(ClientSession client) { subscribers.remove(client); }
    public double  getCurrentPrice() { return currentPrice; }
    public String  getLastBidder()   { return lastBidder; }
    public int     getSessionId()    { return sessionId; }
    public boolean isFinished()      { return isFinished; }
}
