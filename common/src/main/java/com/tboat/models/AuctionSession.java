package com.tboat.models;

import java.time.LocalDateTime;

public class AuctionSession {
    private int id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private double bidIncrease;
    private StatusOfAuction statusOfAuction;
    private Item item;
    private String highestBidderAccount;
    private int itemId;


    public AuctionSession(int id, LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, StatusOfAuction statusOfAuction, Item item,
                          String highestBidderAccount) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.bidIncrease = bidIncrease;
        this.statusOfAuction = statusOfAuction;
        this.item = item;
        this.highestBidderAccount = highestBidderAccount;
    }
    
    public AuctionSession() {}

    public AuctionSession(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, Item item) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.bidIncrease = bidIncrease;
        this.item = item;
        this.highestBidderAccount = null;
        updateStatusBasedOnTime();
    }

    public void updateStatusBasedOnTime() {
        if (this.startTime == null || this.endTime == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            this.statusOfAuction = StatusOfAuction.NOT_STARTED;
        } else if (now.isAfter(endTime)) {
            this.statusOfAuction = StatusOfAuction.ENDED;
        } else {
            this.statusOfAuction = StatusOfAuction.ONGOING;
        }
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public StatusOfAuction getStatusOfAuction() { return statusOfAuction; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getName() { return item.getName(); }
    public double getCurrentPrice() { return currentPrice; }
    public double getBidIncrease() { return bidIncrease; }
    public String getDescription() { return item.getDescription(); }
    public String getImageURL() { return item.getImageURL(); }
    public String getSellerAccountName() { return item.getSellerAccountName(); }
    public String getType() { return item.getType(); }
    public String getHighestBidderAccount() { return highestBidderAccount; }
    // Setters
    public void setHighestBidderAccount(String highestBidderAccount) {
        this.highestBidderAccount = highestBidderAccount;
    }

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

    public void setStatusOfAuction(StatusOfAuction statusOfAuction) {
        this.statusOfAuction = statusOfAuction;
    }

    public void setImageURL(String imageURL) {
        item.setImageURL(imageURL);
    }
    // Thêm getter/setter
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    // Thêm getter trả về Item object (để ItemDAO dùng)
    public Item getItem() { return item; }
    // 2. THÊM CÁC SETTER CÒN THIẾU
    public void setId(int id) { this.id = id; }
    public void setSellerAccountName(String sellerAccountName) { this.item.setSellerAccountName(sellerAccountName); }
    public void setName(String name) { this.item.setName(name); }
    public void setDescription(String description) { this.item.setDescription(description); }
}