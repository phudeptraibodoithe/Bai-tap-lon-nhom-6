package com.tboat.socket.handler;

import com.google.gson.*;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.*;
import com.tboat.socket.ClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

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

        String desc   = payload.has("description") ? payload.get("description").getAsString() : "";
        String avatar = payload.has("avatarURL")   ? payload.get("avatarURL").getAsString()   : "";

        boolean ok = userDAO.updateProfile(context.getClientId(), desc, avatar);
        context.sendResponse(new Response<>(
                "UPDATE_PROFILE",
                ok ? "SUCCESS" : "ERROR",
                ok ? "Cập nhật thành công" : "Lỗi cập nhật",
                null));
    }

    public void transaction(String raw) {
        double amount = JsonParser.parseString(raw)
                .getAsJsonObject().get("payload").getAsDouble();

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean ok = userDAO.updateBalance(conn, context.getClientId(), amount);
            context.sendResponse(new Response<>(
                    "TRANSACTION",
                    ok ? "SUCCESS" : "FAILED",
                    ok ? "Giao dịch đã được xử lý!" : "Giao dịch bị từ chối (Số dư không đủ).",
                    ok ? amount : null));
        } catch (SQLException e) {
            log.error("Lỗi DB khi TRANSACTION [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("TRANSACTION", "ERROR", "Lỗi kết nối cơ sở dữ liệu", null));
        }
    }
}