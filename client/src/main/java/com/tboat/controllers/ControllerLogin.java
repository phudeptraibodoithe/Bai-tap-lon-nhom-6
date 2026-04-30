package com.tboat.controllers;

import com.tboat.models.User;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ControllerLogin extends BaseController implements SocketListener {

    @FXML private TextField signText;
    @FXML private PasswordField passText;
    @FXML private Label err;

    @FXML
    public void submit(ActionEvent event) {
        String username = signText.getText().trim();
        String password = passText.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            err.setStyle("-fx-text-fill: red;");
            err.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        if (username.equals("admin") && password.equals("admin")) {
            changeScene(err, "Admin.fxml");
        } else {
            err.setStyle("-fx-text-fill: blue;");
            err.setText("Đang đăng nhập...");
            new Thread(() -> {
                try {
                    SocketManager.getInstance().send("LOGIN|" + username + "|" + password);
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        err.setStyle("-fx-text-fill: red;");
                        err.setText("Lỗi kết nối: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            String[] parts = response.split("\\|", -1);
            String status = parts[0];

            if (status.equals("LOGIN_SUCCESS") && parts.length >= 5) {
                User loggedUser = new User(signText.getText(), null, parts[1],
                        Double.parseDouble(parts[2]), parts[4], parts[3]);
                UserSession.getInstance().createUserSession(loggedUser);
                changeScene(err, "TrangChu.fxml");

            } else if (status.equals("LOGIN_FAILED")) {
                // Thêm kiểm tra an toàn để tránh lỗi IndexOutOfBounds nếu parts[1] không tồn tại
                String errorType = (parts.length > 1) ? parts[1] : "UNKNOWN_ERROR";
                err.setStyle("-fx-text-fill: red;");

                switch (errorType) {
                    case "USER_NOT_FOUND":
                        err.setText("Tài khoản không tồn tại!");
                        break;
                    case "WRONG_PASSWORD":
                        err.setText("Sai mật khẩu, vui lòng thử lại.");
                        break;
                    case "ALREADY_LOGGED_IN":
                        err.setText("Tài khoản đang online ở nơi khác.");
                        break;
                    case "DATABASE_ERROR":
                        err.setText("Lỗi cơ sở dữ liệu.");
                        break;
                    default:
                        err.setText("Đăng nhập thất bại: " + errorType);
                }
            }
        });
    }
}