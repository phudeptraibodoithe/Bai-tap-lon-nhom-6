package com.tboat.models;

public class Participation {
    private String accountName;
    private int auctionSessionId;
    private String roleType;

    public Participation() {}

    public Participation(String accountName, int auctionSessionId, String roleType) {
        this.accountName = accountName;
        this.auctionSessionId = auctionSessionId;
        this.roleType = roleType;
    }

    // Getter và Setter
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public int getAuctionSessionId() { return auctionSessionId; }
    public void setAuctionSessionId(int auctionSessionId) { this.auctionSessionId = auctionSessionId; }

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
}