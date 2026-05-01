//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.tboat.controllers;

import com.tboat.socket.SocketManager;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ControllerStart {
    @FXML
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane serverPane;
    @FXML
    private TextField ipText;
    @FXML
    private Label serverErr;
    private ActionEvent pendingEvent;
    private String pendingFxml;

    @FXML
    public void switchToRegister(ActionEvent e) {
        this.handleAction(e, "/views/register.fxml");
    }

    @FXML
    public void switchToLogin(ActionEvent e) {
        this.handleAction(e, "/views/login.fxml");
    }

    private void handleAction(ActionEvent e, String fxmlPath) {
        if (!SocketManager.getInstance().isConnected()) {
            this.pendingEvent = e;
            this.pendingFxml = fxmlPath;
            this.serverPane.setVisible(true);
        } else {
            this.executeSwitch(e, fxmlPath);
        }

    }

    @FXML
    public void connectServer(ActionEvent event) {
        String ipv4 = this.ipText.getText().trim();
        if (ipv4.isEmpty()) {
            this.serverErr.setText("Vui lòng nhập IPv4!");
        } else {
            this.serverErr.setStyle("-fx-text-fill: blue;");
            this.serverErr.setText("Đang kết nối...");
            (new Thread(() -> {
                try {
                    SocketManager.getInstance().connect(ipv4, 8888);
                    Platform.runLater(() -> {
                        if (SocketManager.getInstance().isConnected()) {
                            this.serverPane.setVisible(false);
                            this.serverErr.setText("");
                            if (this.pendingEvent != null && this.pendingFxml != null) {
                                this.executeSwitch(this.pendingEvent, this.pendingFxml);
                            }
                        } else {
                            this.serverErr.setStyle("-fx-text-fill: red;");
                            this.serverErr.setText("Không thể kết nối đến IP này!");
                        }

                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        this.serverErr.setStyle("-fx-text-fill: red;");
                        this.serverErr.setText("Lỗi kết nối: " + ex.getMessage());
                    });
                }

            })).start();
        }
    }

    @FXML
    public void cancelConnect(ActionEvent event) {
        this.serverPane.setVisible(false);
        this.serverErr.setText("");
        this.pendingEvent = null;
        this.pendingFxml = null;
    }

    private void executeSwitch(ActionEvent e, String fxmlPath) {
        try {
            this.root = (Parent)FXMLLoader.load(this.getClass().getResource(fxmlPath));
            this.scene = ((Node)e.getSource()).getScene();
            this.scene.getStylesheets().clear();
            this.scene.getStylesheets().add(this.getClass().getResource("/styles/Button.css").toExternalForm());
            this.scene.setRoot(this.root);
        } catch (IOException ex) {
            System.err.println("Lỗi load trang: " + fxmlPath);
            ex.printStackTrace();
        }

    }
}
