package com.tboat.models.auction;

import com.tboat.exception.AuctionBusinessException;
import java.time.LocalDateTime;

public class Bid {
    private int id;
    private int auctionSessionId;
    private String bidderAccount;
    private double bidAmount;
    private LocalDateTime bidTime;

    public Bid(int id, int auctionSessionId, String bidderAccount, double bidAmount, LocalDateTime bidTime) {
        if (bidAmount <= 0) {
            throw new AuctionBusinessException("ERR_BID_01", "Số tiền cược phải lớn hơn 0");
        }
        if (bidderAccount == null || bidderAccount.trim().isEmpty()) {
            throw new AuctionBusinessException("ERR_USER_01", "Tài khoản không được để trống");
        }
        this.id = id;
        this.auctionSessionId = auctionSessionId;
        this.bidderAccount = bidderAccount;
        this.bidAmount = bidAmount;
        this.bidTime = (bidTime != null) ? bidTime : LocalDateTime.now();
    }

    public Bid(int auctionSessionId, String bidderAccount, double bidAmount) {
        this(0, auctionSessionId, bidderAccount, bidAmount, LocalDateTime.now());
    }

    public Bid() {}

    public double getBidAmount() { return bidAmount; }
    public String getBidderAccount() { return bidderAccount; }
    public LocalDateTime getBidTime() { return bidTime; }
}