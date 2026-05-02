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
//        String sqlCharge = "UPDATE user SET balance = balance - ? WHERE accountName = ? AND balance >= ?";
//        String sqlUpdateSession = "UPDATE auction_session SET currentPrice = ?, highestBidderAccount = ? WHERE id = ? AND currentPrice < ?";
//        String sqlRefund = "UPDATE user SET balance = balance + ? WHERE accountName = ?";
//        String sqlInsertBid = "INSERT INTO bid (auctionSessionId, bidderAccount, bidAmount, bidTime) VALUES (?, ?, ?, NOW())";
//
//        try (Connection conn = DatabaseConnection.getConnection()) {
//            try {
//                conn.setAutoCommit(false);
//
//                // Bước 1: Trừ tiền người đặt giá mới
//                try (PreparedStatement psCharge = conn.prepareStatement(sqlCharge)) {
//                    psCharge.setDouble(1, newPrice);
//                    psCharge.setString(2, bidderAccount);
//                    psCharge.setDouble(3, newPrice);
//
//                    if (psCharge.executeUpdate() == 0) {
//                        conn.rollback();
//                        return ResponseCode.INSUFFICIENT_BALANCE;
//                    }
//                }
//
//                // Bước 2: Cập nhật phiên đấu giá (Thực hiện ngay sau khi trừ tiền để chốt vị trí)
//                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateSession)) {
//                    psUpdate.setDouble(1, newPrice);
//                    psUpdate.setString(2, bidderAccount);
//                    psUpdate.setInt(3, sessionId);
//                    psUpdate.setDouble(4, newPrice);
//
//                    if (psUpdate.executeUpdate() == 0) {
//                        conn.rollback();
//                        return ResponseCode.BID_FAILED; // Đã có người khác nhanh tay đặt giá cao hơn trước đó
//                    }
//                }
//
//                // Bước 3: Hoàn tiền cho người giữ giá cao nhất trước đó (nếu có)
//                if (previousBidder != null && !previousBidder.equals(bidderAccount)) {
//                    try (PreparedStatement psRefund = conn.prepareStatement(sqlRefund)) {
//                        psRefund.setDouble(1, previousPrice);
//                        psRefund.setString(2, previousBidder);
//                        psRefund.executeUpdate();
//                    }
//                }
//
//                // Bước 4: Lưu lịch sử đặt giá
//                try (PreparedStatement psInsert = conn.prepareStatement(sqlInsertBid)) {
//                    psInsert.setInt(1, sessionId);
//                    psInsert.setString(2, bidderAccount);
//                    psInsert.setDouble(3, newPrice);
//                    psInsert.executeUpdate();
//                }
//
//                // Hoàn tất giao dịch nếu không có bước nào bị lỗi hoặc bị rollback giữa chừng
//                conn.commit();
//                return ResponseCode.SUCCESS;
//
//            } catch (Exception innerException) {
//                // Rollback chủ động khi có bất kỳ Exception nào (lỗi mạng, rớt mạng, lỗi DB...)
//                conn.rollback();
//                throw innerException; // Ném lỗi ra khối catch bên ngoài để log
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseCode.ERROR;
//        }
        return ResponseCode.BID_FAILED;
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
