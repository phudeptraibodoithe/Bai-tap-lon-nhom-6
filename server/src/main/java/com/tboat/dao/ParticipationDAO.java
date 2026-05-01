package com.tboat.dao;

import com.tboat.database.DatabaseConnection;
import com.tboat.models.Participation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ParticipationDAO {

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
            e.printStackTrace();
        }
        return null;
    }

    public boolean addParticipation(Participation p) {
        String sql = "INSERT INTO participation (accountName, auctionSessionId, roleType) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getAccountName());
            ps.setInt(2, p.getAuctionSessionId());
            ps.setString(3, p.getRoleType());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
