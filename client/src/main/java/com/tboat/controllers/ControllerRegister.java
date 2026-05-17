package com.tboat.controllers;

import com.google.gson.JsonObject;
import com.tboat.socket.SocketListener;
import com.tboat.utilsclient.AlertUtils;
import com.tboat.utilsclient.SocketHelper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.logging.Logger;

public class ControllerRegister extends BaseController implements SocketListener {

    @FXML private TextField nicknameText, accountNameText, emailText, phoneText;
    @FXML private PasswordField passText, repassText;
    @FXML private Label err;

    private static final Logger logger = Logger.getLogger(ControllerRegister.class.getName());

    // --- CONSTANTS ---
    private static final String STYLE_ERROR = "#e74c3c";
    private static final String STYLE_PROCESSING = "#3498db";
    private static final String STYLE_SUCCESS = "#2ecc71";

    @FXML
    public void submit(ActionEvent event) {
        err.setText(""); // Reset thông báo cũ

        String accountName = accountNameText.getText().trim();
        String nickname = nicknameText.getText().trim();
        String password = passText.getText().trim();
        String email = emailText.getText().trim();
        String phone = phoneText.getText().trim();

        if (accountName.isEmpty() || password.isEmpty() || nickname.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            AlertUtils.showStatus(err, "Vui lòng điền đầy đủ thông tin!", STYLE_ERROR);
            return;
        }

        if (!email.endsWith("@gmail.com")) {
            AlertUtils.showStatus(err, "Email phải có đuôi @gmail.com!", STYLE_ERROR);
            return;
        }

        if (!password.equals(repassText.getText())) {
            AlertUtils.showStatus(err, "Mật khẩu xác nhận không khớp!", STYLE_ERROR);
            return;
        }

        AlertUtils.showStatus(err, "Đang gửi yêu cầu đăng ký...", STYLE_PROCESSING);

        // 👉 Đóng gói payload và gửi qua SocketHelper siêu ngắn gọn
        JsonObject payload = new JsonObject();
        payload.addProperty("accountName", accountName);
        payload.addProperty("password", password);
        payload.addProperty("nickname", nickname);
        payload.addProperty("email", email);
        payload.addProperty("phone", phone);

        SocketHelper.sendRequest("REGISTER", payload);
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                if (!"REGISTER".equals(SocketHelper.getType(response))) return;

                String status = SocketHelper.getStatus(response);
                String message = SocketHelper.getMessage(response);

                if ("SUCCESS".equals(status)) {
                    AlertUtils.showStatus(err, "Đăng ký thành công! Đang chuyển hướng...", STYLE_SUCCESS);
                    changeScene(err, "login.fxml");
                } else if ("FAILED".equals(status) || "ERROR".equals(status)) {
                    AlertUtils.showStatus(err, message.isEmpty() ? "Đăng ký thất bại, vui lòng thử lại!" : message, STYLE_ERROR);
                }
            } catch (Exception e) {
                AlertUtils.showStatus(err, "Lỗi đọc dữ liệu từ Server!", STYLE_ERROR);
                logger.severe("❌ KHÔNG THỂ ĐỌC JSON TỪ SERVER: " + response);
            }
        });
    }
}