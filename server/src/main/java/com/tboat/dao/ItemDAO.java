package com.tboat.dao;

import com.tboat.models.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import static com.tboat.database.DatabaseConnection.getConnection;

public class ItemDAO {
    private static final Logger logger = LoggerFactory.getLogger(ItemDAO.class);

    public int addItem(Item item) {
        String sql = "INSERT INTO item (sellerAccountName, type, name, description, imageURL) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getSellerAccountName());
            ps.setString(2, item.getType());
            ps.setString(3, item.getName());
            ps.setString(4, item.getDescription());
            ps.setString(5, item.getImageURL());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi thêm Item: ", e);
        }
        return -1;
    }

    public boolean updateItem(Item item, int itemId) {
        String sql = "UPDATE item SET name = ?, description = ?, imageURL = ?, type = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getImageURL());
            ps.setString(4, item.getType()); // ← THÊM
            ps.setInt(5, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi cập nhật Item: ", e);
            return false;
        }
    }
}