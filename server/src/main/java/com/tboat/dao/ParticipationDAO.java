package com.tboat.dao;

import com.tboat.database.DatabaseConnection;
import com.tboat.models.Participation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ParticipationDAO {
    private static final Logger logger = LoggerFactory.getLogger(ParticipationDAO.class);

    public Participation getRoleType(String accountName, int auctionSessionId) {
        String sql = "SELECT * FROM participation WHERE accountName = ? and auctionSessionId=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountName);
            ps.setInt(2,auctionSessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Participation(
                            accountName,auctionSessionId,rs.getString("roleType")
                    );
                }
            }
        } catch (SQLException e) { // Dùng SQLException thay vì Exception chung
            logger.error("Lỗi khi lấy Role Type: ", e);
        }
        return null;
    }

    public void addParticipation(Participation p) {
        String sql = "INSERT INTO participation (accountName, auctionSessionId, roleType) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getAccountName());
            ps.setInt(2, p.getAuctionSessionId());
            ps.setString(3, p.getRoleType());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Lỗi khi thêm Participation: ", e);
        }
    }
}