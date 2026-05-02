package com.tboat.dao;

import com.tboat.database.DatabaseConnection;
import com.tboat.models.User;
import com.tboat.utils.ResponseCode;

import java.sql.*;

public class UserDAO {


    public ResponseCode addUser(String accountName, String password, String nickname) {
        String insertSql = "INSERT INTO user(accountName, password, nickname, balance, description, avatarURL) VALUES(?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement psInsert = c.prepareStatement(insertSql)) {
            psInsert.setString(1, accountName);
            psInsert.setString(2, password);
            psInsert.setString(3, nickname);
            psInsert.setDouble(4, 0.0);
            psInsert.setString(5, "");
            psInsert.setString(6, "");

            if (psInsert.executeUpdate() > 0) {
                return ResponseCode.SUCCESS;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // lỗi này có thể là trùng primacykey, vi phạm foreignkey, trống cái not null hoặc bị casi unique
            return ResponseCode.EXISTED;
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseCode.ERROR;
        }
        return ResponseCode.ERROR;
    }

    public boolean updateProfile(String accountName, String description, String avatarURL) {
        // Cập nhật profile người dùng, true nếu update thành công
        boolean ck = false;
        String update = "UPDATE user SET description = ?, avatarURL = ? WHERE accountName = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(update)) {
            ps.setString(1, description);
            ps.setString(2, avatarURL);
            ps.setString(3, accountName);
            int af = ps.executeUpdate();
            if (af > 0) {
                ck = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ck;
    }

    public boolean updateBalance(String accountName, double amount) {
        String sql = "UPDATE user SET balance = balance + ? WHERE accountName = ? AND (balance + ?) >= 0";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDouble(1, amount);
            ps.setString(2, accountName);
            ps.setDouble(3, amount);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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
            e.printStackTrace();
            return ResponseCode.ERROR;
        }
    }

    public User getUser(String accountName) {
        String sql = "SELECT * FROM user WHERE accountName = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("accountName"),
                            rs.getString("password"),
                            rs.getString("nickname"),
                            rs.getDouble("balance"),
                            rs.getString("description"),
                            rs.getString("avatarURL")
                    );
                }
            }
        } catch (SQLException e) { // Dùng SQLException thay vì Exception chung
            e.printStackTrace();
        }
        return null;
    }

    public double getBalance(String accountName) {
        String sql = "SELECT balance FROM user WHERE accountName = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }
}