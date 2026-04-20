package com.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminDAO {

    

    public boolean updateSessionStatus(int sessionId, String newStatus) {
        // Cập nhật trạng thái của phiên đấu giá (Ví dụ: APPROVED, REJECTED, ACTIVE), true nếu cập nhật thành công
        String update = "UPDATE auction_session SET status = ? WHERE id = ?";
        boolean ck = false;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(update)) {
            ps.setString(1, newStatus);
            ps.setInt(2, sessionId);
            int rs = ps.executeUpdate();
            if (rs > 0) {
                ck = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ck;
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
