package com.tboat.models;

import java.time.LocalDateTime;

public class ElectronicsFactory extends AuctionFactory {
    @Override 
    public AuctionSession createAuctionSession(LocalDateTime startTime, LocalDateTime endTime, 
                                               double currentPrice,double bidIncrease, String sellerAccountName, String name,
                                               String description, String imageURL) {
        return new ElectronicsAuction(startTime, endTime, currentPrice, bidIncrease, 
                                      sellerAccountName, name, description, imageURL);
    }
}
