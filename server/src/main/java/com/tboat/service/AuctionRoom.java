package com.tboat.service;

import com.tboat.ServerMain;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.History;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.socket.ClientContext;
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

    private final int sessionId;
    private double currentPrice;
    private String lastBidder;
    private boolean isFinished = false;

    private final List<ClientContext> subscribers   = new CopyOnWriteArrayList<>();
    private final AuctionSessionDAO   sessionDAO    = new AuctionSessionDAO();
    private final UserDAO             userDAO       = new UserDAO();
    private final HistoryDAO          historyDAO    = new HistoryDAO();
    private final BiddingService      biddingService = new BiddingService();

    public AuctionRoom(int sessionId, double startingPrice) {
        this.sessionId    = sessionId;
        this.currentPrice = startingPrice;
    }

    public synchronized boolean placeBid(double newPrice, String bidderAccount) {
        if (isFinished) return false;

        boolean success = biddingService.placeBid(bidderAccount, this.sessionId, newPrice);

        if (success) {
            this.currentPrice = newPrice;
            this.lastBidder   = bidderAccount;

            // Sniper Protection — chỉ xử lý tại đây, không xử lý thêm ở AuctionHandler
            AuctionSession session = sessionDAO.getAuctionById(sessionId);
            if (session != null && session.getEndTime() != null) {
                long secondsLeft = java.time.Duration.between(
                        LocalDateTime.now(), session.getEndTime()).getSeconds();
                if (secondsLeft < 15) {
                    AuctionTimerService.getInstance().extendAuction(sessionId, 30);
                    broadcast("TIME_EXTENDED", "Có bid mới trong 15 giây cuối! Phiên gia hạn thêm 30 giây.", 30);
                }
            }
            return true;
        }
        return false;
    }

    public synchronized void finishAuction() {
        if (isFinished) return;
        isFinished = true;

        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null) {
            logger.error("[CRITICAL]: Không tìm thấy phiên {} trong DB!", sessionId);
            AuctionManager.getInstance().removeRoom(sessionId);
            return;
        }

        if (lastBidder != null) {
            String seller = session.getSellerAccountName();

            // Lấy nickname người thắng để broadcast cho client hiển thị
            String winnerNickname = userDAO.getUser(lastBidder).getNickname();
            if (winnerNickname == null || winnerNickname.isBlank()) winnerNickname = lastBidder;

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);

                boolean statusOk  = sessionDAO.updateSessionStatus(
                        conn, sessionId, StatusOfAuction.ENDED);
                boolean historyOk = historyDAO.addHistory(
                        conn, new History(sessionId, lastBidder, currentPrice, LocalDateTime.now()));
                boolean paymentOk = divideMoney(conn, seller, currentPrice);

                if (statusOk && historyOk && paymentOk) {
                    conn.commit();

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("winner",          lastBidder);      // account — để client logic
                    payload.put("winnerNickname",  winnerNickname);  // nickname — để client hiển thị
                    payload.put("finalPrice",      currentPrice);
                    broadcast("AUCTION_FINISHED", "Phiên đấu giá kết thúc thành công!", payload);
                    logger.info("[Server]: Đã chốt phiên {} — người thắng: {} ({})",
                            sessionId, winnerNickname, lastBidder);
                } else {
                    conn.rollback();
                    logger.error("[CRITICAL]: Lỗi chốt phiên {}, đã Rollback toàn bộ!", sessionId);
                }

            } catch (Exception e) {
                logger.error("[CRITICAL]: Ngoại lệ khi chốt phiên {}", sessionId, e);
            }

        } else {
            // Không có ai bid — chỉ đổi trạng thái
            sessionDAO.updateSessionStatus(sessionId, StatusOfAuction.ENDED);
            broadcast("AUCTION_FINISHED", "Kết thúc, không có người thắng", null);
        }

        AuctionManager.getInstance().removeRoom(sessionId);
    }

    private boolean divideMoney(Connection conn, String sellerAccount, double totalAmount) throws SQLException {
        double adminFee      = totalAmount * 0.10;
        double sellerRevenue = totalAmount - adminFee;

        boolean adminOk  = userDAO.updateBalance(conn, "admin",         adminFee);
        boolean sellerOk = userDAO.updateBalance(conn, sellerAccount, sellerRevenue);

        if (adminOk && sellerOk) {
            logger.info("[Payment]: Admin: +{} | Seller: +{}", adminFee, sellerRevenue);
            return true;
        }
        return false;
    }

    public void broadcast(String action, String message, Object payload) {
        if (subscribers.isEmpty()) return;
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

    public void addSubscriber(ClientContext client)    { subscribers.add(client); }
    public void removeSubscriber(ClientContext client) { subscribers.remove(client); }
    public double getCurrentPrice()  { return currentPrice; }
    public String getLastBidder()    { return lastBidder; }
    public int getSessionId()        { return sessionId; }
    public boolean isFinished()      { return isFinished; }
}