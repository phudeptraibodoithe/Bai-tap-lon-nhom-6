package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;

public class BidderRole implements TransactionRole {

    @Override
    public boolean execute(User user, AuctionSession session, double amount,
                           UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryBidDAO bidDAO) {

        // --- BƯỚC 1: KIỂM TRA (LOGIC VẪN NHƯ CŨ) ---
        if (session.getStatusOfAuction() != StatusOfAuction.ONGOING) return false;
        if (user.getBalance() < amount) return false;
        if (amount < session.getCurrentPrice() + session.getBidIncrease()) return false;

        // --- BƯỚC 2: THỰC THI "THẬT" XUỐNG DATABASE ---

        // 2.1 Lưu thông tin người cũ để hoàn tiền
        String prevBidder = session.getHighestBidderAccount();
        double prevPrice = session.getCurrentPrice();

        // 2.2 Cập nhật phiên đấu giá (Giá mới, người dẫn đầu mới)
        boolean ok1 = sessionDAO.updateSessionPriceAndHighest(session.getId(), user.getAccountName(), amount);
        if (!ok1) return false;

        // 2.3 Trừ tiền người đặt giá mới
        userDAO.updateBalance(user.getAccountName(), -amount);

        // 2.4 Hoàn tiền cho người bị ghi đè (nếu có)
        if (prevBidder != null && !prevBidder.equals(user.getAccountName())) {
            userDAO.updateBalance(prevBidder, prevPrice);
        }

        // 2.5 Ghi lịch sử
        bidDAO.addBid(session.getId(), user.getAccountName(), amount);

        // 2.6 Cập nhật lại Object trên RAM để đồng bộ với DB
        session.setCurrentPrice(amount);
        session.setHighestBidderAccount(user.getAccountName());
        user.setBalance(user.getBalance() - amount);

        return true;
    }
}