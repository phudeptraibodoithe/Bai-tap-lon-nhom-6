package com.tboat.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.tboat.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tboat.database.DatabaseConnection.getConnection;

public class AuctionSessionDAO {
    private static final Logger logger = LoggerFactory.getLogger(AuctionSessionDAO.class);

    public int addAuctionSession(AuctionSession session) {
        String sql = "INSERT INTO auction_session (startTime, endTime, currentPrice, bidIncrease, status, sellerAccount, type, name, description, imageURL, highestBidderAccount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = getConnection();
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
            logger.error("Lỗi khi thêm AuctionSession: ", e);
        }
        return -1;
    }

    private AuctionSession mapResultSetToAuctionSession(ResultSet rs) throws SQLException {
        String type = rs.getString("type");
        AuctionFactory factory = AuctionFactoryProducer.getFactory(type);
        AuctionSession session = factory.createAuctionSession(
                rs.getTimestamp("startTime").toLocalDateTime(),
                rs.getTimestamp("endTime").toLocalDateTime(),
                rs.getDouble("currentPrice"),
                rs.getDouble("bidIncrease"),
                rs.getString("sellerAccount"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("imageURL")
        );
        session.setId(rs.getInt("id"));
        session.setStatusOfAuction(StatusOfAuction.valueOf(rs.getString("status")));
        session.setHighestBidderAccount(rs.getString("highestBidderAccount"));
        return session;
    }

    public List<AuctionSession> getAuctionsBySeller(String accountName) {
        List<AuctionSession> list = new ArrayList<>();
        String sql = "SELECT * FROM auction_session WHERE sellerAccount = ? ORDER BY id DESC";

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAuctionSession(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy Auctions by Seller: ", e);
        }
        return list;
    }

    public boolean cancelAuction(int sessionId) {
        String sql = "UPDATE auction_session SET status = ? " +
                "WHERE id = ? AND status NOT IN ('ENDED')";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, StatusOfAuction.CANCELED.name());
            ps.setInt(2, sessionId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi hủy Auction: ", e);
            return false;
        }
    }

    public List<AuctionSession> getAvailableAuctions() {
        List<AuctionSession> list = new ArrayList<>();
        String sql = "SELECT * FROM auction_session " +
                "WHERE status NOT IN (?, ?) " +
                "ORDER BY status DESC, endTime ASC";

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, StatusOfAuction.PENDING.name());
            ps.setString(2, StatusOfAuction.CANCELED.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAuctionSession(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi truy vấn getAvailableAuctions: ", e);
        }
        return list;
    }

    public List<AuctionSession> getPendingAuctions() {
        List<AuctionSession> list = new ArrayList<>();
        String sql = "SELECT * FROM auction_session WHERE status = 'PENDING'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAuctionSession(rs));
            }
        } catch (Exception e) {
            logger.error("Lỗi khi lấy Pending Auctions: ", e);
        }
        return list;
    }

    public boolean updateSessionStatus(int sessionId, StatusOfAuction status) {
        String sql = "UPDATE auction_session SET status = ? WHERE id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name()); // An toàn tuyệt đối
            ps.setInt(2, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi cập nhật Session Status: ", e);
            return false;
        }
    }

    public AuctionSession getAuctionById(int sessionId) {
        String sql = "SELECT * FROM auction_session WHERE id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuctionSession(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy Auction by ID: ", e);
        }
        return null;
    }

    public boolean updateSessionPriceAndHighest(Connection conn, int sessionId, String bidderAccount, double newPrice) throws SQLException {
        String sql = "UPDATE auction_session SET currentPrice = ?, highestBidderAccount = ? WHERE id = ? AND currentPrice < ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setString(2, bidderAccount);
            ps.setInt(3, sessionId);
            ps.setDouble(4, newPrice);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateEndTime(int sessionId, java.time.LocalDateTime newEndTime) {
        String sql = "UPDATE auction_session SET endTime = ? WHERE id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(newEndTime));
            ps.setInt(2, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi cập nhật End Time: ", e);
            return false;
        }
    }

    public boolean updateAuction(AuctionSession session) {
        String sql = "UPDATE auction_session SET name = ?, description = ?, imageURL = ?, " +
                "currentPrice = ?, bidIncrease = ?, startTime = ?, endTime = ? WHERE id = ?";

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, session.getName());
            ps.setString(2, session.getDescription());
            ps.setString(3, session.getImageURL());
            ps.setDouble(4, session.getCurrentPrice());
            ps.setDouble(5, session.getBidIncrease());
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(session.getStartTime()));
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(session.getEndTime()));
            ps.setInt(8, session.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi cập nhật thông tin Auction: ", e);
            return false;
        }
    }
}