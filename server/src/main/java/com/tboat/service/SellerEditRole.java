package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

public class SellerEditRole implements TransactionRole {
    private static final Logger logger = LoggerFactory.getLogger(SellerEditRole.class);

    @Override
    public boolean execute(User user, AuctionSession session, double amount,
                           UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryBidDAO bidDAO) {

        AuctionSession dbSession = sessionDAO.getAuctionById(session.getId());
        if (dbSession == null || !dbSession.getSellerAccountName().equals(user.getAccountName())) {
            logger.warn("Từ chối Edit: Không tìm thấy phiên hoặc không đúng chủ sở hữu.");
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(dbSession.getStartTime())) {
            logger.warn("Từ chối Edit: Phiên đấu giá đã bắt đầu vào lúc {}, không thể sửa!", dbSession.getStartTime());
            return false;
        }
        if (dbSession.getStatusOfAuction() != StatusOfAuction.PENDING &&
                dbSession.getStatusOfAuction() != StatusOfAuction.NOT_STARTED) {
            logger.warn("Từ chối Edit: Trạng thái hiện tại ({}) không cho phép chỉnh sửa.", dbSession.getStatusOfAuction());
            return false;
        }
        if (dbSession.getHighestBidderAccount() != null && !dbSession.getHighestBidderAccount().trim().isEmpty()) {
            logger.warn("Từ chối Edit: Đã có người đặt giá.");
            return false;
        }
        boolean isUpdated = sessionDAO.updateAuction(session);
        if (isUpdated) {
            logger.info("Thành công: Seller {} đã cập nhật thông tin phiên {}", user.getAccountName(), session.getId());
            return true;
        } else {
            logger.error("Lỗi Database: Không thể cập nhật thông tin phiên {}", session.getId());
            return false;
        }
    }
}