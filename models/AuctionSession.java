package models;

import java.time.LocalDateTime;

public class AuctionSession {
    // Attributes
    private int id;
    private int itemId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private double bidIncrease; 
    private String status; 
    private Item item;
    private StatusOfAuction statusOfAuction;

    // Getters
    public int getId() {
        return id;
    }

    public StatusOfAuction getStatusOfAuction() {
        return statusOfAuction;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }


    // Setters
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setBidIncrease(double bidIncrease) {
        this.bidIncrease = bidIncrease;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setStatusOfAuction(StatusOfAuction statusOfAuction) {
        if (LocalDateTime.now().isBefore(startTime)) {
            this.statusOfAuction = StatusOfAuction.NOT_STARTED;
        } else if (LocalDateTime.now().isAfter(endTime)) {
            this.statusOfAuction = StatusOfAuction.ENDED;
        } else {
            this.statusOfAuction = StatusOfAuction.ONGOING;
        }
    }

    
}