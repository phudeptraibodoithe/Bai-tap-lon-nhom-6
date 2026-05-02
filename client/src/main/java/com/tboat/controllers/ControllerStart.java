package com.tboat.controllers;

import com.tboat.socket.SocketManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;

public class ControllerStart extends BaseController {

    private Scene scene;
    private Parent root;

    @FXML private AnchorPane serverPane;
    @FXML private TextField ipText;
    @FXML private Label serverErr;

    private ActionEvent pendingEvent;
    private String pendingFxml;

    @FXML
    public void initialize() {
        ipText.setText("192.168.1.9");
    }

    @FXML
    private void handleAction(ActionEvent e, String fxmlPath) {
        if (!SocketManager.getInstance().isConnected()) {
            pendingEvent = e;
            pendingFxml = fxmlPath;
            serverPane.setVisible(true);
        } else {
            changeScene(serverErr, fxmlPath);
        }
    }

    // Chặn hàm gốc của BaseController, ép nó phải đi qua bộ lọc kiểm tra mạng
    @Override
    @FXML
    public void switchToRegister(ActionEvent event) {
        handleAction(event, "register.fxml");
    }

    // Chặn hàm gốc của BaseController, ép nó phải đi qua bộ lọc kiểm tra mạng
    @Override
    @FXML
    public void switchToLogin(ActionEvent event) {
        handleAction(event, "login.fxml");
    }

    @FXML
    public void connectServer(ActionEvent event) {
        String ipv4 = ipText.getText().trim();
        if (ipv4.isEmpty()) {
            serverErr.setStyle("-fx-text-fill: red;");
            serverErr.setText("Vui lòng nhập địa chỉ IP!");
            return;
        }

        // Disable UI để tránh spam click
        ipText.setDisable(true);
        serverErr.setStyle("-fx-text-fill: blue;");
        serverErr.setText("Đang kết nối...");

        new Thread(() -> {
            try {
                // Đang gọi mạng (Blocking call)
                SocketManager.getInstance().connect(ipv4, 8888);

                Platform.runLater(() -> {
                    ipText.setDisable(false); // Mở lại UI
                    if (SocketManager.getInstance().isConnected()) {
                        serverPane.setVisible(false);
                        serverErr.setText("");
                        if (pendingEvent != null && pendingFxml != null) {
                            changeScene(serverErr, pendingFxml);
                            pendingEvent = null;
                            pendingFxml = null;
                        }
                    } else {
                        serverErr.setStyle("-fx-text-fill: red;");
                        serverErr.setText("Không thể kết nối đến IP này!");
                    }
                });
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    ipText.setDisable(false); // Mở lại UI
                    serverErr.setStyle("-fx-text-fill: red;");
                    serverErr.setText("Lỗi kết nối: " + ex.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void cancelConnect(ActionEvent event) {
        serverPane.setVisible(false);
        serverErr.setText("");
        pendingEvent = null;
        pendingFxml = null;
    }
}