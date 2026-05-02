package com.tboat.service;

import com.tboat.ServerMain;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.History;
import com.tboat.models.StatusOfAuction;
import com.tboat.socket.ClientHandler;
import com.tboat.utils.ResponseCode;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionRoom {
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

                // GỌI TRANSACTION DUY NHẤT Ở ĐÂY
                boolean success = sessionDAO.finalizeAuctionTransaction(sessionId, lastBidder, currentPrice, seller);

                if (success) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("winner", lastBidder);
                    payload.put("finalPrice", currentPrice);
                    broadcast("AUCTION_FINISHED", "Phiên đấu giá kết thúc thành công!", payload);
                } else {
                    // Xử lý lỗi hệ thống nếu transaction thất bại
                    System.err.println("[CRITICAL]: Không thể chốt phiên " + sessionId);
                }
            } else {
                // Trường hợp không ai thèm mua
                sessionDAO.updateSessionStatus(sessionId, StatusOfAuction.ENDED);
                broadcast("AUCTION_FINISHED", "Kết thúc, không có người thắng", null);
            }
        }
        AuctionManager.getInstance().removeRoom(sessionId);
    }

    /**
     * Gửi thông báo JSON tới tất cả người dùng trong phòng
     */
    public void broadcast(String action, String message, Object payload) {
        for (ClientHandler client : subscribers) {
            // Dùng Executor riêng để không làm nghẽn hệ thống
            CompletableFuture.runAsync(() -> {
                client.sendSystemMessage(action, message, payload);
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