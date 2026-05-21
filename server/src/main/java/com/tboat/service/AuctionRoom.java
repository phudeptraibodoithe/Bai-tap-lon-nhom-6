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

    private record AutoBidEntry(String account, double maxBid, long registeredAt)
            implements Comparable<AutoBidEntry> {
        @Override
        public int compareTo(AutoBidEntry other) {
            int cmp = Double.compare(other.maxBid, this.maxBid);
            return cmp != 0 ? cmp : Long.compare(this.registeredAt, other.registeredAt);
        }
    }

    private final int    sessionId;
    private double       currentPrice;
    private String       lastBidder;
    private boolean      isFinished = false;

    private final Map<String, AutoBidEntry>           autoBidMap   = new HashMap<>();
    private final PriorityBlockingQueue<AutoBidEntry> autoBidQueue = new PriorityBlockingQueue<>();

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

    // ── Auto-bid ─────────────────────────────────────────────────────────────

    private void triggerAutoBids(String justBidAccount) {
        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null || isFinished) return;

        double bidStep = session.getBidIncrease();

        Set<String> outAccounts = new HashSet<>();
        String  finalLeader = lastBidder;
        double  finalPrice  = currentPrice;
        boolean anyBid      = false;

        int safetyLimit = autoBidMap.size() * 200 + 10;

        while (!isFinished && safetyLimit-- > 0) {
            double nextPrice = currentPrice + bidStep;

            // Notify những account bị out do giá vượt maxBid
            autoBidMap.values().stream()
                    .filter(e -> e.maxBid() < nextPrice && !outAccounts.contains(e.account()))
                    .forEach(e -> {
                        outAccounts.add(e.account());
                        notifyAutoBidOut(e.account());
                    });

            AutoBidEntry challenger = autoBidMap.values().stream()
                    .filter(e -> !e.account().equals(lastBidder) && e.maxBid() >= nextPrice)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            if (challenger == null) break;

            BidResult result = biddingService.placeBid(challenger.account(), sessionId, nextPrice);

            switch (result) {
                case OK -> {
                    currentPrice = nextPrice;
                    lastBidder   = challenger.account();
                    finalLeader  = challenger.account();
                    finalPrice   = nextPrice;
                    anyBid       = true;
                    checkAndExtendIfSnipe();

                    if (session.getBuyNowPrice() > 0 && nextPrice >= session.getBuyNowPrice()) {
                        finishAuction();
                        return;
                    }
                }
                case INSUFFICIENT_BALANCE -> {
                    logger.info("[AutoBid] {} hết số dư, loại khỏi queue", challenger.account());
                    outAccounts.add(challenger.account());
                    notifyAutoBidOut(challenger.account());
                    autoBidQueue.remove(autoBidMap.remove(challenger.account()));
                }
                default -> {
                    logger.warn("[AutoBid] Loại {} do result={}", challenger.account(), result);
                    autoBidQueue.remove(autoBidMap.remove(challenger.account()));
                }
            }
        }

        // Broadcast 1 lần duy nhất sau khi loop xong
        if (anyBid) {
            String nick = userDAO.getNickname(finalLeader);
            com.google.gson.JsonObject bidData = new com.google.gson.JsonObject();
            bidData.addProperty("newPrice",          finalPrice);
            bidData.addProperty("newLeader",         finalLeader);
            bidData.addProperty("newLeaderNickname", nick);
            bidData.addProperty("bidTime",           LocalDateTime.now().toString());
            broadcast(ServerEvent.NEW_BID.name(), nick + " (auto) vừa đặt giá mới", bidData);
            logger.info("[AutoBid] Kết thúc loop — leader: {} @ {}", finalLeader, finalPrice);
        }
    }

    private void notifyAutoBidOut(String account) {
        subscribers.stream()
                .filter(c -> account.equals(c.getClientId()))
                .findFirst()
                .ifPresent(c -> {
                    try {
                        c.sendSystemMessage(ServerEvent.AUTO_BID_OUT.name(),
                                "Giá hiện tại đã vượt mức tối đa của bạn. Auto-bid đã dừng.", null);
                    } catch (Exception e) {
                        logger.warn("[AutoBid] Không thể notify out cho {}", account);
                    }
                });
    }

    public synchronized void registerAutoBid(String account, double maxBid) {
        if (isFinished) return;

        AutoBidEntry old = autoBidMap.get(account);
        if (old != null) autoBidQueue.remove(old);

        AutoBidEntry entry = new AutoBidEntry(account, maxBid, System.nanoTime());
        autoBidMap.put(account, entry);
        autoBidQueue.add(entry);

        logger.info("[AutoBid] {} đăng ký auto-bid maxBid={} cho phiên {}", account, maxBid, sessionId);
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

        if (lastBidder != null) finishWithWinner(session);
        else                    finishWithNoWinner();

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

        broadcast(ServerEvent.AUCTION_FINISHED.name(), "Phiên đấu giá kết thúc thành công!", payload);
        NotificationService.getInstance().onAuctionWon(session, lastBidder, currentPrice);
        NotificationService.getInstance().onAuctionSold(session, lastBidder, currentPrice);
        broadcastGlobalFinished(payload);

        logger.info("[Server] Chốt phiên {} — người thắng: {} ({})",
                sessionId, winnerNickname, lastBidder);
    }

    private void finishWithNoWinner() {
        sessionDAO.updateSessionStatus(sessionId, StatusOfAuction.ENDED);
        broadcast(ServerEvent.AUCTION_FINISHED.name(), "Kết thúc, không có người thắng", null);
        broadcastGlobalFinished(null);
        logger.info("[Server] Phiên {} kết thúc, không có người thắng.", sessionId);
    }

    private void broadcastGlobalFinished(Object payload) {
        GlobalBroadcaster.getInstance().broadcastToAll(
                new Response<>(ServerEvent.AUCTION_FINISHED.name(), ServerEvent.NOTIFY.name(),
                        "Phiên " + sessionId + " đã kết thúc", payload));
        GlobalBroadcaster.getInstance().broadcastToAll(
                new Response<>(ServerEvent.RELOAD_AVAILABLE.name(), ServerEvent.NOTIFY.name(),
                        "Cập nhật danh sách phiên", sessionId));
        GlobalBroadcaster.getInstance().broadcastToAdmins(
                new Response<>(ServerEvent.RELOAD_ALL_ITEMS.name(), ServerEvent.NOTIFY.name(),
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