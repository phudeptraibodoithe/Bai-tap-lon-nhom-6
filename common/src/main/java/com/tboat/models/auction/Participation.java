package com.tboat.models.auction;

import com.tboat.exception.AuctionBusinessException;

public class Participation {
    private String accountName;
    private int auctionSessionId;
    private String roleType;

    public Participation() {}

    public Participation(String accountName, int auctionSessionId, String roleType) {
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new AuctionBusinessException("ERR_PART_01", "Tài khoản không được trống");
        }
        if (roleType == null || roleType.trim().isEmpty()) {
            throw new AuctionBusinessException("ERR_PART_02", "Vai trò không được trống");
        }
        if (auctionSessionId <= 0) {
            throw new AuctionBusinessException("ERR_PART_03", "ID phiên đấu giá phải lớn hơn 0");
        }
        this.accountName = accountName;
        this.auctionSessionId = auctionSessionId;
        this.roleType = roleType;
    }

    // Getter và Setter
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public int getAuctionSessionId() { return auctionSessionId; }

    public String getRoleType() { return roleType; }
}