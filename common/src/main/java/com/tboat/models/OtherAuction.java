package com.tboat.models;

import java.time.LocalDateTime;

public class OtherAuction extends AuctionSession {
    // Constructors
    public OtherAuction() {
        super.setType("Khác");
    }

    public OtherAuction(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                               double bidIncrease, String sellerAccountName, String name,
                               String description, String imageURL) {
        super(startTime, endTime, currentPrice, bidIncrease, sellerAccountName,"Khác", name,
              description, imageURL);
    }
}
