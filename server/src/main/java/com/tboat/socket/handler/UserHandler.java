package com.tboat.socket.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.network.Response;
import com.tboat.models.core.User;
import com.tboat.models.network.ServerEvent;
import com.tboat.service.NotificationService;
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
            context.sendResponse(new Response<>(ServerEvent.GET_PROFILE.name(), ServerEvent.SUCCESS.name(),
                    "Thông tin hồ sơ", user));
        } else {
            context.sendResponse(new Response<>(ServerEvent.GET_PROFILE.name(), ServerEvent.ERROR.name(),
                    "Không tìm thấy người dùng", null));
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
        context.sendResponse(new Response<>(ServerEvent.UPDATE_PROFILE.name(),
                ok ? ServerEvent.SUCCESS.name() : ServerEvent.ERROR.name(),
                ok ? "Cập nhật thành công" : "Lỗi cập nhật", null));
    }

    public void transaction(String raw) {
        double amount;
        try {
            amount = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsDouble();
        } catch (Exception e) {
            context.sendResponse(new Response<>(ServerEvent.TRANSACTION.name(), ServerEvent.ERROR.name(),
                    "Dữ liệu giao dịch không hợp lệ", null));
            return;
        }

        //Chỉ chặn amount = 0, còn lại để DB tự xử lý
        if (amount == 0) {
            context.sendResponse(new Response<>(ServerEvent.TRANSACTION.name(), ServerEvent.FAILED.name(),
                    "Số tiền giao dịch không thể bằng 0", null));
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean ok = userDAO.updateBalance(conn, context.getClientId(), amount);
            context.sendResponse(new Response<>(
                    ServerEvent.TRANSACTION.name(),
                    ok ? ServerEvent.SUCCESS.name() : ServerEvent.FAILED.name(),
                    ok ? "Giao dịch đã được xử lý!"
                            : (amount < 0 ? "Số dư không đủ để rút." : "Giao dịch thất bại."),
                    ok ? amount : null));
            if (ok) {
                User updatedUser = userDAO.getUser(context.getClientId());
                double newBalance = updatedUser != null ? updatedUser.getBalance() : 0;
                if (amount > 0) {
                    NotificationService.getInstance().onDeposit(context.getClientId(), amount, newBalance);
                } else {
                    NotificationService.getInstance().onWithdraw(context.getClientId(), Math.abs(amount), newBalance);
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi DB khi TRANSACTION [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>(ServerEvent.TRANSACTION.name(), ServerEvent.ERROR.name(),
                    "Lỗi kết nối cơ sở dữ liệu", null));
        }
    }
}
