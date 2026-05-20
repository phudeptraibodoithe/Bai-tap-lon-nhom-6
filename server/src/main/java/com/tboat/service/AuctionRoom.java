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
import com.tboat.socket.ClientContext;
import com.tboat.socket.GlobalBroadcaster;
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

    // ── Auto-bid entry: so sánh theo maxBid DESC, nếu bằng thì ưu tiên đăng ký trước ──
    private record AutoBidEntry(String account, double maxBid, long registeredAt)
            implements Comparable<AutoBidEntry> {
        @Override
        public int compareTo(AutoBidEntry other) {
            int cmp = Double.compare(other.maxBid, this.maxBid); // maxBid cao hơn → ưu tiên hơn
            return cmp != 0 ? cmp : Long.compare(this.registeredAt, other.registeredAt); // đăng ký sớm hơn → ưu tiên hơn
        }
    }

    private final int    sessionId;
    private double       currentPrice;
    private String       lastBidder;
    private boolean      isFinished = false;

    /** Mỗi account chỉ có 1 entry auto-bid — dùng Map để upsert nhanh, Queue để lấy top */
    private final Map<String, AutoBidEntry>         autoBidMap   = new HashMap<>();
    private final PriorityBlockingQueue<AutoBidEntry> autoBidQueue =
            new PriorityBlockingQueue<>();

    private final List<ClientContext> subscribers    = new CopyOnWriteArrayList<>();
    private final AuctionSessionDAO   sessionDAO     = new AuctionSessionDAO();
    private final UserDAO             userDAO        = new UserDAO();
    private final HistoryDAO          historyDAO     = new HistoryDAO();
    private final BiddingService      biddingService = new BiddingService();

    public AuctionRoom(int sessionId, double startingPrice) {
        this.sessionId    = sessionId;
        this.currentPrice = startingPrice;
    }

    // ── Bid thường ───────────────────────────────────────────────────────────

    public synchronized BidResult placeBid(double newPrice, String bidderAccount) {
        if (isFinished) return BidResult.DB_ERROR;

        BidResult result = biddingService.placeBid(bidderAccount, sessionId, newPrice);
        if (result != BidResult.OK) return result;

        currentPrice = newPrice;
        lastBidder   = bidderAccount;
        checkAndExtendIfSnipe();
        triggerAutoBids(bidderAccount);
        return BidResult.OK;
    }

// ── Auto-bid ─────────────────────────────────────────────────────────────────

    private void triggerAutoBids(String justBidAccount) {
        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null || isFinished) return;

        double bidStep = session.getBidIncrease();

        int safetyLimit = autoBidMap.size() * 200 + 10;
        while (!isFinished && safetyLimit-- > 0) {
            double nextPrice = currentPrice + bidStep;

            // Tìm challenger tốt nhất: maxBid >= nextPrice, không phải lastBidder
            // Sort rõ ràng để đảm bảo thứ tự (PriorityBlockingQueue.stream không đảm bảo)
            AutoBidEntry challenger = autoBidMap.values().stream()
                    .filter(e -> !e.account().equals(lastBidder) && e.maxBid() >= nextPrice)
                    .min(Comparator.naturalOrder()) // maxBid DESC → registeredAt ASC
                    .orElse(null);

            if (challenger == null) break; // không ai đáp trả được → người dẫn đầu thắng

            BidResult result = biddingService.placeBid(
                    challenger.account(), sessionId, nextPrice);

            switch (result) {
                case OK -> {
                    currentPrice = nextPrice;
                    lastBidder   = challenger.account();
                    checkAndExtendIfSnipe();

                    String nick = userDAO.getNickname(challenger.account());
                    com.google.gson.JsonObject bidData = new com.google.gson.JsonObject();
                    bidData.addProperty("newPrice",          nextPrice);
                    bidData.addProperty("newLeader",         challenger.account());
                    bidData.addProperty("newLeaderNickname", nick);
                    bidData.addProperty("bidTime",           LocalDateTime.now().toString());
                    broadcast("NEW_BID", nick + " (auto) vừa đặt giá mới", bidData);

                    logger.info("[AutoBid] {} auto-bid {} cho phiên {}",
                            challenger.account(), nextPrice, sessionId);

                    if (session.getBuyNowPrice() > 0 && nextPrice >= session.getBuyNowPrice()) {
                        finishAuction();
                        return;
                    }
                }
                case INSUFFICIENT_BALANCE -> {
                    // Hết tiền → loại khỏi auto-bid, thử người tiếp theo
                    logger.info("[AutoBid] {} hết số dư, loại khỏi queue", challenger.account());
                    autoBidQueue.remove(autoBidMap.remove(challenger.account()));
                }
                default -> {
                    // Lỗi DB hoặc giá không hợp lệ → loại để tránh loop vô hạn
                    logger.warn("[AutoBid] Loại {} do result={}", challenger.account(), result);
                    autoBidQueue.remove(autoBidMap.remove(challenger.account()));
                }
            }
        }
    }

    // ── Auto-bid ─────────────────────────────────────────────────────────────

    /**
     * Đăng ký / cập nhật auto-bid cho một tài khoản.
     * Nếu account đã đăng ký rồi thì replace bằng entry mới (maxBid mới, thời gian mới).
     * Sau khi đăng ký, kích hoạt ngay để xem có thể bid luôn không.
     */
    public synchronized void registerAutoBid(String account, double maxBid) {
        if (isFinished) return;

        // Upsert: xóa entry cũ nếu có
        AutoBidEntry old = autoBidMap.get(account);
        if (old != null) autoBidQueue.remove(old);

        AutoBidEntry entry = new AutoBidEntry(account, maxBid, System.nanoTime());
        autoBidMap.put(account, entry);
        autoBidQueue.add(entry);

        logger.info("[AutoBid] {} đăng ký auto-bid maxBid={} cho phiên {}", account, maxBid, sessionId);

        // Thử kích hoạt ngay nếu giá hiện tại < maxBid của người vừa đăng ký
        triggerAutoBids(null);
    }

    // ── Snipe protection ─────────────────────────────────────────────────────

    private void checkAndExtendIfSnipe() {
        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null || session.getEndTime() == null) return;

        long secondsLeft = java.time.Duration.between(
                LocalDateTime.now(), session.getEndTime()).getSeconds();

        if (secondsLeft < 15) {
            AuctionTimerService.getInstance().extendAuction(sessionId, 30);
        }
    }

    // ── Finish ───────────────────────────────────────────────────────────────

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

        if (lastBidder != null) {
            finishWithWinner(session);
        } else {
            finishWithNoWinner();
        }

        AuctionManager.getInstance().removeRoom(sessionId);
    }

    private void finishWithWinner(AuctionSession session) {
        String seller         = session.getSellerAccountName();
        String winnerNickname = resolveNickname(lastBidder);

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            boolean ok = sessionDAO.updateSessionStatus(conn, sessionId, StatusOfAuction.ENDED)
                    && historyDAO.addHistory(conn,
                    new History(sessionId, lastBidder, currentPrice, LocalDateTime.now()))
                    && divideMoney(conn, seller, currentPrice);

            if (ok) {
                conn.commit();
                onWinnerCommitted(session, winnerNickname);
            } else {
                conn.rollback();
                logger.error("[CRITICAL] Lỗi chốt phiên {}, đã Rollback!", sessionId);
            }

        } catch (Exception e) {
            logger.error("[CRITICAL] Ngoại lệ khi chốt phiên {}", sessionId, e);
        }
    }

    private void onWinnerCommitted(AuctionSession session, String winnerNickname) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("winner",         lastBidder);
        payload.put("winnerNickname", winnerNickname);
        payload.put("finalPrice",     currentPrice);

        broadcast("AUCTION_FINISHED", "Phiên đấu giá kết thúc thành công!", payload);
        NotificationService.getInstance().onAuctionWon(session, lastBidder, currentPrice);
        NotificationService.getInstance().onAuctionSold(session, lastBidder, currentPrice);
        broadcastGlobalFinished(payload);

        logger.info("[Server] Chốt phiên {} — người thắng: {} ({})",
                sessionId, winnerNickname, lastBidder);
    }

    private void finishWithNoWinner() {
        sessionDAO.updateSessionStatus(sessionId, StatusOfAuction.ENDED);
        broadcast("AUCTION_FINISHED", "Kết thúc, không có người thắng", null);
        broadcastGlobalFinished(null);
        logger.info("[Server] Phiên {} kết thúc, không có người thắng.", sessionId);
    }

    private void broadcastGlobalFinished(Object payload) {
        GlobalBroadcaster.getInstance().broadcastToAll(
                new Response<>("AUCTION_FINISHED", "NOTIFY",
                        "Phiên " + sessionId + " đã kết thúc", payload));
        GlobalBroadcaster.getInstance().broadcastToAll(
                new Response<>("RELOAD_AVAILABLE", "NOTIFY",
                        "Cập nhật danh sách phiên", sessionId));
        GlobalBroadcaster.getInstance().broadcastToAdmins(
                new Response<>("RELOAD_ALL_ITEMS", "NOTIFY",
                        "Phiên " + sessionId + " vừa kết thúc", sessionId));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String resolveNickname(String account) {
        try {
            String nick = userDAO.getUser(account).getNickname();
            return (nick == null || nick.isBlank()) ? account : nick;
        } catch (Exception e) { return account; }
    }

    private boolean divideMoney(Connection conn, String sellerAccount,
                                double totalAmount) throws SQLException {
        double adminFee      = totalAmount * 0.10;
        double sellerRevenue = totalAmount - adminFee;
        boolean ok = userDAO.updateBalance(conn, "admin", adminFee)
                && userDAO.updateBalance(conn, sellerAccount, sellerRevenue);
        if (ok) logger.info("[Payment] Admin: +{} | Seller: +{}", adminFee, sellerRevenue);
        return ok;
    }

    public void broadcast(String action, String message, Object payload) {
        for (ClientContext client : subscribers) {
            CompletableFuture.runAsync(() -> {
                try { client.sendSystemMessage(action, message, payload); }
                catch (Exception e) {
                    logger.error("Lỗi broadcast tới client trong phiên {}", sessionId);
                }
            }, ServerMain.broadcastExecutor);
        }
    }

    public void addSubscriber(ClientContext c)    { subscribers.add(c); }
    public void removeSubscriber(ClientContext c) { subscribers.remove(c); }
    public double  getCurrentPrice() { return currentPrice; }
    public String  getLastBidder()   { return lastBidder; }
    public int     getSessionId()    { return sessionId; }
    public boolean isFinished()      { return isFinished; }
}