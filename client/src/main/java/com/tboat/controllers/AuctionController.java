package com.tboat.controllers;

import com.google.gson.*;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;
import com.tboat.socket.SocketListener;
import com.tboat.utilsclient.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionController extends BaseController implements SocketListener {

    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

    // --- CONSTANTS cho CSS và Format ---
    private static final String STYLE_SUCCESS = "-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 17px;";
    private static final String STYLE_ERROR = "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 17px;";
    private static final String STYLE_WARNING = "-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 17px;";
    private static final String STYLE_ENDED = "-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 17px";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // --- FXML FIELDS (Đã chuẩn hóa thành camelCase) ---
    @FXML private ImageView itemImage;
    @FXML private Label timeRemaining;
    @FXML private Label nameItem;
    @FXML private Label idItem;
    @FXML private Label sellerName;
    @FXML private Label description;
    @FXML private Label currentPrice;
    @FXML private Label highestBidder;
    @FXML private TextField bidAmount;
    @FXML private Button btnBid;
    @FXML private Label lblNotification;
    @FXML private TableView<BidEntry> tableBidHistory;
    @FXML private TableColumn<BidEntry, String> colBidTime;
    @FXML private TableColumn<BidEntry, String> colBidUser;
    @FXML private TableColumn<BidEntry, Double> colBidPrice;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;
    @FXML private LineChart<String, Number> bidLineChart;

    // --- CLASS VARIABLES ---
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private final ObservableList<BidEntry> listBids = FXCollections.observableArrayList();
    private AuctionSession currentSession;
    private AuctionTimer auctionTimer;
    private boolean isUserSeller = false;

    // --- INNER CLASS ---
    public static class BidEntry {
        private final String time;
        private final String user;
        private final double price;

        public BidEntry(String time, String user, double price) {
            this.time = time;
            this.user = user;
            this.price = price;
        }
        public String getTime() { return time; }
        public String getUser() { return user; }
        public double getPrice() { return price; }
    }

    // ================== KHỞI TẠO VÀ CẤU HÌNH GIAO DIỆN ==================

    @FXML
    public void initialize() {
        if (lblNotification != null) lblNotification.setText("");
        CurrencyFormatter.attachCurrencyListener(bidAmount);
        setupBidHistoryTable();
        setupChart();
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
    }

    private void setupChart() {
        priceSeries.setName("Diễn biến giá (VNĐ)");
        bidLineChart.getData().add(priceSeries);
        bidLineChart.setAnimated(true);
    }

    private void setupBidHistoryTable() {
        if (colBidTime == null) return;

        colBidTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTime()));
        colBidUser.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUser()));
        colBidPrice.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPrice()));

        colBidPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(CurrencyFormatter.formatDisplay(price));
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                }
            }
        });

        tableBidHistory.setItems(listBids);
    }

    // ================== LOGIC THIẾT LẬP DỮ LIỆU ĐẤU GIÁ ==================

    public void setItemData(AuctionSession item) {
        if (item == null) return;
        this.currentSession = item;

        updateUI();
        SocketHelper.sendRequest("JOIN", item.getId());
        SocketHelper.sendRequest("GET_SESSION_BIDS", item.getId());
        setupAuctionState();
    }

    @Override
    public void onReload() {
        if (currentSession != null) {
            SocketHelper.sendRequest("GET_SESSION_BIDS", currentSession.getId());
            log.info("Đã tải lại lịch sử giá!");
        }
    }

    private void updateUI() {
        nameItem.setText(currentSession.getName());
        idItem.setText("ID : " + currentSession.getId());
        if (sellerName != null) sellerName.setText("Người bán: " + currentSession.getSellerAccountName());

        description.setText(currentSession.getDescription() != null ? "Mô tả: " + currentSession.getDescription() : "Mô tả: Không có.");
        currentPrice.setText(CurrencyFormatter.formatDisplay(currentSession.getCurrentPrice()));

        updateHighestBidderLabel();

        if (itemImage != null && currentSession.getImageURL() != null && !currentSession.getImageURL().isEmpty()) {
            Image img = ImageUtils.base64ToImage(currentSession.getImageURL());
            if (img != null) itemImage.setImage(img);
        }
    }

    private void updateHighestBidderLabel() {
        String statusText = "🏆 Trạng thái: " + (currentSession.getStatusOfAuction() != null ? currentSession.getStatusOfAuction().name() : "ONGOING");
        String topBidder = currentSession.getHighestBidderAccount();
        if (topBidder != null && !topBidder.isEmpty() && !topBidder.equals("N/A")) {
            statusText += " | Đang dẫn đầu: " + topBidder;
        }
        highestBidder.setText(statusText);
    }

    // Hàm chính điều phối giao diện
    private void setupAuctionState() {
        if (btnBid == null || bidAmount == null || timeRemaining == null) return;

        StatusOfAuction status = currentSession.getStatusOfAuction();

        setupInputState(status); // Cập nhật ô nhập và nút bấm
        setupTimerState(status); // Cập nhật đồng hồ đếm ngược
    }

    // 1. Hàm chuyên xử lý ô Text Input và Nút bấm (Dành cho Mua / Bán)
    private void setupInputState(StatusOfAuction status) {
        boolean disableInputs = isUserSeller || status == StatusOfAuction.ENDED ||
                status == StatusOfAuction.CANCELED || status == StatusOfAuction.NOT_STARTED;

        btnBid.setDisable(disableInputs);
        bidAmount.setDisable(disableInputs);

        if (isUserSeller) {
            bidAmount.setPromptText("Bạn là người bán sản phẩm này...");
        } else if (status == StatusOfAuction.ENDED || status == StatusOfAuction.CANCELED) {
            bidAmount.setPromptText("Phiên đấu giá đã khép lại.");
        } else if (status == StatusOfAuction.NOT_STARTED) {
            bidAmount.setPromptText("Chưa tới giờ đấu giá...");
        } else {
            bidAmount.setPromptText("Nhập giá đặt tại đây...");
        }
    }

    // 2. Hàm chuyên xử lý Đồng hồ (Hiển thị cho cả Người bán và Người mua)
    private void setupTimerState(StatusOfAuction status) {
        if (auctionTimer != null) {
            auctionTimer.stop(); // Đảm bảo reset lại timer cũ (nếu có)
        }

        if (status == StatusOfAuction.ENDED || status == StatusOfAuction.CANCELED) {
            timeRemaining.setText("00 : 00 : 00");
            timeRemaining.setStyle(STYLE_ENDED);
        } else if (status == StatusOfAuction.NOT_STARTED) {
            String startTimeStr = currentSession.getStartTime() != null
                    ? currentSession.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM"))
                    : "Sắp diễn ra...";
            timeRemaining.setText("Bắt đầu: " + startTimeStr);
            timeRemaining.setStyle(STYLE_WARNING);
        } else if (status == StatusOfAuction.ONGOING) {
            timeRemaining.setStyle(STYLE_SUCCESS);
            startTimer(); // Gọi bộ đếm thời gian
        }
    }

    private void startTimer() {
        if (currentSession.getEndTime() == null) return;

        auctionTimer = new AuctionTimer(
                currentSession.getEndTime(),
                timeString -> Platform.runLater(() -> {
                    if (timeRemaining != null) timeRemaining.setText(timeString);
                }),
                () -> Platform.runLater(() -> {
                    if (timeRemaining != null) {
                        timeRemaining.setText("00 : 00 : 00");
                        timeRemaining.setStyle(STYLE_ENDED);
                    }
                    if (btnBid != null) btnBid.setDisable(true);
                    if (bidAmount != null) {
                        bidAmount.setDisable(true);
                        bidAmount.setPromptText("Đã kết thúc...");
                    }
                    currentSession.setStatusOfAuction(StatusOfAuction.ENDED);
                })
        );
        auctionTimer.start();
    }

    // ================== HÀNH ĐỘNG NGƯỜI DÙNG ==================

    @FXML
    public void placeBid(ActionEvent event) {
        String inputBid = bidAmount.getText();
        if (inputBid == null || inputBid.trim().isEmpty()) {
            AlertUtils.showStatus(lblNotification, "Vui lòng nhập mức giá!", STYLE_ERROR);
            return;
        }

        try {
            double bidValue = CurrencyFormatter.parse(inputBid);
            double nextMin = currentSession.getCurrentPrice() + currentSession.getBidIncrease();

            if (bidValue < nextMin) {
                AlertUtils.showStatus(lblNotification, "Giá tối thiểu: " + CurrencyFormatter.formatDisplay(nextMin), STYLE_ERROR);
                return;
            }

            AlertUtils.showStatus(lblNotification, "Đang gửi lệnh đặt giá...", "-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            SocketHelper.sendRequest("BID", bidValue);
            bidAmount.clear();

        } catch (Exception e) {
            AlertUtils.showStatus(lblNotification, "Định dạng số không hợp lệ.", STYLE_ERROR);
        }
    }

    // ================== XỬ LÝ SOCKET RESPONSE ==================

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                String type    = SocketHelper.getType(response);
                String status  = SocketHelper.getStatus(response);
                String message = SocketHelper.getMessage(response);

                boolean isBroadcast  = "NEW_BID".equals(status) || "AUCTION_STARTED".equals(status)
                        || "AUCTION_FINISHED".equals(status) || "TIME_EXTENDED".equals(status)
                        || "SERVER_READY".equals(status);
                boolean isMyResponse = "JOIN".equals(type) || "BID".equals(type)
                        || "GET_SESSION_BIDS".equals(type) || "CANCEL_AUCTION".equals(type);

                if (!isBroadcast && !isMyResponse) return;

                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                switch (status) {
                    case "AUCTION_STARTED"  -> handleAuctionStarted(message);
                    case "NEW_BID"          -> handleNewBid(jsonResponse.getAsJsonObject("payload"));
                    case "SUCCESS"          -> handleSuccessMessage(jsonResponse, message);
                    case "JOIN_SUCCESS"     -> handleJoinSuccess(jsonResponse);
                    case "TIME_EXTENDED"    -> AlertUtils.showAlert(Alert.AlertType.WARNING, "Đấu giá kịch tính!", message);
                    case "AUCTION_FINISHED" -> handleAuctionFinished(jsonResponse, message);
                    case "FAILED"           -> handleFailedMessage(jsonResponse, message);
                    case "ERROR"            -> AlertUtils.showStatus(lblNotification, "⚠️ " + message, STYLE_ERROR);
                    case "SERVER_READY"     -> {}
                    default -> log.warn("Không xử lý được status: {}", status);
                }
            } catch (Exception e) {
                log.error("Lỗi đọc dữ liệu từ server: {} | {}", response, e.getMessage());
            }
        });
    }

    private void handleNewBid(JsonObject payloadObj) {
        double newPrice = payloadObj.get("newPrice").getAsDouble();
        String newLeader = payloadObj.get("newLeader").getAsString();
        LocalDateTime parsedTime = payloadObj.has("bidTime") ? TimeUtils.parseServerTime(payloadObj.get("bidTime")) : LocalDateTime.now();
        String nowTime = parsedTime.format(TIME_FORMATTER);

        currentSession.setCurrentPrice(newPrice);
        currentSession.setHighestBidderAccount(newLeader);
        updateUI();

        listBids.add(new BidEntry(nowTime, newLeader, newPrice));
        sortBidsAndChart();
        if (lblNotification != null) lblNotification.setText("");
    }

    private void handleSuccessMessage(JsonObject jsonResponse, String message) {
        if (message.contains("Lấy danh sách Bid thành công") && jsonResponse.has("payload")) {
            listBids.clear();
            for (JsonElement element : jsonResponse.getAsJsonArray("payload")) {
                JsonObject bidObj = element.getAsJsonObject();
                LocalDateTime parsedTime = TimeUtils.parseServerTime(bidObj.get("bidTime"));
                String time = (parsedTime != null) ? parsedTime.format(TIME_FORMATTER) : LocalDateTime.now().format(TIME_FORMATTER);
                String user = bidObj.has("bidderAccount") ? bidObj.get("bidderAccount").getAsString() : "Unknown";
                double price = bidObj.has("bidAmount") ? bidObj.get("bidAmount").getAsDouble() : 0.0;
                listBids.add(new BidEntry(time, user, price));
            }
            sortBidsAndChart();
        } else if (message.contains("Bạn đang dẫn đầu")) {
            AlertUtils.showStatus(lblNotification, "🎉 " + message, STYLE_SUCCESS);
        }
    }

    private void handleJoinSuccess(JsonObject jsonResponse) {
        if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
            JsonObject payload = jsonResponse.getAsJsonObject("payload");
            currentSession.setCurrentPrice(payload.get("currentPrice").getAsDouble());
            this.isUserSeller = payload.has("isSeller") && payload.get("isSeller").getAsBoolean();
            setupAuctionState();
            updateUI();
        }
    }

    private void handleAuctionFinished(JsonObject jsonResponse, String message) {
        currentSession.setStatusOfAuction(StatusOfAuction.ENDED);
        if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
            JsonObject payloadObj = jsonResponse.getAsJsonObject("payload");
            String winnerAccount = payloadObj.has("winner") ? payloadObj.get("winner").getAsString() : "Không có";
            String winnerNickname = payloadObj.has("winnerNickname") ? payloadObj.get("winnerNickname").getAsString() : winnerAccount;
            double finalPrice = payloadObj.has("finalPrice") ? payloadObj.get("finalPrice").getAsDouble() : currentSession.getCurrentPrice();

            currentSession.setHighestBidderAccount(winnerNickname);
            currentSession.setCurrentPrice(finalPrice);
            updateUI();

            updateUserBalance(winnerAccount, finalPrice);
        }
        setupAuctionState();
        highestBidder.setText("🏆 KẾT THÚC | Người chiến thắng: " + currentSession.getHighestBidderAccount());
        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Kết thúc", message);
    }

    private void handleAuctionStarted(String message) {
        currentSession.setStatusOfAuction(StatusOfAuction.ONGOING);
        setupAuctionState();
        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Đã đến giờ", message);
    }

    private void handleFailedMessage(JsonObject jsonResponse, String message) {
        if (jsonResponse.has("payload") && jsonResponse.get("payload").isJsonPrimitive()) {
            try {
                currentSession.setCurrentPrice(jsonResponse.get("payload").getAsDouble());
                updateUI();
            } catch (Exception ignored) {}
        }
        AlertUtils.showStatus(lblNotification, "❌ " + message, STYLE_ERROR);
    }

    private void updateUserBalance(String winnerAccount, double finalPrice) {
        User currentUser = UserSession.getInstance().getUser();
        String myAccount = UserSession.getInstance().getUsername();

        if (currentUser != null) {
            if (myAccount.equalsIgnoreCase(winnerAccount)) {
                currentUser.setBalance(currentUser.getBalance() - finalPrice);
                log.info("Bạn đã thắng! Trừ tiền: {}", currentUser.getBalance());
            } else if (myAccount.equalsIgnoreCase(currentSession.getSellerAccountName())) {
                currentUser.setBalance(currentUser.getBalance() + (finalPrice * 0.9));
                log.info("Đã bán! Cộng tiền: {}", currentUser.getBalance());
            }
        }
    }

    // ================== UTILITY METHODS ==================

    private void sortBidsAndChart() {
        if (listBids.isEmpty()) return;
        listBids.sort((b1, b2) -> b2.getTime().compareTo(b1.getTime())); // Mới nhất lên đầu bảng

        Platform.runLater(() -> {
            priceSeries.getData().clear();
            // Data Chart cần đi từ cũ đến mới
            ObservableList<BidEntry> chronoSorted = FXCollections.observableArrayList(listBids);
            chronoSorted.sort((b1, b2) -> b1.getTime().compareTo(b2.getTime()));

            for (BidEntry bid : chronoSorted) {
                String timeLabel = bid.getTime().length() > 11 ? bid.getTime().substring(11) : bid.getTime();
                priceSeries.getData().add(new XYChart.Data<>(timeLabel, bid.getPrice()));
            }
        });
    }
}