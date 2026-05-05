package com.tboat.models;

import java.time.LocalDateTime;

public class JewelryAuction extends AuctionSession {
    // Constructors
    public JewelryAuction() {
        super.setType("Trang sức");
    }

    public JewelryAuction(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                               double bidIncrease, String sellerAccountName, String name,
                               String description, String imageURL) {
        super(startTime, endTime, currentPrice, bidIncrease, sellerAccountName,"Trang sức", name,
              description, imageURL);
    }
}
