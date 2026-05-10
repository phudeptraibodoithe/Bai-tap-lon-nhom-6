package Item;

import java.time.LocalDateTime;

public abstract class AuctionSession {
    protected int id;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected double currentPrice;
    protected double bidIncrease;
    protected String type;
    protected StatusOfAuction statusOfAuction;
    protected String sellerAccountName;
    protected String name;
    protected String description;
    protected String imageURL;
    protected String highestBidderAccount;

    // Constructors
    public AuctionSession() {}

    public AuctionSession(int id, LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, String type, StatusOfAuction statusOfAuction, String sellerAccountName,
                          String name, String description, String imageURL,
                          String highestBidderAccount) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.bidIncrease = bidIncrease;
        this.type = type;   
        this.statusOfAuction = statusOfAuction;
        this.sellerAccountName = sellerAccountName;
        this.name = name;
        this.description = description;
        this.imageURL = imageURL;
        this.highestBidderAccount = highestBidderAccount;
    }

    public AuctionSession(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, String sellerAccountName, String name,
                          String description, String imageURL) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.bidIncrease = bidIncrease;
        this.sellerAccountName = sellerAccountName;
        this.name = name;
        this.description = description;
        this.imageURL = imageURL;
        this.highestBidderAccount = null; // Mặc định là chưa có ai
        updateStatusBasedOnTime();
    }

    public AuctionSession(int id, String name, double currentPrice, double bidIncrease,String sellerAccountName){
        this.id = id;
        this.currentPrice = currentPrice;
        this.bidIncrease = bidIncrease;
        this.sellerAccountName = sellerAccountName;
        this.name = name;
    }

    public void updateStatusBasedOnTime() {
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
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public double getBidIncrease() { return bidIncrease; }
    public String getDescription() { return description; }
    public String getImageURL() { return imageURL; }
    public String getSellerAccountName() { return sellerAccountName; }
    public String getType() { return type; }
    public String getHighestBidderAccount() { return highestBidderAccount; }
    // Setters
    public void setHighestBidderAccount(String highestBidderAccount) { this.highestBidderAccount = highestBidderAccount; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setBidIncrease(double bidIncrease) { this.bidIncrease = bidIncrease; }
    public void setStatusOfAuction(StatusOfAuction statusOfAuction) { this.statusOfAuction = statusOfAuction; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }
    public void setId(int id) { this.id = id; }
    public void setSellerAccountName(String sellerAccountName) { this.sellerAccountName = sellerAccountName; }
    public void setType(String type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }

}