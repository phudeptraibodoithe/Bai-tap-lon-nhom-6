package com.auction.server;

import java.util.*;
import java.util.concurrent.*;

public class AuctionRoom {
    private String roomName;
    private double currentPrice;
    private String lastBidder;
    private List<ClientHandler> subscribers = new CopyOnWriteArrayList<>();
    private static final ExecutorService broadcastExecutor = Executors.newCachedThreadPool();


    public AuctionRoom(String roomName, int startingPrice) {
        this.roomName = roomName;
        this.currentPrice = startingPrice;
    }

    // Xử lý đấu giá đồng thời an toàn (3.2.2)
    public synchronized boolean placeBid(double newPrice, String bidderId) {
        if (newPrice > currentPrice) {
            this.currentPrice = newPrice;
            this.lastBidder = bidderId;
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

    // Observer Pattern: Cập nhật thời gian thực (3.2.4)
    public void broadcast(String message) {
        for (ClientHandler client : subscribers) {
            // Mỗi việc gửi tin cho 1 client sẽ được chạy riêng biệt, không đợi nhau
            broadcastExecutor.submit(() -> {
                client.sendmessage("[" + roomName + "] " + message);
            });
        }
    }

    public double getCurrentPrice() { return currentPrice; }
}