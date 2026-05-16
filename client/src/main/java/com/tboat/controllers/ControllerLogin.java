package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.User;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.logging.Logger;

public class ControllerLogin extends BaseController implements SocketListener {


    @FXML private TextField signText;
    @FXML private PasswordField passText;
    @FXML private Label err;

    private final Gson gson = GsonUtils.getInstance();
    private static final Logger log = Logger.getLogger(ControllerLogin.class.getName());

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
                JsonObject request = new JsonObject();
                request.addProperty("action", "LOGIN");

                JsonObject payload = new JsonObject();
                payload.addProperty("accountName", username);
                payload.addProperty("password", password);
                request.add("payload", payload);

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
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.get("status").getAsString();
                String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "";

                switch (status) {
                    case "SUCCESS":
                        if ("Đăng nhập thành công".equals(message)) {
                            JsonObject payload = jsonResponse.getAsJsonObject("payload");

                            String nickname = payload.has("nickname") ? payload.get("nickname").getAsString() : "";
                            double balance = payload.has("balance") ? payload.get("balance").getAsDouble() : 0.0;
                            String avatarURL = payload.has("avatarURL") ? payload.get("avatarURL").getAsString() : "null";
                            String description = payload.has("description") ? payload.get("description").getAsString() : "";

                            User loggedUser = new User(signText.getText(), null, nickname, balance, description, avatarURL);
                            UserSession.getInstance().createUserSession(loggedUser);
                            String role = payload.has("role") ? payload.get("role").getAsString() : "";
                            if ("admin".equalsIgnoreCase(signText.getText().trim()) || "ADMIN".equalsIgnoreCase(role)) {
                                changeScene(err, "admin.fxml");
                                break;
                            }
                            changeScene(err, "TrangChu.fxml");
                        }
                        break;

                    case "FAILED":
                    case "ERROR":
                        err.setStyle("-fx-text-fill: red;");
                        switch (message) {
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
                                err.setText("Đăng nhập thất bại: " + message);
                        }
                        break;
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    err.setStyle("-fx-text-fill: red;");
                    err.setText("Lỗi đọc dữ liệu từ Server!");
                });
                log.severe("KHÔNG THỂ ĐỌC JSON ĐĂNG NHẬP: " + response);
            }
        });
    }
}