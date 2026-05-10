package Item;

import java.time.LocalDateTime;

public class FashionAuction extends AuctionSession {
    // Constructors
    public FashionAuction() {}
    public FashionAuction(int id, LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, String type, StatusOfAuction statusOfAuction, String sellerAccountName,
                          String name, String description, String imageURL,
                               String highestBidderAccount) {
        super(id, startTime, endTime, currentPrice, bidIncrease, type, statusOfAuction, sellerAccountName,
              name, description, imageURL, highestBidderAccount);
    }
    public FashionAuction(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                               double bidIncrease, String sellerAccountName, String type, String name,
                               String description, String imageURL) {
        super(startTime, endTime, currentPrice, bidIncrease, sellerAccountName, name,
              description, imageURL);
    }
}
