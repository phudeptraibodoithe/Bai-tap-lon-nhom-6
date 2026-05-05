package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.ImageUtils;
import com.tboat.utilsclient.AuctionTimer;
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

    @FXML private TextField BidAmount;
    @FXML private Button btnBid;

    private AuctionSession currentSession;
    private Gson gson = GsonUtils.getInstance();
    private AuctionTimer auctionTimer;

    public void setItemData(AuctionSession item) {
        if (item == null) return;
        this.currentSession = item;
        System.out.println("🕒 KIỂM TRA THỜI GIAN KẾT THÚC: " + currentSession.getEndTime());
        updateUI();

        // NẾU CÓ THỜI GIAN KẾT THÚC, GỌI CLASS ĐẾM GIỜ
        if (currentSession.getEndTime() != null) {
            if (auctionTimer != null) auctionTimer.stop();

            auctionTimer = new AuctionTimer(
                    currentSession.getEndTime(),
                    // Hàm onTick (Mỗi giây trôi qua)
                    timeString -> {
                        TimeRemaining.setText(timeString);
                    },
                    // Hàm onFinish (Khi hết giờ)
                    () -> {
                        TimeRemaining.setText("00 : 00 : 00");
                        TimeRemaining.setStyle("-fx-text-fill: red;");
                        if (btnBid != null) btnBid.setDisable(true);
                    }
            );
            auctionTimer.start();
        }
    }
    @FXML
    public void initialize() {
        SocketManager.getInstance().subscribe(this);
    }

    /*public void setItemData(AuctionSession item) {
        if (item == null) return;

        this.currentSession = item;
        updateUI();

        JsonObject request = new JsonObject();
        request.addProperty("action", "JOIN");
        request.addProperty("payload", item.getId());

        SocketManager.getInstance().send(gson.toJson(request));
    }*/

    private void updateUI() {
        NameItem.setText(currentSession.getName());
        IdItem.setText("ID : " + currentSession.getId());
        Description.setText(currentSession.getDescription() != null ? "Mô tả: " + currentSession.getDescription() : "Mô tả: Không có.");

        CurrentPrice.setText(String.format("%,.0f VNĐ", currentSession.getCurrentPrice()));

        double nextMin = currentSession.getCurrentPrice() + currentSession.getBidIncrease();

        String statusText = "🏆 Trạng thái: " + (currentSession.getStatusOfAuction() != null ? currentSession.getStatusOfAuction().name() : "ONGOING");
        String topBidder = currentSession.getHighestBidderAccount();
        if (topBidder != null && !topBidder.isEmpty() && !topBidder.equals("N/A")) {
            statusText += " | Đang dẫn đầu: " + topBidder;
        }
        HighestBidder.setText(statusText);

        if (ItemImage != null && currentSession.getImageURL() != null && !currentSession.getImageURL().isEmpty()) {
            Image img = ImageUtils.base64ToImage(currentSession.getImageURL());
            if (img != null) {
                ItemImage.setImage(img);
            }
        }
    }

    @FXML
    public void PlaceBid(ActionEvent event) {
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
                        double newPrice = jsonResponse.get("payload").getAsDouble();
                        String newLeader = message.replace(" vừa đặt giá mới", "");

                        currentSession.setCurrentPrice(newPrice);
                        currentSession.setHighestBidderAccount(newLeader);
                        updateUI();

                        showAlert(Alert.AlertType.INFORMATION, "Cập nhật giá",
                                "Người dùng " + newLeader + " vừa đặt mức giá: " + String.format("%,.0f VNĐ", newPrice));
                        break;

                    case "SUCCESS":
                        if (message.contains("dẫn đầu")) {
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

                    // ---> LUỒNG GIA HẠN THỜI GIAN <---
                    case "TIME_EXTENDED":
                        showAlert(Alert.AlertType.WARNING, "Đấu giá kịch tính!", message);
                        break;

                    // ---> LUỒNG KẾT THÚC PHIÊN <---
                    case "AUCTION_FINISHED":
                        currentSession.setStatusOfAuction(StatusOfAuction.ENDED);

                        // Cập nhật người chiến thắng
                        if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
                            JsonObject payloadObj = jsonResponse.get("payload").getAsJsonObject();
                            String winner = payloadObj.has("winner") ? payloadObj.get("winner").getAsString() : "Không có";
                            double finalPrice = payloadObj.has("finalPrice") ? payloadObj.get("finalPrice").getAsDouble() : currentSession.getCurrentPrice();

                            currentSession.setHighestBidderAccount(winner);
                            currentSession.setCurrentPrice(finalPrice);
                            updateUI();
                        }

                        // Khóa nút Đặt Giá lại vì đã kết thúc
                        btnBid.setDisable(true);
                        HighestBidder.setText("🏆 KẾT THÚC | Người chiến thắng: " + currentSession.getHighestBidderAccount());

                        showAlert(Alert.AlertType.INFORMATION, "Kết thúc phiên đấu giá", message);
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