package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.ImageUtils;
import com.tboat.utilsclient.AuctionTimer;
import com.tboat.utilsclient.CurrencyFormatter;
import com.tboat.utilsclient.HeaderUtils;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionController extends BaseController implements SocketListener {

    @FXML private ImageView ItemImage;
    @FXML private Label TimeRemaining;
    @FXML private Label NameItem;
    @FXML private Label IdItem;
    @FXML private Label SellerName;
    @FXML private Label Description;
    @FXML private Label CurrentPrice;
    @FXML private Label HighestBidder;
    @FXML private TextField BidAmount;
    @FXML private Button btnBid;
    @FXML private Label thongbao;
    @FXML private TableView<BidEntry> tableBidHistory;
    @FXML private TableColumn<BidEntry, String> colBidTime;
    @FXML private TableColumn<BidEntry, String> colBidUser;
    @FXML private TableColumn<BidEntry, Double> colBidPrice;

    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    private ObservableList<BidEntry> listBids = FXCollections.observableArrayList();
    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);
    private AuctionSession currentSession;
    private Gson gson = GsonUtils.getInstance();
    private AuctionTimer auctionTimer;
    private boolean isUserSeller = false;

    public static class BidEntry {
        private String time;
        private String user;
        private double price;

        public BidEntry(String time, String user, double price) {
            this.time = time;
            this.user = user;
            this.price = price;
        }
        public String getTime() { return time; }
        public String getUser() { return user; }
        public double getPrice() { return price; }
    }

    @FXML
    public void initialize() {
        BidAmount.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            String cleanString = newValue.replaceAll("[^\\d]", "");
            if (cleanString.isEmpty()) {
                BidAmount.setText("");
                return;
            }

            try {
                double parsed = Double.parseDouble(cleanString);
                String formatted = CurrencyFormatter.formatInput(parsed);

                if (!newValue.equals(formatted)) {
                    BidAmount.setText(formatted);
                    Platform.runLater(() -> BidAmount.positionCaret(formatted.length()));
                }
            } catch (NumberFormatException e) {
                BidAmount.setText(oldValue);
            }
        });

        if (thongbao != null) {
            thongbao.setText("");
        }

        setupBidHistoryTable();
        // Cần import cái Controller để truyền vào hàm bắt sự kiện F5
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
    }

    // Ghi đè hàm Reload để ấn F5 thì tải lại lịch sử đấu giá
    @Override
    public void onReload() {
        if (currentSession != null) {
            JsonObject reqHistory = new JsonObject();
            reqHistory.addProperty("action", "GET_SESSION_BIDS");
            reqHistory.addProperty("payload", currentSession.getId());
            SocketManager.getInstance().send(gson.toJson(reqHistory));
            System.out.println("Đã tải lại lịch sử giá!");
        }
    }

    private void setupBidHistoryTable() {
        if (colBidTime == null || colBidUser == null || colBidPrice == null) return;

        colBidTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTime()));
        colBidUser.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUser()));
        colBidPrice.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPrice()));

        colBidPrice.setCellFactory(column -> new TableCell<BidEntry, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(CurrencyFormatter.format(price));
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                }
            }
        });

        tableBidHistory.setItems(listBids);
    }

    private void setThongBao(String msg, boolean isSuccess) {
        Platform.runLater(() -> {
            if (thongbao != null) {
                thongbao.setText(msg);
                if (isSuccess) {
                    thongbao.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                } else {
                    thongbao.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });
    }

    public void setItemData(AuctionSession item) {
        if (item == null) return;
        this.currentSession = item;

        updateUI();

        JsonObject request = new JsonObject();
        request.addProperty("action", "JOIN");
        request.addProperty("payload", item.getId());
        SocketManager.getInstance().send(gson.toJson(request));

        JsonObject reqHistory = new JsonObject();
        reqHistory.addProperty("action", "GET_SESSION_BIDS");
        reqHistory.addProperty("payload", item.getId());
        SocketManager.getInstance().send(gson.toJson(reqHistory));

        setupAuctionState();
    }

    private void setupAuctionState() {
        if (btnBid == null || BidAmount == null || TimeRemaining == null) return;

        if (auctionTimer != null) auctionTimer.stop();

        StatusOfAuction status = currentSession.getStatusOfAuction();

        if (isUserSeller) {
            btnBid.setDisable(true);
            BidAmount.setDisable(true);
            BidAmount.setPromptText("Bạn là người bán sản phẩm này...");
        }

        if (status == StatusOfAuction.NOT_STARTED) {
            if (!isUserSeller) {
                btnBid.setDisable(true);
                BidAmount.setDisable(true);
                BidAmount.setPromptText("Chưa tới giờ đấu giá...");
            }

            // ĐÃ SỬA: Lấy thời gian bắt đầu từ currentSession và Format lại
            String startTimeStr = "Sắp diễn ra...";
            if (currentSession.getStartTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM");
                startTimeStr = currentSession.getStartTime().format(formatter);
            }

            TimeRemaining.setText("Bắt đầu: " + startTimeStr);
            // Mình giảm size chữ xuống một chút để ngày tháng dài có thể hiển thị vừa khung
            TimeRemaining.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 20px;");

        } else if (status == StatusOfAuction.ENDED || status == StatusOfAuction.CANCELED) {
            btnBid.setDisable(true);
            BidAmount.setDisable(true);
            BidAmount.setPromptText("Phiên đấu giá đã khép lại.");
            TimeRemaining.setText("00 : 00 : 00");
            TimeRemaining.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 28px;");

        } else if (status == StatusOfAuction.ONGOING) {
            if (isUserSeller) {
                btnBid.setDisable(true);
                BidAmount.setDisable(true);
            } else {
                btnBid.setDisable(false);
                BidAmount.setDisable(false);
                BidAmount.setPromptText("Nhập giá đặt tại đây...");
            }
            TimeRemaining.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 28px;");

            if (currentSession.getEndTime() != null) {
                auctionTimer = new AuctionTimer(
                        currentSession.getEndTime(),
                        timeString -> {
                            Platform.runLater(() -> {
                                if (TimeRemaining != null) TimeRemaining.setText(timeString);
                            });
                        },
                        () -> {
                            Platform.runLater(() -> {
                                if (TimeRemaining != null) {
                                    TimeRemaining.setText("00 : 00 : 00");
                                    TimeRemaining.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 28px;");
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

        if (SellerName != null) {
            SellerName.setText("Người bán: " + currentSession.getSellerAccountName());
        }

        Description.setText(currentSession.getDescription() != null ? "Mô tả: " + currentSession.getDescription() : "Mô tả: Không có.");

        CurrentPrice.setText(CurrencyFormatter.format(currentSession.getCurrentPrice()));

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
            setThongBao("Vui lòng nhập mức giá!", false);
            return;
        }

        try {
            double bidValue = CurrencyFormatter.parse(inputBid);
            double nextMin = currentSession.getCurrentPrice() + currentSession.getBidIncrease();

            if (bidValue < nextMin) {
                setThongBao("Giá tối thiểu: " + CurrencyFormatter.format(nextMin), false);
                return;
            }

            setThongBao("Đang gửi lệnh đặt giá...", true);
            thongbao.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");

            JsonObject request = new JsonObject();
            request.addProperty("action", "BID");
            request.addProperty("payload", bidValue);

            SocketManager.getInstance().send(gson.toJson(request));
            BidAmount.clear();

        } catch (Exception e) {
            setThongBao("Định dạng số không hợp lệ.", false);
        }
    }

    private String parseServerTime(JsonElement timeElement) {
        if (timeElement == null || timeElement.isJsonNull()) {
            return "";
        }
        try {
            if (timeElement.isJsonPrimitive() && timeElement.getAsJsonPrimitive().isNumber()) {
                long timestamp = timeElement.getAsLong();
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(timestamp),
                        java.time.ZoneId.systemDefault()
                );
                return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                String time = timeElement.getAsString();
                if (time.contains("T")) {
                    time = time.replace("T", " ").substring(0, 19);
                }
                return time;
            }
        } catch (Exception e) {
            return "";
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

                    case "AUCTION_STARTED":
                        currentSession.setStatusOfAuction(StatusOfAuction.ONGOING);
                        setupAuctionState();
                        showAlert(Alert.AlertType.INFORMATION, "Đã đến giờ", message);
                        break;

                    case "NEW_BID":
                        JsonObject payloadObjs = jsonResponse.get("payload").getAsJsonObject();
                        double newPrice = payloadObjs.get("newPrice").getAsDouble();
                        String newLeader = payloadObjs.get("newLeader").getAsString();

                        currentSession.setCurrentPrice(newPrice);
                        currentSession.setHighestBidderAccount(newLeader);
                        updateUI();

                        String nowTime = "";
                        if (payloadObjs.has("bidTime")) {
                            nowTime = parseServerTime(payloadObjs.get("bidTime"));
                        }
                        if (nowTime.isEmpty()) {
                            nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        }

                        listBids.add(new BidEntry(nowTime, newLeader, newPrice));
                        listBids.sort((b1, b2) -> b2.getTime().compareTo(b1.getTime()));

                        // =================================================================
                        // ĐÃ SỬA: Xóa thông báo cũ đi nếu có người khác bid đè lên
                        // =================================================================
                        if (thongbao != null) {
                            thongbao.setText("");
                        }
                        break;

                    case "SUCCESS":
                        if (message.contains("Lấy danh sách Bid thành công") && jsonResponse.has("payload") && jsonResponse.get("payload").isJsonArray()) {
                            JsonArray bidArray = jsonResponse.getAsJsonArray("payload");
                            listBids.clear();

                            for (JsonElement element : bidArray) {
                                JsonObject bidObj = element.getAsJsonObject();
                                String time = parseServerTime(bidObj.get("bidTime"));
                                String user = bidObj.has("accountName") ? bidObj.get("accountName").getAsString() : (bidObj.has("bidderAccount") ? bidObj.get("bidderAccount").getAsString() : "Unknown");
                                double price = bidObj.has("amount") ? bidObj.get("amount").getAsDouble() : (bidObj.has("bidAmount") ? bidObj.get("bidAmount").getAsDouble() : 0.0);

                                listBids.add(new BidEntry(time, user, price));
                            }

                            listBids.sort((b1, b2) -> b2.getTime().compareTo(b1.getTime()));
                        }
                        else if (message.contains("Bạn đang dẫn đầu")) {
                            setThongBao("🎉 " + message, true);
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
                            String winnerAccount = payloadObj.has("winner") ? payloadObj.get("winner").getAsString() : "Không có";
                            String winnerNickname = payloadObj.has("winnerNickname") ? payloadObj.get("winnerNickname").getAsString() : winnerAccount;
                            double finalPrice = payloadObj.has("finalPrice") ? payloadObj.get("finalPrice").getAsDouble() : currentSession.getCurrentPrice();

                            currentSession.setHighestBidderAccount(winnerNickname);
                            currentSession.setCurrentPrice(finalPrice);
                            updateUI();

                            com.tboat.models.User currentUser = com.tboat.utilsclient.UserSession.getInstance().getUser();
                            String myAccount = com.tboat.utilsclient.UserSession.getInstance().getUsername();

                            if (currentUser != null) {
                                // Trường hợp 1: Nếu mình là người thắng cuộc -> Trừ tiền trong Session
                                if (myAccount.equalsIgnoreCase(winnerAccount)) {
                                    double newBalance = currentUser.getBalance() - finalPrice;
                                    currentUser.setBalance(newBalance);
                                    log.info("Bạn đã thắng! Đã cập nhật số dư Session (Trừ tiền): " + newBalance);
                                }
                                // Trường hợp 2: Nếu mình là người bán sản phẩm này -> Cộng tiền vào Session
                                else if (myAccount.equalsIgnoreCase(currentSession.getSellerAccountName())) {
                                    double moneyReceived = finalPrice * 0.9;
                                    double newBalance = currentUser.getBalance() + moneyReceived;
                                    currentUser.setBalance(newBalance);
                                    log.info("Sản phẩm của bạn đã bán! Đã cập nhật số dư Session (Cộng tiền): " + newBalance);
                                }
                            }
                            // =========================================================
                        }

                        setupAuctionState();
                        HighestBidder.setText("🏆 KẾT THÚC | Người chiến thắng: " + currentSession.getHighestBidderAccount());
                        showAlert(Alert.AlertType.INFORMATION, "Kết thúc", message);
                        break;

                    case "FAILED":
                        if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull() && jsonResponse.get("payload").isJsonPrimitive()) {
                            try {
                                double actualServerPrice = jsonResponse.get("payload").getAsDouble();
                                currentSession.setCurrentPrice(actualServerPrice);
                                updateUI();
                            } catch (Exception ignored) {}
                        }
                        setThongBao("❌ " + message, false);
                        break;

                    case "ERROR":
                        setThongBao("⚠️ " + message, false);
                        break;

                    default:
                        log.warn("AuctionController nhận được status không xử lý được: {}", status);
                        break;
                }
            } catch (Exception e) {
                System.out.println("❌ KHÔNG THỂ ĐỌC DỮ LIỆU TỪ SERVER: " + response);
                log.error("Không thể đọc dữ liệu từ server: {} | Exception: {}", response, e.getMessage(), e);
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