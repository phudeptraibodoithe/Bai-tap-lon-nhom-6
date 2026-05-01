package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    private Gson gson = new Gson(); // Khởi tạo Gson

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadlichsu();
    }

    public void loadlichsu() {
        lichsu.getChildren().clear();
        String username = UserSession.getInstance().getUsername();

        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_HISTORY");
        request.addProperty("payload", username); // Gửi tên tài khoản cần xem lịch sử

        SocketManager.getInstance().send(gson.toJson(request));
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                // Phân tích phản hồi JSON từ Server
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.get("status").getAsString();

                if (status.equals("HISTORY_RES")) {
                    JsonArray historyArray = jsonResponse.getAsJsonArray("data");

                    for (JsonElement element : historyArray) {
                        JsonObject dataObj = element.getAsJsonObject();

                        String id = dataObj.get("auctionSessionId").getAsString();
                        double priceValue = dataObj.get("finalPrice").getAsDouble();

                        String time = "";
                        if (dataObj.has("completedAt")) {
                            time = dataObj.get("completedAt").getAsString().replace("T", " ");
                        }
                        String sessionName = "Phiên đấu giá #" + id;
                        String price = String.format("%,.0f VNĐ", priceValue);
                        HBox row = createHistoryRow(sessionName, "ID: " + id, "Thành công", price, true);
                        lichsu.getChildren().add(row);
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ KHÔNG THỂ ĐỌC JSON TỪ SERVER: " + response);
                e.printStackTrace();
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