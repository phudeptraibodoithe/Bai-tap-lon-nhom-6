package com.tboat.models;

import java.time.LocalDateTime;

public class FashionAuction extends AuctionSession {

    // Constructors
    public FashionAuction() {
        super.setType("Thời trang");
    }

    public FashionAuction(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, String sellerAccountName, String name,
                          String description, String imageURL) {
        super(startTime, endTime, currentPrice, bidIncrease, sellerAccountName, "Thời trang", name,
                description, imageURL);
    }
}