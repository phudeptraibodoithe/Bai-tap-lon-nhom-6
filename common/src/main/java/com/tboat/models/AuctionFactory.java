package com.tboat.models;

import java.time.LocalDateTime;

public abstract class AuctionFactory {
    public AuctionFactory(){}

    public abstract AuctionSession createAuctionSession(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, String sellerAccountName, String name,
                          String description, String imageURL);
}
