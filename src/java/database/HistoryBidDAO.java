package database;

import models.Bid;
import models.History;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryBidDAO {

    public boolean addHistory(History history) {
        String sql = "INSERT INTO history (auctionSessionId, winnerAccount, finalPrice, completedAt) VALUES (?, ?, ?, ?)";

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

    public boolean updateBidLeader(int sessionId, String bidderAccount, double newPrice) {
        // Câu lệnh cập nhật giá hiện tại của phiên đấu giá
        String sqlUpdateSession = "UPDATE auction_session SET currentPrice = ? WHERE id = ?";
        String sqlInsertBid = "INSERT INTO bid (auctionSessionId, bidderAccount, bidAmount, bidTime) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); //Không cho tự động lưu để đảm bảo cả 2 lệnh cùng thành công

// cập nhaajt giá
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateSession)) {
                psUpdate.setDouble(1, newPrice);
                psUpdate.setInt(2, sessionId);
                psUpdate.executeUpdate();
            }

            // lưu đối tượng bid vào db
            try (PreparedStatement psInsert = conn.prepareStatement(sqlInsertBid)) {
                psInsert.setInt(1, sessionId);
                psInsert.setString(2, bidderAccount);
                psInsert.setDouble(3, newPrice);
                psInsert.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
                psInsert.executeUpdate();
            }

            conn.commit(); // bắt đầu lưu vào db
            return true;

        } catch (SQLException e) {
            // Nếu có bất kỳ lỗi nào, rollback lại hết để tránh sai lệch
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            // Trả lại trạng thái cũ và đóng kết nối
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public String getLeadBidder(int sessionId) {
        String sql = "SELECT bidderAccount FROM bid WHERE auctionSessionId = ? ORDER BY bidAmount DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("bidderAccount");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Chưa có người đặt"; // Trả về thông báo nếu chưa có ai bid
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
