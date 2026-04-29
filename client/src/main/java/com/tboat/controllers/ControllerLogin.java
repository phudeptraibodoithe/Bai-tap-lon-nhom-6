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
import java.io.IOException;

public class ControllerLogin extends BaseController implements SocketListener {

    @FXML private TextField signText, ipText;
    @FXML private PasswordField passText;
    @FXML private Label err;

    @FXML public void initialize() {
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
                if (!SocketManager.getInstance().isConnected()) {
                    SocketManager.getInstance().connect(ipv4, 8888);
                }
                Platform.runLater(() -> {
                    if (username.equals("admin") && password.equals("admin")) {
                        changeScene(err, "Admin.fxml");
                    } else {
                        SocketManager.getInstance().send("LOGIN|" + username + "|" + password);
                        err.setStyle("-fx-text-fill: blue;");
                        err.setText("Đang đăng nhập...");
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
                String errorType = parts[1];
                err.setStyle("-fx-text-fill: red;");
                switch (errorType) {
                    case "USER_NOT_FOUND": err.setText("Tài khoản không tồn tại!"); break;
                    case "WRONG_PASSWORD": err.setText("Sai mật khẩu, vui lòng thử lại."); break;
                    case "ALREADY_LOGGED_IN": err.setText("Tài khoản đang online ở nơi khác."); break;
                    case "DATABASE_ERROR": err.setText("Lỗi cơ sở dữ liệu."); break;
                    default: err.setText("Đăng nhập thất bại: " + errorType);
                }
            }
        });
    }
}