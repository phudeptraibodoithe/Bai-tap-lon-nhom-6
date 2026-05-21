package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.core.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SellerCancelRole implements TransactionRole {
    private static final Logger logger = LoggerFactory.getLogger(SellerCancelRole.class);

    @Override
    public boolean execute(User user, AuctionSession session, double amount,
                           UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryDAO historyDAO, BidDAO bidDAO) {

        if (!session.getSellerAccountName().equals(user.getAccountName())) {
            logger.warn("Từ chối: Tài khoản {} không phải chủ của phiên này!", user.getAccountName());
            return false;
        }

        String currentHighestBidder = session.getHighestBidderAccount();
        if (currentHighestBidder != null && !currentHighestBidder.trim().isEmpty()) {
            logger.warn("Từ chối: Phiên đấu giá đã có người trả giá, Seller không thể hủy!");
            return false;
        }

        boolean isCanceled = sessionDAO.cancelAuction(session.getId());

        if (isCanceled) {
            logger.info("Thành công: Seller {} đã hủy phiên {}", user.getAccountName(), session.getId());
            return true;
        } else {
            logger.error("Lỗi Database: Không thể hủy phiên.");
            return false;
        }
    }
}
