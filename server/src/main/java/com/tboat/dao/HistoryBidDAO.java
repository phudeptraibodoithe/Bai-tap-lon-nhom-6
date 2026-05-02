package com.tboat.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.tboat.database.DatabaseConnection;
import com.tboat.models.Bid;
import com.tboat.models.History;
import com.tboat.models.User;
import com.tboat.utils.ResponseCode;


public class HistoryBidDAO {

    public boolean addHistory(History history) {
        String sql = "INSERT INTO history (auctionSessionId, winnerAccountName, finalPrice, completedAt) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, history.getAuctionSessionId());
            ps.setString(2, history.getWinnerAccountName());
            ps.setDouble(3, history.getFinalPrice());

            if (history.getCompletedAt() != null) {
                ps.setTimestamp(4, java.sql.Timestamp.valueOf(history.getCompletedAt()));
            } else {
                ps.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
            }

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm lịch sử (addHistory): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<History> getHistoryByAccount(String accountName) {
        List<History> list = new ArrayList<>();
        String sql = "SELECT * FROM history WHERE winnerAccountName = ? ORDER BY completedAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    History history = new History(
                            rs.getInt("auctionSessionId"),
                            rs.getString("winnerAccountName"),
                            rs.getDouble("finalPrice"),
                            rs.getTimestamp("completedAt").toLocalDateTime()
                    );
                    list.add(history);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy lịch sử theo accountName: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public boolean addBid(int sessionId, String bidderAccount, double bidAmount) {
        String sql = "INSERT INTO bid (auctionSessionId, bidderAccount, bidAmount, bidTime) VALUES (?, ?, ?, NOW())";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setString(2, bidderAccount);
            ps.setDouble(3, bidAmount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ResponseCode updateBidLeader(int sessionId, String bidderAccount, double newPrice, String previousBidder, double previousPrice) {
        // SQL linh hoạt hơn: Nếu là người cũ thì trừ phần chênh lệch, nếu người mới thì trừ toàn bộ
        String sqlChargeNewBidder = "UPDATE user SET balance = balance - ? WHERE accountName = ? AND balance >= ?";
        String sqlUpdateSession = "UPDATE auction_session SET currentPrice = ?, highestBidderAccount = ? WHERE id = ? AND currentPrice < ?";
        String sqlRefundOldBidder = "UPDATE user SET balance = balance + ? WHERE accountName = ?";
        String sqlInsertBid = "INSERT INTO bid (auctionSessionId, bidderAccount, bidAmount, bidTime) VALUES (?, ?, ?, NOW())";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. XỬ LÝ TRỪ TIỀN
            if (bidderAccount.equals(previousBidder)) {
                // Trường hợp người cũ nâng giá: Chỉ trừ phần chênh lệch
                double diff = newPrice - previousPrice;
                try (PreparedStatement ps = conn.prepareStatement(sqlChargeNewBidder)) {
                    ps.setDouble(1, diff);
                    ps.setString(2, bidderAccount);
                    ps.setDouble(3, diff);
                    if (ps.executeUpdate() == 0) { conn.rollback(); return ResponseCode.INSUFFICIENT_BALANCE; }
                }
            } else {
                // Trường hợp người mới đặt giá: Trừ đủ số tiền mới
                try (PreparedStatement ps = conn.prepareStatement(sqlChargeNewBidder)) {
                    ps.setDouble(1, newPrice);
                    ps.setString(2, bidderAccount);
                    ps.setDouble(3, newPrice);
                    if (ps.executeUpdate() == 0) { conn.rollback(); return ResponseCode.INSUFFICIENT_BALANCE; }
                }

                // 2. HOÀN TIỀN CHO NGƯỜI CŨ (Vì người mới đã chiếm ưu thế)
                if (previousBidder != null && !previousBidder.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlRefundOldBidder)) {
                        ps.setDouble(1, previousPrice);
                        ps.setString(2, previousBidder);
                        ps.executeUpdate();
                    }
                }
            }

            // 3. CẬP NHẬT PHIÊN ĐẤU GIÁ (Kiểm tra lại giá một lần nữa để tránh Race Condition)
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateSession)) {
                ps.setDouble(1, newPrice);
                ps.setString(2, bidderAccount);
                ps.setInt(3, sessionId);
                ps.setDouble(4, newPrice);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return ResponseCode.BID_FAILED;
                }
            }

            // 4. LƯU LỊCH SỬ ĐẶT GIÁ
            try (PreparedStatement ps = conn.prepareStatement(sqlInsertBid)) {
                ps.setInt(1, sessionId);
                ps.setString(2, bidderAccount);
                ps.setDouble(3, newPrice);
                ps.executeUpdate();
            }

            conn.commit();
            return ResponseCode.SUCCESS;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return ResponseCode.ERROR;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public List<Bid> getBidsBySession(int sessionId) {
        List<Bid> bidList = new ArrayList<>();
        // Sắp xếp bidAmount giảm dần để người dẫn đầu luôn ở trên cùng
        String sql = "SELECT * FROM bid WHERE auctionSessionId = ? ORDER BY bidAmount DESC, bidTime DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bid bid = new Bid(
                            rs.getInt("id"),
                            rs.getInt("auctionSessionId"),
                            rs.getString("bidderAccount"),
                            rs.getDouble("bidAmount"),
                            rs.getTimestamp("bidTime").toLocalDateTime()
                    );
                    bidList.add(bid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bidList;
    }
}
