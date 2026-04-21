package database;
import models.AuctionSession;
import models.StatusOfAuction;
import models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {


    public boolean addUser(String accountName, String password,String nickname ){
        // thêm phần tử và db, nó chỉ thêm thôi còn check bên code logic rồi, true nếu thêm thành công
        String them="insert into user(accountName, password, nickname)values(?,?,?)";
        boolean ck=false;
        try(Connection c= DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement(them);
        ){
            ps.setString(1,accountName);
            ps.setString(2,password);
            ps.setString(3,nickname);
            int af=ps.executeUpdate();
            if(af>0){
                ck=true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ck;
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

    public boolean updateBalance(String accountName, double balance) {
        String sql = "UPDATE user SET balance = ? WHERE accountName = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDouble(1, balance);
            ps.setString(2, accountName);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkLogin(String accountName,String password){
        // nếu mậtkhaaurur truùng tên đăng nhập thì true
        String check="select 1 from user where accountName=? and password=?";
        boolean ck=false;
        try(Connection c= DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement(check);
        ){
            ps.setString(1,accountName);
            ps.setString(2,password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ck = true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ck;
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


//    public boolean deleteUser(String accountName){
//        // xóa user, true nếu xóa thành công
//        String update = "delete from user WHERE accountName = ?";
//        boolean ck = false;
//        try (Connection c = DatabaseConnection.getConnection();
//             PreparedStatement ps = c.prepareStatement(update)) {
//            ps.setString(1, accountName);
//            int af = ps.executeUpdate();
//            if (af > 0) {
//                ck = true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ck;
//    }

//    cái code delete đang gặp vấn đề. neếu xoóa user thì k xoóa đượcvifif mắc khóa ngoại
//     liệu có nêndđặt trạng thái cho user, xoa thì trạng thái chuyển sang banned hoặc delete
}
