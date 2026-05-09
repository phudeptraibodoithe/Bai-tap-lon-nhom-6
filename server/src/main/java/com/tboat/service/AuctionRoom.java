package com.tboat.service;

import com.tboat.ServerMain;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.AuctionSession;
import com.tboat.models.History;
import com.tboat.models.StatusOfAuction;
import com.tboat.socket.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
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

    private final List<ClientHandler> subscribers = new CopyOnWriteArrayList<>();
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
    private final UserDAO userDAO = new UserDAO();
    private final HistoryBidDAO historyDAO = new HistoryBidDAO();
    private final BiddingService biddingService = new BiddingService(); // Khởi tạo một lần dùng mãi mãi

    public AuctionRoom(int sessionId, double startingPrice) {
        this.sessionId = sessionId;
        this.currentPrice = startingPrice;
    }

    /**
     * Xử lý đặt giá mới (Kết hợp logic Transaction updateBidLeader)
     */
    public synchronized boolean placeBid(double newPrice, String bidderAccount) {
        if (isFinished) return false;

        // Gọi service đã khởi tạo sẵn
        boolean success = biddingService.placeBid(bidderAccount, this.sessionId, newPrice);

        if (success) {
            this.currentPrice = newPrice;
            this.lastBidder = bidderAccount;

            // Kiểm tra Sniper Protection
            AuctionSession session = sessionDAO.getAuctionById(sessionId);
            if (session != null && session.getEndTime() != null) {
                long secondsLeft = java.time.Duration.between(
                        LocalDateTime.now(),
                        session.getEndTime()
                ).getSeconds();

                if (secondsLeft < 15) {
                    AuctionTimerService.getInstance().extendAuction(sessionId, 30);
                    broadcast("TIME_EXTENDED", "Phiên được gia hạn thêm 30 giây!", 30);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Chốt phiên đấu giá
     */
    public synchronized void finishAuction() {
        if (isFinished) return;
        isFinished = true;

        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session != null) {
            if (lastBidder != null) {
                String seller = session.getSellerAccountName();

                try {
                    // 1. Cập nhật trạng thái phiên thành ENDED
                    boolean statusOk = sessionDAO.updateSessionStatus(sessionId, StatusOfAuction.ENDED);

                    // 2. Thêm vào bảng lịch sử
                    History history = new History(sessionId, lastBidder, currentPrice, LocalDateTime.now());
                    boolean historyOk = historyDAO.addHistory(history);

                    // 3. Gọi hàm chia tiền
                    boolean paymentOk = divideMoney(seller, currentPrice);

                    // 4. Kiểm tra
                    if (statusOk && historyOk && paymentOk) {
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("winner", lastBidder);
                        payload.put("finalPrice", currentPrice);
                        broadcast("AUCTION_FINISHED", "Phiên đấu giá kết thúc thành công!", payload);
                        logger.info("[Server]: ✅ Đã chốt phiên {} thành công!", sessionId);
                    } else {
                        logger.error("[CRITICAL]: Lỗi chốt phiên {} (DB thất bại)", sessionId);
                    }
                } catch (Exception e) {
                    logger.error("[CRITICAL]: Ngoại lệ khi chốt phiên {}", sessionId, e);
                }

            } else {
                // Trường hợp không ai mua
                sessionDAO.updateSessionStatus(sessionId, StatusOfAuction.ENDED);
                broadcast("AUCTION_FINISHED", "Kết thúc, không có người thắng", null);
            }
        }
        AuctionManager.getInstance().removeRoom(sessionId);
    }

    /**
     * Xử lý chia tiền: 10% cho Admin, 90% cho người bán
     * Trả về true nếu giao dịch an toàn và thành công
     */
    private boolean divideMoney(String sellerAccount, double totalAmount) {
        double adminFee = totalAmount * 0.10;
        double sellerRevenue = totalAmount - adminFee;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            boolean adminOk = userDAO.updateBalance(conn, "admin", adminFee);
            boolean sellerOk = userDAO.updateBalance(conn, sellerAccount, sellerRevenue);

            if (adminOk && sellerOk) {
                conn.commit();
                logger.info("[Payment]: Đã chia tiền -> Admin: {}, Seller: {}", adminFee, sellerRevenue);
                return true;
            } else {
                conn.rollback(); // Lỗi 1 trong 2 thì hoàn tiền
                logger.error("[Payment]: Lỗi khi cập nhật số dư, đã Rollback!");
                return false;
            }
        } catch (Exception e) {
            logger.error("[Payment]: Lỗi ngoại lệ khi chia tiền", e);
            return false;
        }
    }

    /**
     * Gửi thông báo JSON tới tất cả người dùng trong phòng
     */
    public void broadcast(String action, String message, Object payload) {
        if (subscribers.isEmpty()) return; // Tối ưu: Nếu phòng trống thì khỏi tốn công chạy đa luồng
        for (ClientHandler client : subscribers) {
            CompletableFuture.runAsync(() -> {
                try {
                    client.sendSystemMessage(action, message, payload);
                } catch (Exception e) {
                    logger.error("Lỗi gửi thông tin cho client!");
                }
            }, ServerMain.broadcastExecutor);
        }
    }

    public void addSubscriber(ClientHandler client) { subscribers.add(client); }
    public void removeSubscriber(ClientHandler client) { subscribers.remove(client); }
    public double getCurrentPrice() { return currentPrice; }
    public String getLastBidder() { return lastBidder; }
    public int getSessionId() { return sessionId; }
    public boolean isFinished() { return isFinished; }
}