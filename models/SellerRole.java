package models;

public class SellerRole implements TransactionRole {
    
    @Override
    public boolean execute(User user, AuctionSession session, double amount) {
        System.out.println("Seller " + user.getAccountName() + " đang quản lý phiên.");
        return true;
    }
}