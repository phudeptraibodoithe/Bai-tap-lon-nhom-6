package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utils.GsonUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ControllerRegister extends BaseController implements SocketListener {

    @FXML private TextField nicknameText, accountNameText, emailText, phoneText;
    @FXML private PasswordField passText, repassText;
    @FXML private Label err;

    private Gson gson = GsonUtils.getInstance();

    @FXML
    public void submit(ActionEvent event) {
        err.setStyle("-fx-text-fill: red;");
        err.setText("");

        String accountName = accountNameText.getText().trim();
        String nickname = nicknameText.getText().trim();
        String password = passText.getText().trim();
        String email = emailText.getText().trim();
        String phone = phoneText.getText().trim();

        if (accountName.isEmpty() || password.isEmpty() || nickname.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            err.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        if (!email.endsWith("@gmail.com")) {
            err.setText("Email phải có đuôi @gmail.com!");
            return;
        }

        if (!password.equals(repassText.getText())) {
            err.setText("Mật khẩu xác nhận không khớp!");
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "REGISTER");

        JsonObject payload = new JsonObject();
        payload.addProperty("accountName", accountName);
        payload.addProperty("password", password);
        payload.addProperty("nickname", nickname);
        payload.addProperty("email", email);
        payload.addProperty("phone", phone);

        request.add("payload", payload);

        SocketManager.getInstance().send(gson.toJson(request));

        err.setStyle("-fx-text-fill: blue;");
        err.setText("Đang gửi yêu cầu đăng ký...");
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : "";
                String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "";

                if ("SUCCESS".equals(status)) {
                    err.setStyle("-fx-text-fill: green;");
                    err.setText("Đăng ký thành công! Đang chuyển hướng...");
                    changeScene(err, "login.fxml");
                } else if ("FAILED".equals(status) || "ERROR".equals(status)) {
                    err.setStyle("-fx-text-fill: red;");
                    err.setText(message.isEmpty() ? "Đăng ký thất bại, vui lòng thử lại!" : message);
                }
            } catch (Exception e) {
                err.setStyle("-fx-text-fill: red;");
                err.setText("Lỗi đọc dữ liệu từ Server!");
                System.out.println("❌ KHÔNG THỂ ĐỌC JSON TỪ SERVER: " + response);
            }
        });
    }
}