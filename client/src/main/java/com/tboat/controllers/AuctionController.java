package com.tboat.controllers;

import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionController extends BaseController implements Initializable, SocketListener {

    @FXML private ImageView ItemImage;
    @FXML private Label TimeRemaining;
    @FXML private Label NameItem;
    @FXML private Label IdItem;
    @FXML private Label Description;
    @FXML private Label CurrentPrice;
    @FXML private Label HighestBidder;
    @FXML private Label err;
    @FXML private TextField BidAmount;
    private int currentSessionId;

    public void initData(int sessionId, String name, String desc, double price) {
        this.currentSessionId = sessionId;
        IdItem.setText("ID : " + sessionId);
        NameItem.setText(name);
        Description.setText(desc);
        CurrentPrice.setText(String.valueOf(price));
        SocketManager.getInstance().send("JOIN_ROOM|" + sessionId);
    }

    @FXML public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    @FXML public void placeBid(ActionEvent event) {
        String inputAmount = BidAmount.getText();

        if (inputAmount == null || inputAmount.trim().isEmpty()) {
            err.setStyle("-fx-text-fill: red;");
            err.setText("Vui lòng nhập mức giá!");
            return;
        }
        try {
            double bidValue = Double.parseDouble(inputAmount);
            String message = String.format("BID|" + bidValue);
            SocketManager.getInstance().send(message);
            BidAmount.clear();
            err.setStyle("-fx-text-fill: blue;");
            err.setText("Đang gửi yêu cầu đặt giá...");
        } catch (NumberFormatException e) {
            err.setStyle("-fx-text-fill: red;");
            err.setText("Vui lòng chỉ nhập số!");
        }
    }

    @Override
    public void handleServerResponse(String response) {
        javafx.application.Platform.runLater(() -> {
            // Tách chuỗi theo ký tự "|"
            String[] parts = response.split("\\|");
            String command = parts[0];

            switch (command) {
                case "NEW_BID":
                    // Format server trả về: NEW_BID|price|clientId
                    if (parts.length >= 3) {
                        CurrentPrice.setText(parts[1]);
                        HighestBidder.setText("🏆 Người dẫn đầu: " + parts[2]);
                    }
                    break;

                case "BID_SUCCESS":
                    // Format server trả về: BID_SUCCESS|Bạn đang dẫn đầu!
                    if (parts.length >= 2) {
                        err.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        err.setText("Đặt giá thành công!");
                    }
                    break;

                case "BID_FAILED":
                    // Format server trả về: BID_FAILED|Lý do thất bại
                    if (parts.length >= 2) {
                        err.setStyle("-fx-text-fill: red;");
                        err.setText(parts[1]);
                    }
                    break;
                case "ERROR":
                    // Format server trả về: ERROR|Thông báo lỗi hệ thống
                    if (parts.length >= 2) {
                        err.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        err.setText(parts[1]);
                    }
                    break;
                default:
                    System.out.println("Lệnh không hợp lệ " + response);
                    break;
            }
        });
    }
}
