package Item;

import java.time.LocalDateTime;

public class JewelryAuction extends AuctionSession {
    // Constructors
    public JewelryAuction() {}
    public JewelryAuction(int id, LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                          double bidIncrease, String type, StatusOfAuction statusOfAuction, String sellerAccountName,
                          String name, String description, String imageURL,
                               String highestBidderAccount) {
        super(id, startTime, endTime, currentPrice, bidIncrease, type, statusOfAuction, sellerAccountName,
              name, description, imageURL, highestBidderAccount);
    }
    public JewelryAuction(LocalDateTime startTime, LocalDateTime endTime, double currentPrice,
                               double bidIncrease, String sellerAccountName, String type, String name,
                               String description, String imageURL) {
        super(startTime, endTime, currentPrice, bidIncrease, sellerAccountName, name,
              description, imageURL);
    }
}
