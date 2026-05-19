package com.tboat.models.core;

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
        this.bidderAccount = bidderAccount;
        this.bidAmount = bidAmount;

//        if (bidTime == null) {
//            this.bidTime = LocalDateTime.now();
//        }
//        else {
//            this.bidTime=bidTime;
//        }
        this.bidTime = (bidTime != null) ? bidTime : LocalDateTime.now(); //Tuong tu if-else o tren
    }

    public Bid(int auctionSessionId, String bidderAccount, double bidAmount) {
        this.auctionSessionId = auctionSessionId;
        this.bidderAccount = bidderAccount;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }
    public Bid(){}

    public double getBidAmount() { return bidAmount; }
    public String getBidderAccount() { return bidderAccount; }
}   