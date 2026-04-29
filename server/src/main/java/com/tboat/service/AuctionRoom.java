package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.History;
import com.tboat.socket.ClientHandler;
import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.atomic.AtomicInteger;

public class AuctionRoom {
    private final String roomName;
    private double currentPrice;
    private String lastBidder;
    private List<ClientHandler> subscribers = new CopyOnWriteArrayList<>();
    private final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(10);

    // Khai báo biến đếm ngược an toàn cho đa luồng
    private final AtomicInteger timeLeft = new AtomicInteger(60);
    private boolean isFinished = false;
    private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor();

    public AuctionRoom(String roomName, double startingPrice) {
        this.roomName = roomName;
        this.currentPrice = startingPrice;
        startCountdown();
    }

    // Xử lý đấu giá đồng thời an toàn
    // Trong AuctionRoom.java
    public synchronized boolean placeBid(double newPrice, String bidderId) {
        if (isFinished) return false;

        if (newPrice > currentPrice) {
            // CHỈ CẬP NHẬT TRẠNG THÁI TRONG BỘ NHỚ
            this.currentPrice = newPrice;
            this.lastBidder = bidderId;

            // Gia hạn thời gian nếu cần (giữ nguyên logic này)
            if (this.timeLeft.get() <= 15) {
                this.timeLeft.set(30);
                broadcast("TIME_EXTENDED|30");
            }
            return true;
        }
        return false;
    }

    public void addSubscriber(ClientHandler client) {
        subscribers.add(client);
    }

    public void removeSubscriber(ClientHandler client) {
        subscribers.remove(client);
    }

    // Observer Pattern: Cập nhật thời gian thực
    public void broadcast(String message) {
        for (ClientHandler client : subscribers) {
            // Mỗi việc gửi tin cho 1 client sẽ được chạy riêng biệt, không đợi nhau
            broadcastExecutor.submit(() -> {
                client.sendMessage("[" + roomName + "] " + message);
            });
        }
    }

    public double getCurrentPrice() { return currentPrice; }

    public void startCountdown() {
        timerExecutor.scheduleAtFixedRate(() -> {
            if (timeLeft.get() > 0) {
                int time = timeLeft.decrementAndGet(); //Tương đuương timeleft--
                if (time %10 == 0 || time <= 5) {
                    broadcast("Thoi gian con lai: " + time + " giay!");
                }
            } else {
                finishAuction();
            }
        }, 1, 1, TimeUnit.SECONDS); // Chạy 1 giây 1 lần
    }

    // Sửa lại hàm finishAuction
    private void finishAuction() {
        synchronized (this) {
            if (!isFinished) {
                isFinished = true;
                timerExecutor.shutdown();

                int sId = Integer.parseInt(roomName);
                AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
                UserDAO userDAO = new UserDAO();
                HistoryBidDAO historyDAO = new HistoryBidDAO();

                if (lastBidder != null) {
                    com.tboat.models.AuctionSession session = sessionDAO.getAuctionById(sId);

                    if (session != null) {
                        String seller = session.getSellerAccountName();

                        // 1. Lưu lịch sử thắng (Lúc này mới ghi vào bảng History)
                        historyDAO.addHistory(new History(sId, lastBidder, currentPrice, java.time.LocalDateTime.now()));

                        // 2. Chốt trạng thái phiên là ENDED
                        sessionDAO.updateSessionStatus(sId, com.tboat.models.StatusOfAuction.ENDED);

                        // --- QUAN TRỌNG: KHÔNG ĐƯỢC trừ tiền lastBidder ở đây nữa ---
                        // Vì chúng ta đã trừ tiền họ ngay lúc họ gọi lệnh BID ở ClientHandler rồi.

                        // 3. CỘNG tiền cho người bán (Seller)
                        userDAO.updateBalance(seller, currentPrice);

                        System.out.println("[Room " + roomName + "]: Kết thúc. Người thắng: " + lastBidder + ", Người bán: " + seller);
                        broadcast("AUCTION_FINISHED|WINNER|" + lastBidder + "|" + currentPrice);
                    }
                } else {
                    // Không có ai bid, chỉ cần đóng phiên
                    sessionDAO.updateSessionStatus(sId, com.tboat.models.StatusOfAuction.ENDED);
                    broadcast("AUCTION_FINISHED|NO_WINNER");
                }

                // Giải phóng Pool gửi tin nhắn của phòng
                broadcastExecutor.shutdown();
            }
        }
    }

    public boolean isFinished() {
        return isFinished;
    }

    public String getLastBidder() {
        return lastBidder;
    }

    public String getRoomName() {
        return roomName;
    }
}