package models;

import java.time.LocalDateTime;

public class Bid {
    private int id;
    private int auctionSessionId;
    private int bidderAccountName;
    private double bidAmount;
    private LocalDateTime bidTime;

    public Bid(int auctionSessionId, int bidderAccountName, double bidAmount) {
        this.auctionSessionId = auctionSessionId;
        this.bidderAccountName = bidderAccountName;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }
}   