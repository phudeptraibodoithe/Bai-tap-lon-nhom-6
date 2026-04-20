package models;

import java.time.LocalDateTime;

public class Bid {
    private int id;
    private int auctionSessionId;
    private int bidderId;
    private double bidAmount;
    private LocalDateTime bidTime;

    public Bid(int id, int auctionSessionId, int bidderId, double bidAmount) {
        this.id = id;
        this.auctionSessionId = auctionSessionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }
}   