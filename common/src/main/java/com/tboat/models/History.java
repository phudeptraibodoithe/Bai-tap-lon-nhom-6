package com.tboat.models;


import java.time.LocalDateTime;

public class History {
    private int auctionSessionId;
    private String winnerAccountName;
    private double finalPrice;
    private LocalDateTime completedAt;

    public History(){}

    public History(int auctionSessionId, String winnerAccountName, double finalPrice,LocalDateTime completedAt) {
        this.auctionSessionId = auctionSessionId;
        this.winnerAccountName = winnerAccountName;
        this.finalPrice = finalPrice;
        this.completedAt = completedAt;
    }

    // Getters
    public int getAuctionSessionId() {
        return auctionSessionId;
    }

    public String getWinnerAccountName() {
        return winnerAccountName;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}