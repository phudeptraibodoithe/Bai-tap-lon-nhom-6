package com.tboat.dao;


import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tboat.database.DatabaseConnection;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;


public class AuctionSessionDAO {
    public int addAuctionSession(AuctionSession session) {
        String sql = "INSERT INTO auction_session (startTime, endTime, currentPrice, bidIncrease, status, sellerAccount, type, name, description, imageURL, highestBidderAccount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setTimestamp(1, Timestamp.valueOf(session.getStartTime()));
            ps.setTimestamp(2, Timestamp.valueOf(session.getEndTime()));
            ps.setDouble(3, session.getCurrentPrice());
            ps.setDouble(4, session.getBidIncrease());
            ps.setString(5, session.getStatusOfAuction().toString());
            ps.setString(6, session.getSellerAccountName());
            ps.setString(7, session.getType());
            ps.setString(8, session.getName());
            ps.setString(9, session.getDescription());
            ps.setString(10, session.getImageURL());
            ps.setString(11, session.getHighestBidderAccount());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1); // Trả về ID vừa tạo
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private AuctionSession mapResultSetToAuctionSession(ResultSet rs) throws SQLException {
        return new AuctionSession(
                rs.getInt("id"),
                rs.getTimestamp("startTime").toLocalDateTime(),
                rs.getTimestamp("endTime").toLocalDateTime(),
                rs.getDouble("currentPrice"),
                rs.getDouble("bidIncrease"),
                StatusOfAuction.valueOf(rs.getString("status")),
                rs.getString("sellerAccount"),
                rs.getString("type"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("imageURL"),
                rs.getString("highestBidderAccount")
        );
    }

    public List<AuctionSession> getAuctionsBySeller(String accountName) {
        List<AuctionSession> list = new ArrayList<>();
        String sql = "SELECT * FROM auction_session WHERE sellerAccount = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAuctionSession(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean cancelAuction(int sessionId) {
        String sql = "UPDATE auction_session SET status = ? " +
                "WHERE id = ? AND status NOT IN ('ENDED')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, StatusOfAuction.CANCELED.name());
            ps.setInt(2, sessionId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<AuctionSession> getAvailableAuctions() {
        List<AuctionSession> list = new ArrayList<>();
        // Thêm dấu cách trước chữ WHERE và trước chữ ORDER để không bị dính chuỗi
        String sql = "SELECT * FROM auction_session " +
                "WHERE status NOT IN (?, ?) " +
                "ORDER BY status DESC, startTime ASC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            // Dùng enum.name() hoặc enum.toString() đều được
            ps.setString(1, StatusOfAuction.PENDING.name());
            ps.setString(2, StatusOfAuction.CANCELED.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAuctionSession(rs));
                }
            }
        } catch (SQLException e) {
            // In ra lỗi để debug nếu câu SQL có vấn đề
            System.err.println("Lỗi truy vấn getAvailableAuctions: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<AuctionSession> getPendingAuctions() {
        List<AuctionSession> list = new ArrayList<>();
        String sql = "SELECT * FROM auction_session WHERE status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAuctionSession(rs)); // Dùng luôn hàm helper này
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateSessionStatus(int sessionId, StatusOfAuction status) {
        String sql = "UPDATE auction_session SET status = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name()); // An toàn tuyệt đối
            ps.setInt(2, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public AuctionSession getAuctionById(int sessionId) {
        String sql = "SELECT * FROM auction_session WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuctionSession(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateSessionPriceAndHighest(int sessionId, String bidderAccount, double newPrice) {
        String sql = "UPDATE auction_session SET currentPrice = ?, highestBidderAccount = ? WHERE id = ? AND currentPrice < ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setString(2, bidderAccount);
            ps.setInt(3, sessionId);
            ps.setDouble(4, newPrice);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Thêm đoạn này vào cuối class AuctionSessionDAO.java
    public boolean updateEndTime(int sessionId, LocalDateTime newEndTime) {
        String sql = "UPDATE auction_session SET endTime = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(newEndTime));
            ps.setInt(2, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
