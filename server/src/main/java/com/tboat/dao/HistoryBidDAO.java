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

    public ResponseCode updateBidLeader(int sessionId, String bidderAccount, double newPrice, String previousBidder, double previousPrice) {
        String sqlUpdateSession = "UPDATE auction_session SET currentPrice = ?, highestBidderAccount = ? WHERE id = ?";
        String sqlInsertBid = "INSERT INTO bid (auctionSessionId, bidderAccount, bidAmount, bidTime) VALUES (?, ?, ?, NOW())";
        String sqlRefund = "UPDATE user SET balance = balance + ? WHERE accountName = ?";
        String sqlCharge = "UPDATE user SET balance = balance - ? WHERE accountName = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // BẮT BUỘC: Bắt đầu Transaction

            // Bước 1: Trừ tiền người mới (Kiểm tra số dư bằng DB constraint: balance >= 0)
            try (PreparedStatement psCharge = conn.prepareStatement(sqlCharge)) {
                psCharge.setDouble(1, newPrice);
                psCharge.setString(2, bidderAccount);
                if (psCharge.executeUpdate() == 0) {
                    conn.rollback();
                    return ResponseCode.INSUFFICIENT_BALANCE; // Không đủ tiền
                }
            }

            // Bước 2: Hoàn tiền cho người cũ (Nếu có và không phải chính mình)
            if (previousBidder != null && !previousBidder.equals(bidderAccount)) {
                try (PreparedStatement psRefund = conn.prepareStatement(sqlRefund)) {
                    psRefund.setDouble(1, previousPrice);
                    psRefund.setString(2, previousBidder);
                    psRefund.executeUpdate();
                }
            }

            // Bước 3: Cập nhật phiên đấu giá
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateSession)) {
                psUpdate.setDouble(1, newPrice);
                psUpdate.setString(2, bidderAccount);
                psUpdate.setInt(3, sessionId);
                psUpdate.executeUpdate();
            }

            // Bước 4: Lưu lịch sử đặt giá
            try (PreparedStatement psInsert = conn.prepareStatement(sqlInsertBid)) {
                psInsert.setInt(1, sessionId);
                psInsert.setString(2, bidderAccount);
                psInsert.setDouble(3, newPrice);
                psInsert.executeUpdate();
            }

            conn.commit(); // Hoàn tất toàn bộ các bước trên
            return ResponseCode.SUCCESS;

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return ResponseCode.ERROR;
        } finally {
            // Đóng connection an toàn (Try-with-resources hoặc finally)
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public String getLeadBidder(int sessionId) {
        String sql = "SELECT highestBidderAccount FROM auction_session WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("highestBidderAccount");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Trả về thông báo nếu chưa có ai bid
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
