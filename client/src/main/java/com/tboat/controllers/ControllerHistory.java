package com.tboat.controllers;

import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;

public class ControllerHistory extends BaseController implements Initializable, SocketListener {

    @FXML VBox lichsu;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadlichsu();
    }

    public void loadlichsu() {
        lichsu.getChildren().clear();
        String username = UserSession.getInstance().getUsername();
        SocketManager.getInstance().send("GET_HISTORY|" + username);
    }

    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            String[] parts = response.split("\\|");

            if (parts[0].equals("HISTORY_RES")) {
                for (int i = 1; i < parts.length; i++) {
                    String[] data = parts[i].split(";");
                    if (data.length >= 3) {
                        String sessionName = "Phiên đấu giá #" + data[0];
                        String price = String.format("%,.0f VNĐ", Double.parseDouble(data[1]));
                        String time = data[2].replace("T", " ");
                        HBox row = createHistoryRow(sessionName, "ID: " + data[0], "Thành công", price, true);
                        lichsu.getChildren().add(row);
                    }
                }
            }
        });
    }

    private HBox createHistoryRow(String name, String id, String result, String bienDong, boolean success) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(80.0);
        row.setPadding(new Insets(15, 25, 15, 25));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; " +
                "-fx-border-width: 1; -fx-border-color: #dddddd; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        Label ten = new Label(name);
        ten.setPrefWidth(250.0);
        ten.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 17px;");
        Label lblId = new Label(id);
        lblId.setAlignment(Pos.CENTER);
        lblId.setPrefWidth(120.0);
        lblId.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 15px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label kq = new Label(result);
        kq.setAlignment(Pos.CENTER);
        kq.setPrefWidth(160.0);
        String colorStatus = success ? "#27ae60" : "#e74c3c";
        kq.setStyle("-fx-text-fill: " + colorStatus + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        Label lblBienDong = new Label(bienDong);
        lblBienDong.setAlignment(Pos.CENTER_RIGHT);
        lblBienDong.setPrefWidth(180.0);
        lblBienDong.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 17px;");
        row.getChildren().addAll(ten, lblId, spacer, kq, lblBienDong);
        return row;
    }
}