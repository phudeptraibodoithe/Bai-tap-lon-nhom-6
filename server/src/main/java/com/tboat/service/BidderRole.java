package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.core.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class BidderRole implements TransactionRole {
    private static final Logger logger = LoggerFactory.getLogger(BidderRole.class);

    /**
     * Lưu một lần đặt giá trong một giao dịch database:
     * trừ tiền người mới, cập nhật phiên, hoàn tiền người cũ, rồi lưu lịch sử bid.
     */
    @Override
    public boolean execute(User user, AuctionSession session, double amount,
                           UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryDAO historyDAO, BidDAO bidDAO) {

        String bidderAccount = user.getAccountName();
        int sessionId = session.getId();

        String previousBidder = session.getHighestBidderAccount();
        double previousPrice = session.getCurrentPrice();

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            boolean isCharged = userDAO.updateBalance(connection, bidderAccount, -amount);
            if (!isCharged) {
                connection.rollback();
                logger.error("Giao dịch thất bại: Số dư không đủ!");
                return false;
            }

            boolean isSessionUpdated = sessionDAO.updateSessionPriceAndHighest(
                    connection, sessionId, bidderAccount, amount);
            if (!isSessionUpdated) {
                connection.rollback();
                logger.error("Giao dịch thất bại: Không thể cập nhật phiên (Có thể người khác đã đặt giá cao hơn)!");
                return false;
            }

            if (previousBidder != null && !previousBidder.trim().isEmpty() && !previousBidder.equals(bidderAccount)) {
                boolean isRefunded = userDAO.updateBalance(connection, previousBidder, previousPrice);
                if (!isRefunded) {
                    connection.rollback();
                    logger.error("Giao dịch thất bại: Lỗi hoàn tiền cho người cũ!");
                    return false;
                }
            }

            boolean isBidRecorded = bidDAO.addBid(connection, sessionId, bidderAccount, amount);
            if (!isBidRecorded) {
                connection.rollback();
                logger.error("Giao dịch thất bại: Lỗi lưu lịch sử bid!");
                return false;
            }

            connection.commit();
            return true;

        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    logger.error("Lỗi khi rollback transaction!", rollbackError);
                }
            }
            logger.error("Lỗi nghiêm trọng trong quá trình đấu giá!", e);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Lỗi khi đóng kết nối database!", e);
                }
            }
        }
    }
}
