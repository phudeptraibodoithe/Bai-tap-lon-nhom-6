package com.tboat.dao;

import com.tboat.database.DatabaseConnection;
import com.tboat.models.User;
import com.tboat.utils.ResponseCode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {


    public ResponseCode addUser(String accountName, String password, String nickname) {
        // 1. Kiểm tra xem accountName đã tồn tại chưa
        String checkSql = "SELECT 1 FROM user WHERE accountName = ?";

        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement psCheck = c.prepareStatement(checkSql)) {
                psCheck.setString(1, accountName);
                if (psCheck.executeQuery().next()) {
                    return ResponseCode.EXISTED; // Trả về mã: Đã tồn tại
                }
            }

            // 2. Nếu chưa tồn tại, tiến hành thêm mới
            String insertSql = "INSERT INTO user(accountName, password, nickname, balance,description,avatarURL) VALUES(?,?,?,?,?,?)";
            try (PreparedStatement psInsert = c.prepareStatement(insertSql)) {
                psInsert.setString(1, accountName);
                psInsert.setString(2, password);
                psInsert.setString(3, nickname);
                psInsert.setDouble(4, 0.0);
                psInsert.setString(5,"");
                psInsert.setString(6,"");

                if (psInsert.executeUpdate() > 0) {
                    return ResponseCode.SUCCESS;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ResponseCode.ERROR;
    }

    public boolean updateUser(User user) {
        // update người dùng, true nếu update thành công
        String update = "UPDATE user SET nickname = ?, password = ?, balance = ?, description = ?, avatarURL = ? WHERE accountName = ?";
        boolean ck = false;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(update)) {
            ps.setString(1, user.getNickname());
            ps.setString(2, user.getPassword());
            ps.setDouble(3, user.getBalance());
            ps.setString(4, user.getDescription());
            ps.setString(5, user.getAvatarURL());
            ps.setString(6, user.getAccountName());
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
        User user = null;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, accountName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user = new User(rs.getString("accountName"),rs.getString("password")
                            ,rs.getString("nickname"),rs.getDouble("balance"),rs.getString("description"),rs.getString("avatarURL"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
        // Sẽ trả về null nếu không tìm thấy user
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
