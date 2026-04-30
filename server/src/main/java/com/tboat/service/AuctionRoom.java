package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.History;
import com.tboat.socket.ClientHandler;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionRoom {
    private final String roomName;
    private double currentPrice;
    private String lastBidder;
    private final List<ClientHandler> subscribers = new CopyOnWriteArrayList<>();
    private final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(10);;
    private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger timeLeft = new AtomicInteger(60);
    private boolean isFinished = false;
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
    private final UserDAO userDAO = new UserDAO();
    private final HistoryBidDAO historyDAO = new HistoryBidDAO();

    public AuctionRoom(String roomName, double startingPrice) {
        this.roomName = roomName;
        this.currentPrice = startingPrice;
        startCountdown();
    }
    public synchronized boolean placeBid(double newPrice, String bidderId) {
        if (isFinished) return false;

        if (newPrice > currentPrice) {
            this.currentPrice = newPrice;
            this.lastBidder = bidderId;

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
    public void broadcast(String message) {
        for (ClientHandler client : subscribers) {
            broadcastExecutor.submit(() -> {
                client.sendMessage(message);
            });
        }
    }

    public double getCurrentPrice() { return currentPrice; }
    public void startCountdown() {
        timerExecutor.scheduleAtFixedRate(() -> {
            if (timeLeft.get() > 0) {
                int time = timeLeft.decrementAndGet();
                if (time % 10 == 0 || time <= 5) {
                    broadcast("TIME_LEFT|" + time);
                }
            } else {
                finishAuction();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void finishAuction() {
        synchronized (this) {
            if (!isFinished) {
                isFinished = true;
                timerExecutor.shutdown();
                int sId = Integer.parseInt(roomName);
                if (lastBidder != null) {
                    com.tboat.models.AuctionSession session = sessionDAO.getAuctionById(sId);
                    if (session != null) {
                        String seller = session.getSellerAccountName();

                        historyDAO.addHistory(new History(sId, lastBidder, currentPrice, java.time.LocalDateTime.now()));
                        sessionDAO.updateSessionStatus(sId, com.tboat.models.StatusOfAuction.ENDED);
                        userDAO.updateBalance(seller, currentPrice);

                        System.out.println("[Room " + roomName + "]: Kết thúc. Người thắng: " + lastBidder + ", Người bán: " + seller);
                        broadcast("AUCTION_FINISHED|WINNER|" + lastBidder + "|" + currentPrice);
                    }
                } else {
                    sessionDAO.updateSessionStatus(sId, com.tboat.models.StatusOfAuction.ENDED);
                    broadcast("AUCTION_FINISHED|NO_WINNER");
                }
                broadcastExecutor.shutdown();
                AuctionManager.getInstance().removeRoom(roomName);
            }
        }
    }

    public boolean isFinished() { return isFinished; }
    public String getLastBidder() { return lastBidder; }
    public String getRoomName() { return roomName; }
}