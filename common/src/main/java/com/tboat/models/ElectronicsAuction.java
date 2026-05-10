package com.tboat.models;

import java.time.LocalDateTime;

public class ElectronicsAuction extends AuctionSession {

    public ElectronicsAuction() {
        super.setType("Điện tử");
    }
    // Constructor dùng cho Factory
    public ElectronicsAuction(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                              double bidIncrease, String sellerAccountName, String name,
                              String description, String imageURL) {
        super(startTime, endTime, currentPrice, bidIncrease, sellerAccountName, "Điện tử", name,
                description, imageURL);
    }
}