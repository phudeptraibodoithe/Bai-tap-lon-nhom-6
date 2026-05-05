package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.User;

public class SellerCancelRole implements TransactionRole {

    @Override
    public boolean execute(User user, AuctionSession session, double amount,
                           UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryBidDAO bidDAO) {

        if (!session.getSellerAccountName().equals(user.getAccountName())) {
            System.err.println("❌ Từ chối: Tài khoản " + user.getAccountName() + " không phải chủ của phiên này!");
            return false;
        }

        String currentHighestBidder = session.getHighestBidderAccount();
        if (currentHighestBidder != null && !currentHighestBidder.trim().isEmpty()) {
            System.err.println("❌ Từ chối: Phiên đấu giá đã có người trả giá, Seller không thể hủy!");
            return false;
        }

        // 3. THỰC THI: Gọi DAO để cập nhật trạng thái thành CANCELED
        boolean isCanceled = sessionDAO.cancelAuction(session.getId());

        if (isCanceled) {
            System.out.println("✅ Thành công: Seller " + user.getAccountName() + " đã hủy phiên " + session.getId());
            return true;
        } else {
            System.err.println("❌ Lỗi Database: Không thể hủy phiên.");
            return false;
        }
    }
}