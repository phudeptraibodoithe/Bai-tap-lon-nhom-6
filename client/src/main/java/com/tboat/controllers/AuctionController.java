package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.AuctionSession;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AuctionController extends BaseController implements SocketListener {

    @FXML private Button btnHome;
    @FXML private Button btnHistory;
    @FXML private Button btnPostItem;
    @FXML private Button btnProfile;

    @FXML private ImageView ItemImage;
    @FXML private Label TimeRemaining;
    @FXML private Label NameItem;
    @FXML private Label IdItem;
    @FXML private Label Description;
    @FXML private Label CurrentPrice;
    @FXML private Label HighestBidder;
    @FXML private Label NextPrice;

    @FXML private TextField BidAmount;
    @FXML private Button btnBid;

    private AuctionSession currentSession;
    private Gson gson = new Gson();

    public void setItemData(AuctionSession item) {
        if (item == null) return;

        this.currentSession = item;
        updateUI();

        JsonObject request = new JsonObject();
        request.addProperty("action", "JOIN");
        request.addProperty("payload", item.getId());

        SocketManager.getInstance().send(gson.toJson(request));
    }

    private void updateUI() {
        NameItem.setText(currentSession.getName());
        IdItem.setText("ID : " + currentSession.getId());
        Description.setText(currentSession.getDescription() != null ? "Mô tả: " + currentSession.getDescription() : "Mô tả: Không có.");

        CurrentPrice.setText(String.format("%,.0f VNĐ", currentSession.getCurrentPrice()));

        double nextMin = currentSession.getCurrentPrice() + currentSession.getBidIncrease();
        NextPrice.setText(String.format("Mức giá tối thiểu tiếp theo: %,.0f VNĐ", nextMin));

        String statusText = "🏆 Trạng thái: " + currentSession.getStatusOfAuction().name();
        String topBidder = currentSession.getHighestBidderAccount();
        if (topBidder != null && !topBidder.isEmpty() && !topBidder.equals("N/A")) {
            statusText += " | Đang dẫn đầu: " + topBidder;
        }
        HighestBidder.setText(statusText);

        if (ItemImage != null && currentSession.getImageURL() != null && !currentSession.getImageURL().isEmpty()) {
            try {
                String imageUrl = currentSession.getImageURL();
                Image img = imageUrl.startsWith("http") ? new Image(imageUrl, true) : new Image(getClass().getResourceAsStream("/images/" + imageUrl));
                if (img != null && !img.isError()) ItemImage.setImage(img);
            } catch (Exception e) {
                System.err.println("Không thể tải ảnh sản phẩm");
            }
        }
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        String inputBid = BidAmount.getText();

        if (inputBid == null || inputBid.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập mức giá bạn muốn trả!");
            return;
        }

        try {
            double bidValue = Double.parseDouble(inputBid);
            double nextMin = currentSession.getCurrentPrice() + currentSession.getBidIncrease();

            if (bidValue < nextMin) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đặt giá", "Giá đặt phải lớn hơn hoặc bằng: " + String.format("%,.0f VNĐ", nextMin));
                return;
            }

            JsonObject request = new JsonObject();
            request.addProperty("action", "BID");
            request.addProperty("payload", bidValue);

            SocketManager.getInstance().send(gson.toJson(request));

            System.out.println("Đã gửi lệnh đặt giá JSON: " + bidValue);
            BidAmount.clear();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng chỉ nhập số hợp lệ.");
        }
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : jsonResponse.get("action").getAsString();
                String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Lỗi hệ thống.";

                switch (status) {
                    case "NEW_BID":
                        // Server gửi: message="[clientId] vừa đặt giá mới", payload=price
                        double newPrice = jsonResponse.get("payload").getAsDouble();
                        String newLeader = message.replace(" vừa đặt giá mới", ""); // Tách tên người dùng từ message

                        currentSession.setCurrentPrice(newPrice);
                        currentSession.setHighestBidderAccount(newLeader);
                        updateUI();

                        showAlert(Alert.AlertType.INFORMATION, "Cập nhật giá",
                                "Người dùng " + newLeader + " vừa đặt mức giá: " + String.format("%,.0f VNĐ", newPrice));
                        break;

                    case "SUCCESS":
                        if (message.contains("dẫn đầu")) {
                            // Cập nhật giá nếu chính mình vừa bid thành công
                            if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
                                currentSession.setCurrentPrice(jsonResponse.get("payload").getAsDouble());
                                updateUI();
                            }
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", message);
                        }
                        break;

                    case "JOIN_SUCCESS":
                        System.out.println("Vào phòng thành công: " + message);
                        if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
                            double currentPrice = jsonResponse.get("payload").getAsDouble();
                            currentSession.setCurrentPrice(currentPrice);
                            updateUI();
                        }
                        break;

                    case "SERVER_READY":
                        System.out.println("Hệ thống: " + message);
                        break;

                    case "FAILED":
                    case "ERROR":
                        showAlert(Alert.AlertType.ERROR, "Thất bại", message);
                        break;
                }
            } catch (Exception e) {
                System.out.println("❌ KHÔNG THỂ ĐỌC DỮ LIỆU TỪ SERVER: " + response);
                e.printStackTrace();
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}