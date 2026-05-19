package models;

import java.time.LocalDateTime;

public class AuctionSession {
    // Attributes
    private int id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private double bidIncrease; 
    private StatusOfAuction statusOfAuction;
    private String sellerAccountName; 
    private String type;
    private String name;
    private String description;
    private String imageURL;
    private String highestBidderAccount;

    // Constructor
    public AuctionSession(LocalDateTime startTime, LocalDateTime endTime, double currentPrice, double bidIncrease, String sellerAccountName, String type, String name, String description, String imageURL) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.bidIncrease = bidIncrease;
        this.sellerAccountName = sellerAccountName;
        this.type = type;
        this.name = name;
        this.description = description;
        this.imageURL = imageURL;
        setStatusOfAuction(StatusOfAuction.PENDING); 
    }

    // Getters
    public int getId() { return id; }
    public StatusOfAuction getStatusOfAuction() { return statusOfAuction; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public double getCurrentPrice() { return currentPrice; }
    public double getBidIncrement() { return bidIncrease; }
    public String getSellerAccountName() { return sellerAccountName; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageURL() { return imageURL; }
    public String getHighestBidderAccount() { return winnerAccountName; }
    
    // Setters
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setBidIncrease(double bidIncrease) { this.bidIncrease = bidIncrease; }
    public void setSellerAccountName(String sellerAccountName) { this.sellerAccountName = sellerAccountName; }
    public void setHighestBidderAccount(String winnerAccountName) { this.winnerAccountName = winnerAccountName; }
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