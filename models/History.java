package models;

import java.time.LocalDateTime;

public class History {
    private int auctionSessionId;
    private int winnerAccountName;
    private double finalPrice;
    private LocalDateTime completedAt;

    public History(int auctionSessionId, int winnerAccountName, double finalPrice) {
        this.auctionSessionId = auctionSessionId;
        this.winnerAccountName = winnerAccountName;
        this.finalPrice = finalPrice;
        this.completedAt = LocalDateTime.now();
    }

    // Getters
    public int getAuctionSessionId() { return auctionSessionId; }
    public int getWinnerAccountName() { return winnerAccountName; }
    public double getFinalPrice() { return finalPrice; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}