package com.tboat.models;

import java.time.LocalDateTime;

public class Bid {
    private int id;
    private int auctionSessionId;
    private String bidderAccount;
    private double bidAmount;
    private LocalDateTime bidTime;

    public Bid(int id,int auctionSessionId, String bidderAccount, double bidAmount,LocalDateTime bidTime) {
        this.auctionSessionId = auctionSessionId;
        this.id=id;
        this.bidTime=bidTime;
        this.bidderAccount = bidderAccount;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    public Bid(int auctionSessionId, String bidderAccount, double bidAmount) {
        this.auctionSessionId = auctionSessionId;
        this.bidderAccount = bidderAccount;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }
}   