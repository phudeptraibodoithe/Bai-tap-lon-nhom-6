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
    private Gson gson = new Gson(); // Khởi tạo Gson

    // ====================================================================
    // HÀM NHẬN DỮ LIỆU TỪ TRANG CHỦ / LỊCH SỬ GỬI SANG
    // ====================================================================
    public void setItemData(AuctionSession item) {
        if (item == null) return;

        this.currentSession = item;
        updateUI();

        // TẠO JSON REQUEST ĐỂ JOIN PHÒNG
        JsonObject request = new JsonObject();
        request.addProperty("action", "JOIN");
        request.addProperty("payload", item.getId()); // Gửi ID phòng

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

    // ====================================================================
    // HÀM XỬ LÝ KHI NGƯỜI DÙNG BẤM NÚT "PLACE BID"
    // ====================================================================
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

            // TẠO JSON REQUEST CHO LỆNH BID
            JsonObject request = new JsonObject();
            request.addProperty("action", "BID");
            request.addProperty("payload", bidValue); // Gửi mức giá lên

            SocketManager.getInstance().send(gson.toJson(request));

            System.out.println("Đã gửi lệnh đặt giá JSON: " + bidValue);
            BidAmount.clear();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng chỉ nhập số hợp lệ.");
        }
    }

    // ====================================================================
    // LẮNG NGHE PHẢN HỒI TỪ SERVER BẰNG JSON
    // ====================================================================
    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                // Phân tích JSON từ Server
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.get("status").getAsString();

                switch (status) {
                    case "NEW_BID":
                        // Lấy object "data" chứa giá mới và tên người dẫn đầu
                        JsonObject data = jsonResponse.getAsJsonObject("data");
                        double newPrice = data.get("price").getAsDouble();
                        String newLeader = data.get("clientId").getAsString();

                        currentSession.setCurrentPrice(newPrice);
                        currentSession.setHighestBidderAccount(newLeader);
                        updateUI();

                        showAlert(Alert.AlertType.INFORMATION, "Cập nhật giá",
                                "Người dùng " + newLeader + " vừa đặt mức giá: " + String.format("%,.0f VNĐ", newPrice));
                        break;

                    case "BID_FAILED":
                        // Đọc thẳng câu thông báo lỗi
                        String errorMsg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Lỗi hệ thống.";
                        showAlert(Alert.AlertType.ERROR, "Đặt giá thất bại", errorMsg);
                        break;

                    case "BID_SUCCESS":
                        String successMsg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Bạn đang dẫn đầu!";
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", successMsg);
                        break;

                    case "JOIN_SUCCESS":
                        System.out.println("Vào phòng thành công. Lời chào từ server: " + jsonResponse.get("message").getAsString());
                        break;

                    case "ROOM_INFO":
                        // Server giờ sẽ gửi thẳng kiểu SỐ (Double) chứ không gửi chữ "Giá hiện tại: xxx" nữa
                        if (jsonResponse.has("data")) {
                            double currentPrice = jsonResponse.get("data").getAsDouble();
                            currentSession.setCurrentPrice(currentPrice);
                            updateUI();
                        }
                        break;
                }
            } catch (Exception e) {
                System.out.println("❌ KHÔNG THỂ ĐỌC JSON: " + response);
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