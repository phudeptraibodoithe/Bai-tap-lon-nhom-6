package com.tboat.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tboat.database.DatabaseConnection;
import com.tboat.models.auction.History;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryDAO {
    private static final Logger logger = LoggerFactory.getLogger(HistoryDAO.class);

    // Overload nhận conn từ ngoài — dùng trong transaction
    public boolean addHistory(Connection conn, History history) throws SQLException {
        String sql = "INSERT INTO history (auctionSessionId, winnerAccount, finalPrice, completedAt) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, history.getAuctionSessionId());
            ps.setString(2, history.getWinnerAccountName());
            ps.setDouble(3, history.getFinalPrice());
            ps.setTimestamp(4, history.getCompletedAt() != null
                    ? Timestamp.valueOf(history.getCompletedAt())
                    : new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        }
    }

    public boolean addHistory(History history) {
        String sql = "INSERT INTO history (auctionSessionId, winnerAccount, finalPrice, completedAt) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, history.getAuctionSessionId());
            ps.setString(2, history.getWinnerAccountName());
            ps.setDouble(3, history.getFinalPrice());
            ps.setTimestamp(4, history.getCompletedAt() != null
                    ? Timestamp.valueOf(history.getCompletedAt())
                    : new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi thêm lịch sử (addHistory): ", e);
            return false;
        }
    }

    public List<Map<String, Object>> getHistoryByAccount(String accountName) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.auctionSessionId, p.roleType, i.name, h.winnerAccount, h.finalPrice " +
                "FROM participation p " +
                "JOIN auction_session s ON p.auctionSessionId = s.id " +
                "JOIN item i ON s.itemId = i.id " +
                "LEFT JOIN history h ON p.auctionSessionId = h.auctionSessionId " +
                "WHERE p.accountName = ? " +
                "ORDER BY h.completedAt DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("auctionSessionId", rs.getInt("auctionSessionId"));
                    row.put("roleType",         rs.getString("roleType"));
                    row.put("name",             rs.getString("name"));
                    row.put("winnerAccountName", rs.getString("winnerAccount")); // key giữ cho client
                    row.put("finalPrice",       rs.getDouble("finalPrice"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy lịch sử theo account: ", e);
        }
        return list;
    }

    public List<Map<String, Object>> getAllHistory() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT h.auctionSessionId, h.winnerAccount, h.finalPrice, h.completedAt, " +
                "       i.name, i.type, s.sellerAccountName " +
                "FROM history h " +
                "JOIN auction_session s ON h.auctionSessionId = s.id " +
                "JOIN item i ON s.itemId = i.id " +
                "ORDER BY h.completedAt DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("auctionSessionId",  rs.getInt("auctionSessionId"));
                row.put("winnerAccountName", rs.getString("winnerAccount"));
                row.put("finalPrice",        rs.getDouble("finalPrice"));
                row.put("completedAt",       rs.getTimestamp("completedAt") != null
                        ? rs.getTimestamp("completedAt").toLocalDateTime().toString() : null);
                row.put("name",              rs.getString("name"));
                row.put("type",              rs.getString("type"));
                row.put("sellerAccountName", rs.getString("sellerAccountName"));
                list.add(row);
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy toàn bộ lịch sử: ", e);
        }
        return list;
    }
}