package Item;

import java.time.LocalDateTime;

public class OtherAuction extends AuctionSession {
    // Constructors
    public OtherAuction() {}
    public OtherAuction(int id, LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                         double bidIncrease, String type, StatusOfAuction statusOfAuction, String sellerAccountName,
                         String name, String description, String imageURL,
                               String highestBidderAccount) {
        super(id, startTime, endTime, currentPrice, bidIncrease, type, statusOfAuction, sellerAccountName,
              name, description, imageURL, highestBidderAccount);
    }
    public OtherAuction(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                               double bidIncrease, String sellerAccountName, String type, String name,
                               String description, String imageURL) {
        super(startTime, endTime, currentPrice, bidIncrease, sellerAccountName, name,
              description, imageURL);
    }
}
