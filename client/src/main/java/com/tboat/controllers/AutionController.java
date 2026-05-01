package com.tboat.controllers;

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

// Thêm implements SocketListener để lắng nghe Server trả về
public class AutionController extends BaseController implements SocketListener {

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

    // Biến lưu trữ phiên đấu giá đang được mở
    private AuctionSession currentSession;

    // ====================================================================
    // HÀM NHẬN DỮ LIỆU TỪ TRANG CHỦ / LỊCH SỬ GỬI SANG
    // ====================================================================
    public void setItemData(AuctionSession item) {
        if (item == null) return;

        this.currentSession = item;

        // 1. Cập nhật dữ liệu lên màn hình
        updateUI();

        // 2. Gửi lệnh JOIN lên Server để tham gia vào phòng đấu giá này
        // (Khớp với case "JOIN" trong ClientHandler của bạn)
        SocketManager.getInstance().send("JOIN|" + item.getId());
    }

    // Tách riêng hàm updateUI để tái sử dụng khi có người đặt giá mới
    private void updateUI() {
        NameItem.setText(currentSession.getName());
        IdItem.setText("ID : " + currentSession.getId());
        Description.setText(currentSession.getDescription() != null ? "Mô tả: " + currentSession.getDescription() : "Mô tả: Không có.");

        CurrentPrice.setText(String.format("%,.0f VNĐ", currentSession.getCurrentPrice()));

        // Tính toán mức giá tối thiểu tiếp theo
        double nextMin = currentSession.getCurrentPrice() + currentSession.getBidIncrease();
        NextPrice.setText(String.format("Mức giá tối thiểu tiếp theo: %,.0f VNĐ", nextMin));

        // Hiển thị người đang dẫn đầu
        String statusText = "🏆 Trạng thái: " + currentSession.getStatusOfAuction().name();
        String topBidder = currentSession.getHighestBidderAccount();
        if (topBidder != null && !topBidder.isEmpty() && !topBidder.equals("N/A")) {
            statusText += " | Đang dẫn đầu: " + topBidder;
        }
        HighestBidder.setText(statusText);

        // Xử lý Ảnh
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

            // Kiểm tra giá đặt phải lớn hơn hoặc bằng giá tối thiểu
            if (bidValue < nextMin) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đặt giá", "Giá đặt phải lớn hơn hoặc bằng: " + String.format("%,.0f VNĐ", nextMin));
                return;
            }

            // Gửi lệnh đấu giá lên Server (Khớp với case "BID" trong ClientHandler)
            SocketManager.getInstance().send("BID|" + bidValue);

            System.out.println("Đã gửi lệnh đặt giá: " + bidValue);
            BidAmount.clear(); // Xóa ô nhập sau khi gửi

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng chỉ nhập số hợp lệ (không chứa chữ cái hay ký tự đặc biệt).");
        }
    }

    // ====================================================================
    // LẮNG NGHE PHẢN HỒI TỪ SERVER
    // ====================================================================
    @Override
    public void handleServerResponse(String response) {
        // Bắt buộc bọc trong Platform.runLater để bảo vệ giao diện
        Platform.runLater(() -> {
            String[] parts = response.split("\\|", -1);

            switch (parts[0]) {
                case "NEW_BID":
                    // Server báo: NEW_BID | price | clientId
                    if (parts.length >= 3) {
                        double newPrice = Double.parseDouble(parts[1]);
                        String newLeader = parts[2];

                        // Cập nhật lại Object và Giao diện
                        currentSession.setCurrentPrice(newPrice);
                        currentSession.setHighestBidderAccount(newLeader);
                        updateUI();

                        showAlert(Alert.AlertType.INFORMATION, "Cập nhật giá", "Người dùng " + newLeader + " vừa đặt mức giá: " + String.format("%,.0f VNĐ", newPrice));
                    }
                    break;

                case "BID_FAILED":
                    showAlert(Alert.AlertType.ERROR, "Đặt giá thất bại", parts.length > 1 ? parts[1] : "Số dư không đủ hoặc lỗi hệ thống.");
                    break;

                case "JOIN_SUCCESS":
                    System.out.println("Vào phòng thành công: " + parts[1]);
                    break;

                case "ROOM_INFO":
                    // Server báo: ROOM_INFO | Giá hiện tại: xxx
                    if (parts.length > 1) {
                        try {
                            String priceStr = parts[1].replace("Giá hiện tại: ", "").trim();
                            double currentPrice = Double.parseDouble(priceStr);
                            currentSession.setCurrentPrice(currentPrice);
                            updateUI();
                        } catch (Exception e) {
                            // Bỏ qua nếu lỗi ép kiểu
                        }
                    }
                    break;
            }
        });
    }

    // ====================================================================
    // CÁC HÀM TIỆN ÍCH
    // ====================================================================
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}