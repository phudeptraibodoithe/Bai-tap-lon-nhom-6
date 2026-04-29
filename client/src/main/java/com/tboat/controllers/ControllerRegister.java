package com.tboat.controllers;

import com.tboat.socket.SocketListener;
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

public class ControllerRegister extends BaseController implements SocketListener {

    @FXML private TextField nicknameText, accountNameText, emailText, phoneText;
    @FXML private PasswordField passText, repassText;
    @FXML private Label err;

    @FXML public void submit(ActionEvent event) {
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

        String command = "REGISTER|" + accountName + " " + password + " " + nickname;
        SocketManager.getInstance().send(command);

        err.setStyle("-fx-text-fill: blue;");
        err.setText("Đang gửi yêu cầu đăng ký...");
    }

    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            switch (response) {
                case "REG_SUCCESS":
                    err.setStyle("-fx-text-fill: green;");
                    err.setText("Đăng ký thành công! Đang chuyển hướng...");
                    changeScene(err,"login.fxml");
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
}