package com.tboat.service;

import com.tboat.socket.ClientHandler;
import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.atomic.AtomicInteger;

public class AuctionRoom {
    private final String roomName;
    private double currentPrice;
    private String lastBidder;
    private List<ClientHandler> subscribers = new CopyOnWriteArrayList<>();
    private static final ExecutorService broadcastExecutor = Executors.newCachedThreadPool();

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
    public synchronized boolean placeBid(double newPrice, String bidderId) {
        if (isFinished) return false;

        if (newPrice > currentPrice) {
            this.currentPrice = newPrice;
            this.lastBidder = bidderId;
            //Khi thời gian còn dưới 30s có người đặt giá mới -> +30s
            if (this.timeLeft.get() <= 15) {
                this.timeLeft.addAndGet(30);
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

    private void finishAuction() {
        if (!isFinished) {
            isFinished = true;
            timerExecutor.shutdownNow();
            if (lastBidder != null) {
                broadcast("PHIEN DAU GIA KET THUC! Nguoi thang: " + lastBidder + " voi gia " + currentPrice);
            } else {
                broadcast("Phien dau gia ket thuc ma khong co nguoi dat gia.");
            }
        }
    }

    public boolean isFinished() {
        return isFinished;
    }

    public String getRoomName() {
        return roomName;
    }
}