package com.tboat.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tboat.database.DatabaseConnection;
import com.tboat.models.Bid;
import com.tboat.models.History;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryBidDAO {
    private static final Logger logger = LoggerFactory.getLogger(HistoryBidDAO.class);

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
        String sql = "SELECT p.auctionSessionId, p.roleType, s.name, h.winnerAccountName, h.finalPrice " +
                "FROM participation p " +
                "JOIN auction_session s ON p.auctionSessionId = s.id " +
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

    public boolean addBid(Connection conn, int sessionId, String bidderAccount, double bidAmount) throws SQLException {
        String sql = "INSERT INTO bid (auctionSessionId, bidderAccount, bidAmount, bidTime) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setString(2, bidderAccount);
            ps.setDouble(3, bidAmount);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Bid> getBidsBySession(int sessionId) {
        List<Bid> bidList = new ArrayList<>();
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
            logger.error("Lỗi khi lấy bids theo session: ", e);
        }
        return bidList;
    }
}