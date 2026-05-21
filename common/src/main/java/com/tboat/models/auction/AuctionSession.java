package com.tboat.models.auction;

import com.tboat.exception.AuctionBusinessException;
import com.tboat.models.item.Item;
import java.time.LocalDateTime;

public class AuctionSession {
    private int id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private double bidIncrease;
    private StatusOfAuction statusOfAuction;
    private Item item;
    private double buyNowPrice;
    private String highestBidderAccount;
    private int itemId;
    public AuctionSession(int id, LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, StatusOfAuction statusOfAuction, Item item,
                          String highestBidderAccount, double buyNowPrice) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.bidIncrease = bidIncrease;
        this.buyNowPrice = 0.0;
        this.statusOfAuction = statusOfAuction;
        this.item = item;
        this.highestBidderAccount = highestBidderAccount;
    }

    public AuctionSession() {}

    public AuctionSession(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, Item item) {
        if (currentPrice < 0) {
            throw new AuctionBusinessException("ERR_SESSION_01", "Giá khởi điểm không được âm");
        }
        if (bidIncrease <= 0) {
            throw new AuctionBusinessException("ERR_SESSION_02", "Bước giá phải lớn hơn 0");
        }
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.bidIncrease = bidIncrease;
        this.buyNowPrice = 0.0;
        this.item = item;
        // Tự động đồng bộ itemId từ object Item sang trường itemId của session
        this.itemId = (item != null) ? item.getId() : 0;
        this.highestBidderAccount = null;
        updateStatusBasedOnTime();
    }

    public void setCurrentPrice(double currentPrice) {
        if (currentPrice < 0) {
            throw new AuctionBusinessException("ERR_SESSION_01", "Giá hiện tại không được âm");
        }
        this.currentPrice = currentPrice;
    }

    public String getSellerAccountName(){return this.item.getSellerAccountName();}
    public String getName(){return this.item.getName();}
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getBuyNowPrice() { return buyNowPrice; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public double getCurrentPrice() { return currentPrice; }
    public double getBidIncrease() { return bidIncrease; }
    public StatusOfAuction getStatusOfAuction() { return statusOfAuction; }
    public String getDescription() { return item.getDescription(); }
    public String getImageURL() { return item.getImageURL(); }
    public String getType() { return item.getType(); }
    public Item getItem() {
        return item;
    }

    public void setStatusOfAuction(StatusOfAuction statusOfAuction) { this.statusOfAuction = statusOfAuction; }
    public String getHighestBidderAccount() { return highestBidderAccount; }
    public void setHighestBidderAccount(String highestBidderAccount) { this.highestBidderAccount = highestBidderAccount; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setSellerAccountName(String sellerAccountName) { this.item.setSellerAccountName(sellerAccountName); }
    public void setItem(Item item) {
        this.item = item;
    }
    public void setBuyNowPrice(double buyNowPrice) {
        this.buyNowPrice = buyNowPrice;
    }
    public void updateStatusBasedOnTime() {
        if (this.startTime == null || this.endTime == null) return;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            this.statusOfAuction = StatusOfAuction.NOT_STARTED;
        } else if (now.isAfter(endTime)) {
            this.statusOfAuction = StatusOfAuction.ENDED;
        } else {
            this.statusOfAuction = StatusOfAuction.ONGOING;
        }
    }
}