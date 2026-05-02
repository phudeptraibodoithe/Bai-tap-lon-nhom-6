package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.History;
import com.tboat.models.StatusOfAuction;
import com.tboat.socket.ClientHandler;
import com.tboat.utils.ResponseCode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionRoom {
    private final String roomName;
    private double currentPrice;
    private String lastBidder;
    private final List<ClientHandler> subscribers = new CopyOnWriteArrayList<>();
    //private final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(10);
    //private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor();
    //private final AtomicInteger timeLeft = new AtomicInteger(60);
    //private boolean isFinished = false;

    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
    private final UserDAO userDAO = new UserDAO();
    private final HistoryBidDAO historyDAO = new HistoryBidDAO();

    public AuctionRoom(String roomName, double startingPrice) {
        this.roomName = roomName;
        this.currentPrice = startingPrice;
        //startCountdown();
    }

    /**
     * Xử lý đặt giá mới
     */
    public synchronized boolean placeBid(double newPrice, String bidderId) {
        // 1. Chuyển roomName thành int để có sessionId
        int sessionId = Integer.parseInt(this.roomName);

        // 2. Gọi hàm updateBidLeader với đầy đủ 5 tham số:
        // (sessionId, người đặt mới, giá mới, người giữ giá cũ, giá cũ)
        ResponseCode result = historyDAO.updateBidLeader(
                sessionId,
                bidderId,
                newPrice,
                this.lastBidder,
                this.currentPrice
        );

        // 3. Nếu đặt giá thành công
        if (result == ResponseCode.SUCCESS) {
            this.currentPrice = newPrice;
            this.lastBidder = bidderId;

            // Kiểm tra Sniper Protection
            AuctionSession session = sessionDAO.getAuctionById(sessionId);
            if (session != null) {
                long secondsLeft = java.time.Duration.between(
                        java.time.LocalDateTime.now(),
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
     * Gửi thông báo JSON tới tất cả người dùng trong phòng
     */
    // Sửa lại hàm broadcast:
    public void broadcast(String action, String message, Object payload) {
        for (ClientHandler client : subscribers) {
            // Sử dụng CompletableFuture để đẩy task vào ForkJoinPool dùng chung của hệ thống
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                client.sendSystemMessage(action, message, payload);
            });
        }
    }

    // Các hàm getter/setter hỗ trợ
    public void addSubscriber(ClientHandler client) { subscribers.add(client); }
    public void removeSubscriber(ClientHandler client) { subscribers.remove(client); }
    public double getCurrentPrice() { return currentPrice; }
    //public boolean isFinished() { return isFinished; }
    public String getLastBidder() { return lastBidder; }
    public String getRoomName() { return roomName; }
}