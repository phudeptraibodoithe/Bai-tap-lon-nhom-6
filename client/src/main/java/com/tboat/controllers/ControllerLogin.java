package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    private Gson gson = new Gson(); // Khởi tạo Gson

    @FXML
    public void submit(ActionEvent event) {
        String username = signText.getText().trim();
        String password = passText.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            err.setStyle("-fx-text-fill: red;");
            err.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        err.setStyle("-fx-text-fill: blue;");
        err.setText("Đang đăng nhập...");

        new Thread(() -> {
            try {
                // TẠO JSON REQUEST GỬI LÊN SERVER
                JsonObject request = new JsonObject();
                request.addProperty("action", "LOGIN");

                // Đóng gói username và password vào payload
                JsonObject payload = new JsonObject();
                payload.addProperty("username", username);
                payload.addProperty("password", password);
                request.add("payload", payload);

                // Gửi JSON đi
                SocketManager.getInstance().send(gson.toJson(request));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    err.setStyle("-fx-text-fill: red;");
                    err.setText("Lỗi kết nối: " + e.getMessage());
                });
            }
        }).start();
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                // Phân tích JSON trả về
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.get("status").getAsString();

                switch (status) {
                    case "LOGIN_ADMIN_SUCCESS":
                        changeScene(err, "Admin.fxml");
                        break;

                    case "LOGIN_SUCCESS":
                        // Lấy object data chứa thông tin user
                        JsonObject data = jsonResponse.getAsJsonObject("data");

                        String nickname = data.has("nickname") ? data.get("nickname").getAsString() : "";
                        double balance = data.has("balance") ? data.get("balance").getAsDouble() : 0.0;
                        String avatar = data.has("avatar") ? data.get("avatar").getAsString() : "default.png";
                        String description = data.has("description") ? data.get("description").getAsString() : "";

                        // Tạo User object
                        User loggedUser = new User(signText.getText(), null, nickname, balance, description, avatar);
                        UserSession.getInstance().createUserSession(loggedUser);

                        changeScene(err, "TrangChu.fxml");
                        break;

                    case "LOGIN_FAILED":
                        // Lấy mã lỗi từ message
                        String errorType = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "UNKNOWN_ERROR";
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
                        break;
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    err.setStyle("-fx-text-fill: red;");
                    err.setText("Lỗi đọc dữ liệu từ Server!");
                });
                System.out.println("❌ KHÔNG THỂ ĐỌC JSON ĐĂNG NHẬP: " + response);
            }
        });
    }
}