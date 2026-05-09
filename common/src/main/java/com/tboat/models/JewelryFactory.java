package com.tboat.models;

import java.time.LocalDateTime;

public class JewelryFactory extends AuctionFactory {
    @Override
    public AuctionSession createAuctionSession(LocalDateTime startTime, LocalDateTime endTime,
                                               double currentPrice, double bidIncrease, String sellerAccountName, String name,
                                               String description, String imageURL) {
        return new JewelryAuction(startTime, endTime, currentPrice, bidIncrease,
                                      sellerAccountName, name, description, imageURL);
    }
}
