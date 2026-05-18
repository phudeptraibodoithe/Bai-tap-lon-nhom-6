package com.tboat.controllers;

import com.tboat.socket.SocketManager;
import com.tboat.utilsclient.AlertUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ControllerStart extends BaseController {

    @FXML private VBox serverPane;
    @FXML private TextField ipText;
    @FXML private Label serverErr;

    private ActionEvent pendingEvent;
    private String pendingFxml;

    // --- CONSTANTS ---
    private static final String STYLE_ERROR = "#e74c3c";
    private static final String STYLE_PROCESSING = "#3498db";

    @FXML
    public void initialize() {
        ipText.setText("192.168.1.251");
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

    @Override
    @FXML
    public void switchToRegister(ActionEvent event) {
        handleAction(event, "register.fxml");
    }

    @Override
    @FXML
    public void switchToLogin(ActionEvent event) {
        handleAction(event, "login.fxml");
    }

    @FXML
    public void connectServer(ActionEvent event) {
        String ipv4 = ipText.getText().trim();
        if (ipv4.isEmpty()) {
            AlertUtils.showStatus(serverErr, "Vui lòng nhập địa chỉ IP!", STYLE_ERROR);
            return;
        }

        ipText.setDisable(true);
        AlertUtils.showStatus(serverErr, "Đang kết nối...", STYLE_PROCESSING);

        new Thread(() -> {
            try {
                SocketManager.getInstance().connect(ipv4, 8888);

                Platform.runLater(() -> {
                    ipText.setDisable(false);
                    if (SocketManager.getInstance().isConnected()) {
                        serverPane.setVisible(false);
                        serverErr.setText("");
                        if (pendingEvent != null && pendingFxml != null) {
                            changeScene(serverErr, pendingFxml);
                            pendingEvent = null;
                            pendingFxml = null;
                        }
                    } else {
                        AlertUtils.showStatus(serverErr, "Không thể kết nối đến IP này!", STYLE_ERROR);
                    }
                });
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    ipText.setDisable(false); // Mở lại UI
                    AlertUtils.showStatus(serverErr, "Lỗi kết nối: " + ex.getMessage(), STYLE_ERROR);
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