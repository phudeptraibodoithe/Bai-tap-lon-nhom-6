package com.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    public boolean checkUser(String accountName){
        // nếu mà accountName có trong db thì trả về true, không thì false
        String check="select accountName from user where accountName=?";
        boolean ck=false;
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement(check);
        ){
            ps.setString(1,accountName);
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

    public boolean addUser(String accountName, String password,String nickname ){
        // thêm phần tử và db, nó chỉ thêm thôi còn check bên code logic rồi, true nếu thêm thành công
        String them="insert into user(accountName, password, nickname)values(?,?,?)";
        boolean ck=false;
        try(Connection c=DatabaseConnection.getConnection();
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

    public boolean updateUser(String accountName, String nickname, String password, double balance, String description, String avatarURL) {
        // update người dùng, true nếu update thành công
        String update = "UPDATE user SET nickname = ?, password = ?, balance = ?, description = ?, avatarURL = ? WHERE accountName = ?";
        boolean ck = false;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(update)) {
            ps.setString(1, nickname);
            ps.setString(2, password);
            ps.setDouble(3, balance);
            ps.setString(4, description);
            ps.setString(5, avatarURL);
            ps.setString(6, accountName);
            int af = ps.executeUpdate();
            if (af > 0) {
                ck = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ck;
    }

    public boolean checkLogin(String accountName,String password){
        // nếu mậtkhaaurur truùng tên đăng nhập thì true
        String check="select 1 from user where accountName=? and password=?";
        boolean ck=false;
        try(Connection c=DatabaseConnection.getConnection();
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

}
