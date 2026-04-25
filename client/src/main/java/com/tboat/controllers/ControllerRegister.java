package com.tboat.controllers;

import com.tboat.socket.SocketManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class ControllerRegister {

    @FXML private TextField nicknameText, accountNameText, emailText, phoneText;
    @FXML private PasswordField passText, repassText;
    @FXML private Label err;

    @FXML
    public void initialize() {
        // ĐĂNG KÝ NHẬN TIN: Thay thế hoàn toàn hàm listenToServer() cũ
        SocketManager.getInstance().setOnMessageReceived(this::handleServerResponse);

        if (!SocketManager.getInstance().isConnected()) {
            err.setText("Cảnh báo: Chưa có kết nối Socket!");
        }
    }

    @FXML
    public void submit(ActionEvent event) {
        // 1. Reset UI
        err.setStyle("-fx-text-fill: red;");
        err.setText("");

        // 2. Lấy dữ liệu
        String accountName = accountNameText.getText().trim();
        String nickname = nicknameText.getText().trim();
        String password = passText.getText().trim();
        String email = emailText.getText().trim();
        String phone = phoneText.getText().trim();

        // 3. Validate nhanh
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

        // 4. Gửi lệnh qua SocketManager
        String command = "REGISTER " + accountName + " " + password + " " + nickname;
        SocketManager.getInstance().send(command);

        err.setStyle("-fx-text-fill: blue;");
        err.setText("Đang gửi yêu cầu đăng ký...");
    }

    private void handleServerResponse(String response) {
        // Luôn chạy trong Platform.runLater để an toàn cho UI
        Platform.runLater(() -> {
            switch (response) {
                case "REG_SUCCESS":
                    err.setStyle("-fx-text-fill: green;");
                    err.setText("Đăng ký thành công! Đang chuyển hướng...");

                    // Delay một chút để người dùng kịp thấy chữ "Thành công" (Tùy chọn)
                    try {
                        loadScene("/views/login.fxml");
                    } catch (IOException e) {
                        err.setText("Lỗi khi chuyển màn hình Login!");
                    }
                    break;

                case "REG_EXISTED":
                    err.setStyle("-fx-text-fill: red;");
                    err.setText("Tên tài khoản đã tồn tại trên hệ thống!");
                    break;

                case "REG_ERROR":
                    err.setText("Máy chủ gặp sự cố khi xử lý!");
                    break;

                default:
                    break;
            }
        });
    }

    private void loadScene(String fxmlPath) throws IOException {
        if (err.getScene() == null) return;

        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) err.getScene().getWindow();
        Scene scene = new Scene(root);

        var cssResource = getClass().getResource("/styles/Button.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void switchToLogin(ActionEvent e) throws IOException {
        loadScene("/views/login.fxml");
    }

    @FXML
    public void switchToStart(MouseEvent e) throws IOException {
        loadScene("/views/start.fxml");
    }
}