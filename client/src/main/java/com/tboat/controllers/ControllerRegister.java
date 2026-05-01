package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
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

    private Gson gson = new Gson(); // Khởi tạo Gson

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

        // TẠO JSON REQUEST
        JsonObject request = new JsonObject();
        request.addProperty("action", "REGISTER");

        // Đóng gói toàn bộ thông tin đăng ký vào payload
        JsonObject payload = new JsonObject();
        payload.addProperty("accountName", accountName);
        payload.addProperty("password", password);
        payload.addProperty("nickname", nickname);
        payload.addProperty("email", email);
        payload.addProperty("phone", phone);

        request.add("payload", payload);

        // Gửi chuỗi JSON qua Socket
        SocketManager.getInstance().send(gson.toJson(request));

        err.setStyle("-fx-text-fill: blue;");
        err.setText("Đang gửi yêu cầu đăng ký...");
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                // Phân tích JSON từ Server
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.get("status").getAsString();

                switch (status) {
                    case "REG_SUCCESS":
                        err.setStyle("-fx-text-fill: green;");
                        err.setText("Đăng ký thành công! Đang chuyển hướng...");
                        changeScene(err, "login.fxml");
                        break;

                    case "REG_EXISTED":
                        err.setStyle("-fx-text-fill: red;");
                        // Đọc message từ server nếu có, không thì hiển thị mặc định
                        String existMsg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Tên tài khoản đã tồn tại trên hệ thống!";
                        err.setText(existMsg);
                        break;

                    case "REG_ERROR":
                    case "ERROR":
                        err.setStyle("-fx-text-fill: red;");
                        String errorMsg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Máy chủ gặp sự cố khi xử lý!";
                        err.setText(errorMsg);
                        break;

                    default:
                        break;
                }
            } catch (Exception e) {
                err.setStyle("-fx-text-fill: red;");
                err.setText("Lỗi đọc dữ liệu từ Server!");
                System.out.println("❌ KHÔNG THỂ ĐỌC JSON TỪ SERVER: " + response);
            }
        });
    }
}