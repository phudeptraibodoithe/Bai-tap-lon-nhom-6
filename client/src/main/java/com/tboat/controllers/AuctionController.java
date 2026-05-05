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
    private boolean isUserSeller = false;

    @FXML
    public void initialize() {
        SocketManager.getInstance().subscribe(this);
    }

    public void setItemData(AuctionSession item) {
        if (item == null) return;
        this.currentSession = item;

        updateUI();

        // Gửi yêu cầu vào phòng
        JsonObject request = new JsonObject();
        request.addProperty("action", "JOIN");
        request.addProperty("payload", item.getId());
        SocketManager.getInstance().send(gson.toJson(request));

        // Thiết lập giao diện UI (nút Bid, Đồng hồ) dựa trên trạng thái
        setupAuctionState();
    }

    private void setupAuctionState() {
        // 1. Kiểm tra an toàn: Nếu UI chưa kịp load xong thì không làm gì cả để tránh lỗi NullPointer
        if (btnBid == null || BidAmount == null || TimeRemaining == null) {
            return;
        }

        // 2. Tắt bộ đếm cũ (nếu có) để tránh việc nhiều Timer chạy ngầm đè lên nhau
        if (auctionTimer != null) {
            auctionTimer.stop();
        }

        StatusOfAuction status = currentSession.getStatusOfAuction();

        // ==============================================================
        // TRƯỜNG HỢP ĐẶC BIỆT: NẾU USER HIỆN TẠI LÀ NGƯỜI BÁN (SELLER)
        // ==============================================================
        if (isUserSeller) {
            btnBid.setDisable(true);
            BidAmount.setDisable(true);
            BidAmount.setPromptText("Bạn là người bán sản phẩm này...");

            // Vẫn tiếp tục chạy logic bên dưới để người bán nhìn thấy đồng hồ đếm ngược chạy
        }

        // ==============================================================
        // XỬ LÝ THEO TRẠNG THÁI PHIÊN ĐẤU GIÁ
        // ==============================================================
        if (status == StatusOfAuction.NOT_STARTED) {
            // Chưa bắt đầu: Khóa UI
            if (!isUserSeller) {
                btnBid.setDisable(true);
                BidAmount.setDisable(true);
                BidAmount.setPromptText("Chưa tới giờ đấu giá...");
            }
            TimeRemaining.setText("⏳ Sắp diễn ra...");
            TimeRemaining.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); // Màu cam

        } else if (status == StatusOfAuction.ENDED || status == StatusOfAuction.CANCELED) {
            // Đã kết thúc hoặc đã hủy: Khóa UI cứng
            btnBid.setDisable(true);
            BidAmount.setDisable(true);
            BidAmount.setPromptText("Phiên đấu giá đã khép lại.");
            TimeRemaining.setText("00 : 00 : 00");
            TimeRemaining.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        } else if (status == StatusOfAuction.ONGOING) {
            // Đang diễn ra: Mở UI cho người mua, khóa UI cho người bán
            if (isUserSeller) {
                btnBid.setDisable(true);
                BidAmount.setDisable(true);
            } else {
                btnBid.setDisable(false);
                BidAmount.setDisable(false);
                BidAmount.setPromptText("Nhập giá đặt tại đây...");
            }
            TimeRemaining.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Màu xanh lá

            // Chạy bộ đếm thời gian
            if (currentSession.getEndTime() != null) {
                auctionTimer = new AuctionTimer(
                        currentSession.getEndTime(),
                        timeString -> {
                            // CHUẨN: Đồng bộ luồng để giao diện cập nhật mượt mà, không bị crash
                            Platform.runLater(() -> {
                                if (TimeRemaining != null) {
                                    TimeRemaining.setText(timeString);
                                }
                            });
                        },
                        () -> {
                            // Khi hết giờ, tự động khóa nút Bid và chuyển trạng thái ENDED
                            Platform.runLater(() -> {
                                if (TimeRemaining != null) {
                                    TimeRemaining.setText("00 : 00 : 00");
                                    TimeRemaining.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                }
                                if (btnBid != null) btnBid.setDisable(true);
                                if (BidAmount != null) {
                                    BidAmount.setDisable(true);
                                    BidAmount.setPromptText("Đã kết thúc...");
                                }
                                currentSession.setStatusOfAuction(StatusOfAuction.ENDED);
                            });
                        }
                );
                auctionTimer.start();
            }
        }
    }

    private void updateUI() {
        NameItem.setText(currentSession.getName());
        IdItem.setText("ID : " + currentSession.getId());
        Description.setText(currentSession.getDescription() != null ? "Mô tả: " + currentSession.getDescription() : "Mô tả: Không có.");

        CurrentPrice.setText(String.format("%,.0f VNĐ", currentSession.getCurrentPrice()));

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

                    // ==============================================================
                    // BẮT TÍN HIỆU SERVER: PHIÊN ĐẤU GIÁ BẮT ĐẦU THEO THỜI GIAN THỰC
                    // ==============================================================
                    case "AUCTION_STARTED":
                        currentSession.setStatusOfAuction(StatusOfAuction.ONGOING);
                        setupAuctionState(); // Tự động mở nút Bid và chạy đồng hồ
                        showAlert(Alert.AlertType.INFORMATION, "Đã đến giờ", message);
                        break;

                    case "NEW_BID":
                        JsonObject payloadObjs = jsonResponse.get("payload").getAsJsonObject();
                        double newPrice = payloadObjs.get("newPrice").getAsDouble();
                        String newLeader = payloadObjs.get("newLeader").getAsString();

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
                        if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
                            JsonObject payload = jsonResponse.get("payload").getAsJsonObject();
                            double currentPrice = payload.get("currentPrice").getAsDouble();
                            currentSession.setCurrentPrice(currentPrice);

                            if (payload.has("isSeller")) {
                                this.isUserSeller = payload.get("isSeller").getAsBoolean();
                            } else {
                                this.isUserSeller = false;
                            }
                            setupAuctionState();
                            updateUI();
                        }
                        break;

                    case "SERVER_READY":
                        break;

                    case "TIME_EXTENDED":
                        showAlert(Alert.AlertType.WARNING, "Đấu giá kịch tính!", message);
                        break;

                    case "AUCTION_FINISHED":
                        currentSession.setStatusOfAuction(StatusOfAuction.ENDED);

                        if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
                            JsonObject payloadObj = jsonResponse.get("payload").getAsJsonObject();
                            String winner = payloadObj.has("winner") ? payloadObj.get("winner").getAsString() : "Không có";
                            double finalPrice = payloadObj.has("finalPrice") ? payloadObj.get("finalPrice").getAsDouble() : currentSession.getCurrentPrice();

                            currentSession.setHighestBidderAccount(winner);
                            currentSession.setCurrentPrice(finalPrice);
                            updateUI();
                        }

                        setupAuctionState(); // Khóa UI bằng hàm setupAuctionState
                        HighestBidder.setText("🏆 KẾT THÚC | Người chiến thắng: " + currentSession.getHighestBidderAccount());

                        showAlert(Alert.AlertType.INFORMATION, "Kết thúc", message);
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