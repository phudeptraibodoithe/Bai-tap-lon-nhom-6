package com.tboat.service;

import com.tboat.ServerMain;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.auction.AuctionSession;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionRoom {

    private static final Logger logger = LoggerFactory.getLogger(AuctionRoom.class);

    private final int    sessionId;
    private double       currentPrice;
    private String       lastBidder;
    private boolean      isFinished = false;

    private final List<ClientContext> subscribers    = new CopyOnWriteArrayList<>();
    private final AuctionSessionDAO   sessionDAO     = new AuctionSessionDAO();
    private final UserDAO             userDAO        = new UserDAO();
    private final HistoryDAO          historyDAO     = new HistoryDAO();
    private final BiddingService      biddingService = new BiddingService();

    public AuctionRoom(int sessionId, double startingPrice) {
        this.sessionId    = sessionId;
        this.currentPrice = startingPrice;
    }

    // ── Bid ──────────────────────────────────────────────────────────────────

    public synchronized boolean placeBid(double newPrice, String bidderAccount) {
        if (isFinished) return false;

        boolean success = biddingService.placeBid(bidderAccount, sessionId, newPrice);
        if (!success) return false;

        currentPrice = newPrice;
        lastBidder   = bidderAccount;
        checkAndExtendIfSnipe();
        return true;
    }

    /** Sniper protection: gia hạn 30 giây nếu bid trong 15 giây cuối */
    private void checkAndExtendIfSnipe() {
        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null || session.getEndTime() == null) return;

        long secondsLeft = java.time.Duration.between(
                LocalDateTime.now(), session.getEndTime()).getSeconds();

        if (secondsLeft < 15) {
            AuctionTimerService.getInstance().extendAuction(sessionId, 30);
            broadcast("TIME_EXTENDED",
                    "Có bid mới trong 15 giây cuối! Phiên gia hạn thêm 30 giây.", 30);
        }
    }

    // ── Finish ───────────────────────────────────────────────────────────────

    public synchronized void finishAuction() {
        if (isFinished) return;
        isFinished = true;

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

    /** Kết thúc phiên khi có người thắng — thực hiện trong 1 transaction */
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

    /** Sau khi commit thành công: broadcast + notification + global reload */
    private void onWinnerCommitted(AuctionSession session, String winnerNickname) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("winner",        lastBidder);
        payload.put("winnerNickname", winnerNickname);
        payload.put("finalPrice",    currentPrice);

        broadcast("AUCTION_FINISHED", "Phiên đấu giá kết thúc thành công!", payload);

        NotificationService.getInstance().onAuctionWon(session, lastBidder, currentPrice);
        NotificationService.getInstance().onAuctionSold(session, lastBidder, currentPrice);

        broadcastGlobalFinished(payload);

        logger.info("[Server] Chốt phiên {} — người thắng: {} ({})",
                sessionId, winnerNickname, lastBidder);
    }

    /** Kết thúc phiên khi không có ai bid */
    private void finishWithNoWinner() {
        sessionDAO.updateSessionStatus(sessionId, StatusOfAuction.ENDED);
        broadcast("AUCTION_FINISHED", "Kết thúc, không có người thắng", null);
        broadcastGlobalFinished(null);
        logger.info("[Server] Phiên {} kết thúc, không có người thắng.", sessionId);
    }

    /** Broadcast toàn cục để các controller ngoài room reload dữ liệu */
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveNickname(String account) {
        try {
            String nick = userDAO.getUser(account).getNickname();
            return (nick == null || nick.isBlank()) ? account : nick;
        } catch (Exception e) {
            return account;
        }
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
                try {
                    client.sendSystemMessage(action, message, payload);
                } catch (Exception e) {
                    logger.error("Lỗi broadcast tới client trong phiên {}", sessionId);
                }
            }, ServerMain.broadcastExecutor);
        }
    }

    // ── Getters / mutators ────────────────────────────────────────────────────

    public void addSubscriber(ClientContext client)    { subscribers.add(client); }
    public void removeSubscriber(ClientContext client) { subscribers.remove(client); }
    public double  getCurrentPrice() { return currentPrice; }
    public String  getLastBidder()   { return lastBidder; }
    public int     getSessionId()    { return sessionId; }
    public boolean isFinished()      { return isFinished; }
}