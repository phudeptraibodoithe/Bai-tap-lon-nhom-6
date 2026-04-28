package models;

public class BidderRole implements TransactionRole {

    @Override
    public RoleType getRoleType() {
        return RoleType.BIDDER;
    }

    public void placeBid(double amount) {
        // Code đặt giá
    }
}