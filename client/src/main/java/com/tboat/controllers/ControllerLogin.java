package com.tboat.controllers;

import com.tboat.models.User;
import com.tboat.socket.SocketManager;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;

public class ControllerLogin extends BaseController {

    @FXML private TextField signText, ipText;
    @FXML private PasswordField passText;
    @FXML private Label err;

    @FXML public void initialize() {
        SocketManager.getInstance().setOnMessageReceived(this::handleServerResponse);
        ipText.setText("192.168.1.27");
    }

    @FXML public void submit(ActionEvent event) {
        String username = signText.getText().trim();
        String password = passText.getText().trim();
        String ipv4 = ipText.getText().trim();

        if (username.isEmpty() || password.isEmpty() || ipv4.isEmpty()) {
            err.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        new Thread(() -> {
            try {
                SocketManager.getInstance().connect(ipv4, 8888);
                Platform.runLater(() -> {
                    if (SocketManager.getInstance().isConnected()) {
                        if (username.equals("admin") && password.equals("admin")) {
                            changeScene(err, "Admin.fxml");
                        } else {
                            SocketManager.getInstance().send("LOGIN " + username + " " + password);
                            err.setStyle("-fx-text-fill: blue;");
                            err.setText("Đang đăng nhập...");
                        }
                    } else {
                        err.setStyle("-fx-text-fill: red;");
                        err.setText("Không thể kết nối đến IP này!");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    err.setStyle("-fx-text-fill: red;");
                    err.setText("Lỗi kết nối: " + e.getMessage());
                });
            }
        }).start();
    }
    private void handleServerResponse(String response) {
        Platform.runLater(() -> {
            String[] parts = response.split("\\|", -1);
            String status = parts[0];

            if (status.equals("LOGIN_SUCCESS")) {
                String nick = (parts.length > 1) ? parts[1] : "Người dùng";
                double balance = (parts.length > 2 && !parts[2].isEmpty()) ? Double.parseDouble(parts[2]) : 0;
                String desc = (parts.length > 3) ? parts[3] : "";
                String avatar = (parts.length > 4) ? parts[4] : "";

                User loggedUser = new User(signText.getText(), null, nick, balance, desc, avatar);
                UserSession.getInstance().createUserSession(loggedUser);
                changeScene(err, "TrangChu.fxml");
            } else {
                err.setStyle("-fx-text-fill: red;");
                err.setText("Đăng nhập thất bại: " + status);
            }
        });
    }
}