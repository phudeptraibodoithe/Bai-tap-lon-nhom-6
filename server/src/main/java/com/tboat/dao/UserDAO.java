package com.tboat.dao;

import com.tboat.database.DatabaseConnection;
import com.tboat.models.core.User;
import com.tboat.utils.ResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    public ResponseCode addUser(String accountName, String password, String nickname,
                                String email, String phone) {
        String sql = "INSERT INTO user(accountName, password, nickname, balance, " +
                "description, avatarURL, email, phone) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountName);
            ps.setString(2, password);
            ps.setString(3, nickname);
            ps.setDouble(4, 0.0);
            ps.setString(5, "");
            ps.setString(6, "");
            ps.setString(7, email);
            ps.setString(8, phone);
            if (ps.executeUpdate() > 0) return ResponseCode.SUCCESS;
        } catch (SQLIntegrityConstraintViolationException e) {
            return ResponseCode.EXISTED;
        } catch (SQLException e) {
            logger.error("Lỗi addUser: ", e);
            return ResponseCode.ERROR;
        }
        return ResponseCode.ERROR;
    }

    // Thêm method updateProfile mới hỗ trợ đủ fields
    public boolean updateProfile(String accountName, String nickname, String description,
                                 String avatarURL, String email, String phone) {
        String sql = "UPDATE user SET nickname=?, description=?, avatarURL=?, email=?, phone=? " +
                "WHERE accountName=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nickname);
            ps.setString(2, description);
            ps.setString(3, avatarURL);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.setString(6, accountName);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Lỗi updateProfile: ", e);
            return false;
        }
    }

    public User getUser(String accountName) {
        String sql = "SELECT * FROM user WHERE accountName = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User(
                            rs.getString("accountName"),
                            rs.getString("password"),
                            rs.getString("nickname"),
                            rs.getDouble("balance"),
                            rs.getString("description"),
                            rs.getString("avatarURL")
                    );
                    u.setEmail(rs.getString("email"));
                    u.setPhone(rs.getString("phone"));
                    return u;
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi getUser: ", e);
        }
        return null;
    }

    public boolean updateBalance(Connection conn, String accountName, double amount) throws SQLException {
        String sql = "UPDATE user SET balance = balance + ? WHERE accountName = ? AND (balance + ?) >= 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, accountName);
            ps.setDouble(3, amount);
            return ps.executeUpdate() > 0;
        }
    }

    public ResponseCode checkLogin(String accountName, String password) {
        // 1. Tìm user theo tên đăng nhập
        String sql = "SELECT password FROM user WHERE accountName = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    // 2. So sánh mật khẩu
                    if (dbPassword.equals(password)) {
                        return ResponseCode.SUCCESS;
                    } else {
                        return ResponseCode.WRONG_PASSWORD; // Trả về mã: Sai mật khẩu
                    }
                } else {
                    return ResponseCode.NOT_FOUND; // Trả về mã: Không tìm thấy tài khoản
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi kiểm tra đăng nhập: ", e);
            return ResponseCode.ERROR;
        }
    }

    public String getNickname(String accountName) {
        String sql = "SELECT nickname FROM user WHERE accountName = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("nickname");
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy nickname của {}: ", accountName, e);
        }
        return accountName;
    }
}