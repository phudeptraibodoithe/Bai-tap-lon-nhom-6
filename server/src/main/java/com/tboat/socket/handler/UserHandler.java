package com.tboat.socket.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.network.Response;
import com.tboat.models.core.User;
import com.tboat.socket.ClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class UserHandler {

    private static final Logger log = LoggerFactory.getLogger(UserHandler.class);

    private final ClientContext context;
    private final UserDAO       userDAO = new UserDAO();

    public UserHandler(ClientContext context) { this.context = context; }

    public void getProfile() {
        User user = userDAO.getUser(context.getClientId());
        if (user != null) {
            context.sendResponse(new Response<>("GET_PROFILE", "PROFILE_INFO", "Thông tin hồ sơ", user));
        } else {
            context.sendResponse(new Response<>("GET_PROFILE", "ERROR", "Không tìm thấy người dùng", null));
        }
    }

    public void updateProfile(String raw) {
        JsonObject payload = JsonParser.parseString(raw)
                .getAsJsonObject().getAsJsonObject("payload");

        String account     = context.getClientId();
        String nickname    = payload.has("nickname")    ? payload.get("nickname").getAsString()    : "";
        String description = payload.has("description") ? payload.get("description").getAsString() : "";
        String avatarURL   = payload.has("avatarURL")   ? payload.get("avatarURL").getAsString()   : "";
        String email       = payload.has("email")       ? payload.get("email").getAsString()       : "";
        String phone       = payload.has("phone")       ? payload.get("phone").getAsString()       : "";

        boolean ok = userDAO.updateProfile(account, nickname, description, avatarURL, email, phone);
        context.sendResponse(new Response<>("UPDATE_PROFILE",
                ok ? "SUCCESS" : "ERROR",
                ok ? "Cập nhật thành công" : "Lỗi cập nhật", null));
    }

    public void transaction(String raw) {
        double amount;
        try {
            amount = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsDouble();
        } catch (Exception e) {
            context.sendResponse(new Response<>("TRANSACTION", "ERROR",
                    "Dữ liệu giao dịch không hợp lệ", null));
            return;
        }

        //Chỉ chặn amount = 0, còn lại để DB tự xử lý
        if (amount == 0) {
            context.sendResponse(new Response<>("TRANSACTION", "FAILED",
                    "Số tiền giao dịch không thể bằng 0", null));
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean ok = userDAO.updateBalance(conn, context.getClientId(), amount);
            context.sendResponse(new Response<>(
                    "TRANSACTION",
                    ok ? "SUCCESS" : "FAILED",
                    ok ? "Giao dịch đã được xử lý!"
                            : (amount < 0 ? "Số dư không đủ để rút." : "Giao dịch thất bại."),
                    ok ? amount : null));
        } catch (SQLException e) {
            log.error("Lỗi DB khi TRANSACTION [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("TRANSACTION", "ERROR",
                    "Lỗi kết nối cơ sở dữ liệu", null));
        }
    }
}