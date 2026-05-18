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

    // Overload mới — nhận conn từ ngoài
    public boolean addHistory(Connection conn, History history) throws SQLException {
        String sql = "INSERT INTO history (auctionSessionId, winnerAccountName, finalPrice, completedAt) " +
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
// Giữ nguyên method cũ bên dưới — không xóa

    public boolean addHistory(History history) {
        String sql = "INSERT INTO history (auctionSessionId, winnerAccountName, finalPrice, completedAt) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, history.getAuctionSessionId());
            ps.setString(2, history.getWinnerAccountName());
            ps.setDouble(3, history.getFinalPrice());

            if (history.getCompletedAt() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(history.getCompletedAt()));
            } else {
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            }

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi thêm lịch sử (addHistory): ", e);
            return false;
        }
    }

    public List<Map<String, Object>> getHistoryByAccount(String accountName) {
        List<Map<String, Object>> list = new ArrayList<>();
        // Thêm p.roleType vào câu SELECT
        String sql = "SELECT p.auctionSessionId, p.roleType, i.name, h.winnerAccountName, h.finalPrice " +
                "FROM participation p " +
                "JOIN auction_session s ON p.auctionSessionId = s.id " +
                "JOIN item i ON s.itemId = i.id " +          // ← THÊM dòng này
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
                    row.put("roleType", rs.getString("roleType")); // Gửi roleType về để Client phân loại
                    row.put("name", rs.getString("name"));
                    row.put("winnerAccountName", rs.getString("winnerAccountName"));
                    row.put("finalPrice", rs.getDouble("finalPrice"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy lịch sử: ", e);
        }
        return list;
    }
}