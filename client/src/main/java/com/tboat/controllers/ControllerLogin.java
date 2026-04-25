package com.tboat.controllers;

import com.tboat.socket.SocketManager;
import com.tboat.utilsclient.UserSession;
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

public class ControllerLogin {

    @FXML private TextField signText;
    @FXML private PasswordField passText;
    @FXML private Label err;

    @FXML
    public void initialize() {
        // Đăng ký Listener với SocketManager: "Khi có tin nhắn, hãy gọi hàm handleServerResponse của tôi"
        // Điều này thay thế hoàn toàn cho việc tạo Thread thủ công trong Controller
        SocketManager.getInstance().setOnMessageReceived(this::handleServerResponse);

        if (!SocketManager.getInstance().isConnected()) {
            err.setText("Chưa kết nối được Server!");
        }
    }

    @FXML
    public void submit(ActionEvent event) {
        String username = signText.getText().trim();
        String password = passText.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            err.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        // Gửi lệnh qua hàm send tập trung của SocketManager
        SocketManager.getInstance().send("LOGIN " + username + " " + password);

        err.setStyle("-fx-text-fill: blue;");
        err.setText("Đang đăng nhập...");
    }

    private void handleServerResponse(String response) {
        // Luôn sử dụng Platform.runLater để đảm bảo cập nhật UI an toàn
        Platform.runLater(() -> {
            String[] parts = response.split("\\|", 3);
            String status = parts[0];

            switch (status) {
                case "LOGIN_SUCCESS":
                    if (parts.length >= 3) {
                        String nick = parts[1];
                        double balance = Double.parseDouble(parts[2]);

                        // Lưu phiên làm việc
                        UserSession.getInstance().createUserSession(signText.getText().trim(), nick, balance);

                        try {
                            loadScene("/views/TrangChu.fxml"); // Hoặc profile.fxml tùy bạn
                        } catch (IOException e) {
                            err.setText("Lỗi load màn hình!");
                        }
                    }
                    break;

                case "LOGIN_WRONG_PASSWORD":
                    err.setStyle("-fx-text-fill: red;");
                    err.setText("Sai mật khẩu!");
                    break;

                case "LOGIN_NOT_FOUND":
                    err.setStyle("-fx-text-fill: red;");
                    err.setText("Tài khoản không tồn tại!");
                    break;

                default:
                    // Bỏ qua các tin nhắn không liên quan đến Login (như tin nhắn chat...)
                    break;
            }
        });
    }

    private void loadScene(String fxmlPath) throws IOException {
        // Logic chuyển cảnh an toàn
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
    public void switchToRegister(ActionEvent e) throws IOException {
        loadScene("/views/register.fxml");
    }

    @FXML
    public void switchToStart(MouseEvent e) throws IOException {
        loadScene("/views/start.fxml");
    }
}