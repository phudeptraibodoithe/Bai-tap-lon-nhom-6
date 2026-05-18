package com.tboat.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.item.Item;
import com.tboat.models.item.factory.ItemFactory;
import com.tboat.models.item.factory.ItemFactoryProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tboat.database.DatabaseConnection.getConnection;

public class AuctionSessionDAO {
    private static final Logger logger = LoggerFactory.getLogger(AuctionSessionDAO.class);

    private static final String SELECT_WITH_ITEM =
            "SELECT s.*, i.id AS itemId, i.sellerAccountName, i.type, i.name, i.description, i.imageURL " +
                    "FROM auction_session s JOIN item i ON s.itemId = i.id ";

    /**
     * Thêm auction session. itemId phải được set sẵn trước khi gọi hàm này.
     */
    public int addAuctionSession(AuctionSession session) {
        String sql = "INSERT INTO auction_session (startTime, endTime, currentPrice, bidIncrease, status, highestBidderAccount, itemId) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(session.getStartTime()));
            ps.setTimestamp(2, Timestamp.valueOf(session.getEndTime()));
            ps.setDouble(3, session.getCurrentPrice());
            ps.setDouble(4, session.getBidIncrease());
            ps.setString(5, session.getStatusOfAuction().toString());
            ps.setString(6, session.getHighestBidderAccount());
            ps.setInt(7, session.getItemId());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi thêm AuctionSession: ", e);
        }
        return -1;
    }

    private AuctionSession mapResultSetToAuctionSession(ResultSet rs) throws SQLException {
        String type = rs.getString("type");
        ItemFactory itemFactory = ItemFactoryProducer.getFactory(type);
        Item item = itemFactory.createItem(
                rs.getString("sellerAccountName"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("imageURL")
        );

        AuctionSession session = new AuctionSession(
                rs.getTimestamp("startTime").toLocalDateTime(),
                rs.getTimestamp("endTime").toLocalDateTime(),
                rs.getDouble("currentPrice"),
                rs.getDouble("bidIncrease"),
                item
        );
        session.setId(rs.getInt("id"));
        session.setStatusOfAuction(StatusOfAuction.valueOf(rs.getString("status")));
        session.setHighestBidderAccount(rs.getString("highestBidderAccount"));
        session.setItemId(rs.getInt("itemId"));
        return session;
    }

    public List<AuctionSession> getAuctionsBySeller(String accountName) {
        List<AuctionSession> list = new ArrayList<>();
        String sql = SELECT_WITH_ITEM + "WHERE i.sellerAccountName = ? ORDER BY s.id DESC";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToAuctionSession(rs));
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy Auctions by Seller: ", e);
        }
        return list;
    }

    public boolean cancelAuction(int sessionId) {
        String sql = "UPDATE auction_session SET status = ? WHERE id = ? AND status NOT IN ('ENDED')";
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
        String sql = SELECT_WITH_ITEM +
                "WHERE s.status NOT IN (?, ?) ORDER BY s.status DESC, s.endTime ASC";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, StatusOfAuction.PENDING.name());
            ps.setString(2, StatusOfAuction.CANCELED.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToAuctionSession(rs));
            }
        } catch (SQLException e) {
            logger.error("Lỗi truy vấn getAvailableAuctions: ", e);
        }
        return list;
    }

    public List<AuctionSession> getPendingAuctions() {
        List<AuctionSession> list = new ArrayList<>();
        String sql = SELECT_WITH_ITEM + "WHERE s.status = 'PENDING'";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSetToAuctionSession(rs));
        } catch (Exception e) {
            logger.error("Lỗi khi lấy Pending Auctions: ", e);
        }
        return list;
    }

    // Overload mới — nhận conn từ ngoài để dùng chung transaction
// Logic y hệt method cũ, chỉ khác là không tự getConnection()
    public boolean updateSessionStatus(Connection conn, int sessionId, StatusOfAuction status) throws SQLException {
        String sql = "UPDATE auction_session SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, sessionId);
            return ps.executeUpdate() > 0;
        }
    }
// Giữ nguyên method cũ bên dưới — không xóa, code chỗ khác vẫn dùng

    public boolean updateSessionStatus(int sessionId, StatusOfAuction status) {
        String sql = "UPDATE auction_session SET status = ? WHERE id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi cập nhật Session Status: ", e);
            return false;
        }
    }

    public AuctionSession getAuctionById(int sessionId) {
        String sql = SELECT_WITH_ITEM + "WHERE s.id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToAuctionSession(rs);
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

    public void updateEndTime(int sessionId, java.time.LocalDateTime newEndTime) {
        String sql = "UPDATE auction_session SET endTime = ? WHERE id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(newEndTime));
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Lỗi khi cập nhật End Time: ", e);
        }
    }

    /**
     * Chỉ update các field thuộc auction_session. Item đã được update riêng từ bên ngoài.
     */
    public boolean updateAuction(AuctionSession session) {
        String sql = "UPDATE auction_session SET currentPrice = ?, bidIncrease = ?, startTime = ?, endTime = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, session.getCurrentPrice());
            ps.setDouble(2, session.getBidIncrease());
            ps.setTimestamp(3, Timestamp.valueOf(session.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(session.getEndTime()));
            ps.setInt(5, session.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi cập nhật thông tin Auction: ", e);
            return false;
        }
    }
}