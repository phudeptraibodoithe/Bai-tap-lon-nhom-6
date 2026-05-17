package com.tboat.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.User;
import com.tboat.socket.SocketListener;
import com.tboat.utilsclient.AlertUtils;
import com.tboat.utilsclient.SocketHelper;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.logging.Logger;

public class ControllerLogin extends BaseController implements SocketListener {

    @FXML private TextField signText;
    @FXML private PasswordField passText;
    @FXML private Label err;

    private static final Logger log = Logger.getLogger(ControllerLogin.class.getName());

    // --- CONSTANTS ---
    private static final String STYLE_ERROR = "#e74c3c";
    private static final String STYLE_PROCESSING = "#3498db";

    @FXML
    public void submit(ActionEvent event) {
        String username = signText.getText().trim();
        String password = passText.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            AlertUtils.showStatus(err, "Vui lòng điền đầy đủ thông tin!", STYLE_ERROR);
            return;
        }

        AlertUtils.showStatus(err, "Đang đăng nhập...", STYLE_PROCESSING);

        // 👉 Đóng gói payload và gửi qua SocketHelper siêu ngắn gọn
        JsonObject payload = new JsonObject();
        payload.addProperty("accountName", username);
        payload.addProperty("password", password);

        SocketHelper.sendRequest("LOGIN", payload);
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                if (!"LOGIN".equals(SocketHelper.getType(response))) return;

                String status = SocketHelper.getStatus(response);
                String message = SocketHelper.getMessage(response);

                if ("SUCCESS".equals(status)) {
                    JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                    if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
                        JsonObject payload = jsonResponse.getAsJsonObject("payload");

                        String nickname = payload.has("nickname") ? payload.get("nickname").getAsString() : "";
                        double balance = payload.has("balance") ? payload.get("balance").getAsDouble() : 0.0;
                        String avatarURL = payload.has("avatarURL") ? payload.get("avatarURL").getAsString() : "null";
                        String description = payload.has("description") ? payload.get("description").getAsString() : "";
                        String role = payload.has("role") ? payload.get("role").getAsString() : "";

                        User loggedUser = new User(signText.getText(), null, nickname, balance, description, avatarURL);
                        UserSession.getInstance().createUserSession(loggedUser);
                        if ("admin".equalsIgnoreCase(signText.getText().trim()) || "ADMIN".equalsIgnoreCase(role)) {
                            changeScene(signText, "admin.fxml");
                        } else {
                            changeScene(signText, "TrangChu.fxml");
                        }
                    }
                } else if ("FAILED".equals(status) || "ERROR".equals(status)) {
                    // 👉 Sử dụng Switch Expression của Java hiện đại để map lỗi
                    String displayMsg = switch (message) {
                        case "USER_NOT_FOUND" -> "Tài khoản không tồn tại!";
                        case "WRONG_PASSWORD" -> "Sai mật khẩu, vui lòng thử lại.";
                        case "ALREADY_LOGGED_IN" -> "Tài khoản đang online ở nơi khác.";
                        case "DATABASE_ERROR" -> "Lỗi cơ sở dữ liệu.";
                        default -> "Đăng nhập thất bại: " + message;
                    };
                    AlertUtils.showStatus(err, displayMsg, STYLE_ERROR);
                }
            } catch (Exception e) {
                AlertUtils.showStatus(err, "Lỗi đọc dữ liệu từ Server!", STYLE_ERROR);
                log.severe("KHÔNG THỂ ĐỌC JSON ĐĂNG NHẬP: " + response);
            }
        });
    }
}