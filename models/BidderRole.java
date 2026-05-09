package models;

public class BidderRole implements TransactionRole {

    @Override
    public boolean execute(User user, AuctionSession session, double amount) {
        if (session.getStatusOfAuction() != StatusOfAuction.ONGOING) {
            System.out.println("Lỗi: Phiên đấu giá không đang diễn ra!");
            return false;
        } else {
            if (user.getBalance() < amount) {
                System.out.println("Lỗi: Số dư  không đủ!");
                return false;
            } else {
                if (amount < session.getCurrentPrice() + session.getBidIncrement()) {
                    System.out.println("Lỗi: Giá không hợp lệ!");
                    return false;
                }
                else {
                    session.setCurrentPrice(amount);
                    session.setHighestBidderAccount(user.getAccountName());
                    System.out.println("Đặt giá thành công!");
                    return true; 
                }
            }
        }
    }
}