package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.AuctionSession;
import com.tboat.models.User;

import java.sql.Connection;
import java.sql.SQLException;

public class BidderRole implements TransactionRole {

    /**
     * Logic đấu giá chuẩn:
     * 1. Trừ tiền người đấu giá -> 2. Cập nhật giá phiên -> 3. Hoàn tiền người cũ -> 4. Lưu lịch sử
     */
    @Override
    public boolean execute(User user, AuctionSession session, double amount,
                           UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryBidDAO bidDAO) {

        // Trích xuất thông tin từ các object truyền vào
        String bidderAccount = user.getAccountName();
        int sessionId = session.getId();

        // Lấy thông tin người giữ giá cao nhất trước đó từ object session
        String previousBidder = session.getHighestBidderAccount();
        double previousPrice = session.getCurrentPrice();

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            // TẮT auto commit để gom nhóm thao tác thành 1 Transaction duy nhất
            conn.setAutoCommit(false);

            // 1. Trừ tiền người đặt giá mới (amount truyền số âm để trừ)
            boolean isCharged = userDAO.updateBalance(conn, bidderAccount, -amount);
            if (!isCharged) {
                conn.rollback();
                System.err.println("Giao dịch thất bại: Số dư không đủ!");
                return false;
            }

            // 2. Cập nhật phiên đấu giá
            boolean isSessionUpdated = sessionDAO.updateSessionPriceAndHighest(conn, sessionId, bidderAccount, amount);
            if (!isSessionUpdated) {
                conn.rollback();
                System.err.println("Giao dịch thất bại: Không thể cập nhật phiên (Có thể người khác đã đặt giá cao hơn)!");
                return false;
            }

            // 3. Hoàn tiền cho người giữ giá cao nhất trước đó (Nếu có)
            if (previousBidder != null && !previousBidder.trim().isEmpty() && !previousBidder.equals(bidderAccount)) {
                boolean isRefunded = userDAO.updateBalance(conn, previousBidder, previousPrice);
                if (!isRefunded) {
                    conn.rollback();
                    System.err.println("Giao dịch thất bại: Lỗi hoàn tiền cho người cũ!");
                    return false;
                }
            }

            // 4. Lưu lịch sử đặt giá (vào bảng bid)
            boolean isBidRecorded = bidDAO.addBid(conn, sessionId, bidderAccount, amount);
            if (!isBidRecorded) {
                conn.rollback();
                System.err.println("Giao dịch thất bại: Lỗi lưu lịch sử bid!");
                return false;
            }

            // THÀNH CÔNG -> Chốt tất cả thay đổi xuống Database
            conn.commit();
            return true;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Hủy bỏ mọi thay đổi nếu có lỗi (Exception)
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Trả lại trạng thái mặc định
                    conn.close();             // Đóng kết nối để trả về Connection Pool
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}