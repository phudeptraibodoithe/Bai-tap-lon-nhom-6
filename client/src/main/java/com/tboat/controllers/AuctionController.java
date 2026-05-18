package com.tboat.controllers;

import com.google.gson.*;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;
import com.tboat.socket.SocketListener;
import com.tboat.utilsclient.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class AuctionController extends BaseController implements SocketListener {

    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

    private static final String STYLE_SUCCESS = "-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 17px;";
    private static final String STYLE_ERROR   = "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 17px;";

    // ── FXML ─────────────────────────────────────────────────────────────────

    @FXML private ImageView itemImage;
    @FXML private Label timeRemaining, nameItem, idItem, sellerName;
    @FXML private Label description, currentPrice, highestBidder, lblNotification;
    @FXML private TextField bidAmount;
    @FXML private Button btnBid;
    @FXML private TableView<BidEntry> tableBidHistory;
    @FXML private TableColumn<BidEntry, String> colBidTime, colBidUser;
    @FXML private TableColumn<BidEntry, Double> colBidPrice;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;
    @FXML private LineChart<String, Number> bidLineChart;

    // ── State ─────────────────────────────────────────────────────────────────

    private AuctionSession currentSession;
    private AuctionUIHelper ui;
    private boolean isUserSeller = false;

    // ── Khởi tạo ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        ObservableList<BidEntry> listBids = FXCollections.observableArrayList();

        ui = new AuctionUIHelper(
                itemImage, timeRemaining, nameItem, idItem,
                sellerName, description, currentPrice, highestBidder, lblNotification,
                bidAmount, btnBid,
                tableBidHistory, colBidTime, colBidUser, colBidPrice,
                bidLineChart, listBids
        );

        ui.setupBidHistoryTable();
        if (lblNotification != null) lblNotification.setText("");
        CurrencyFormatter.attachCurrencyListener(bidAmount);
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
    }

    // ── Entry point từ màn hình trước ────────────────────────────────────────

    public void setItemData(AuctionSession session) {
        if (session == null) return;
        this.currentSession = session;
        ui.updateUI(session);
        SocketHelper.sendRequest("JOIN", session.getId());
        SocketHelper.sendRequest("GET_SESSION_BIDS", session.getId());
        refreshAuctionState();
    }

    @Override
    public void onReload() {
        if (currentSession != null) {
            SocketHelper.sendRequest("GET_SESSION_BIDS", currentSession.getId());
            log.info("Đã tải lại lịch sử giá!");
        }
    }

    private void refreshAuctionState() {
        if (btnBid == null || bidAmount == null || timeRemaining == null) return;
        StatusOfAuction status = currentSession.getStatusOfAuction();
        ui.setupInputState(status, isUserSeller);
        ui.setupTimerState(status, currentSession);
    }

    // ── Hành động người dùng ─────────────────────────────────────────────────

    @FXML
    public void placeBid(ActionEvent event) {
        String input = bidAmount.getText();
        if (input == null || input.trim().isEmpty()) {
            AlertUtils.showStatus(lblNotification, "Vui lòng nhập mức giá!", STYLE_ERROR);
            return;
        }
        try {
            double bidValue = CurrencyFormatter.parse(input);
            double minNext  = currentSession.getCurrentPrice() + currentSession.getBidIncrease();

            if (bidValue < minNext) {
                AlertUtils.showStatus(lblNotification,
                        "Giá tối thiểu: " + CurrencyFormatter.formatDisplay(minNext), STYLE_ERROR);
                return;
            }

            AlertUtils.showStatus(lblNotification, "Đang gửi lệnh đặt giá...",
                    "-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            SocketHelper.sendRequest("BID", bidValue);
            bidAmount.clear();

        } catch (Exception e) {
            AlertUtils.showStatus(lblNotification, "Định dạng số không hợp lệ.", STYLE_ERROR);
        }
    }

    // ── Socket handler ────────────────────────────────────────────────────────

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                String type   = SocketHelper.getType(response);
                String status = SocketHelper.getStatus(response);
                String msg    = SocketHelper.getMessage(response);

                boolean isBroadcast  = "NEW_BID".equals(status) || "AUCTION_STARTED".equals(status)
                        || "AUCTION_FINISHED".equals(status) || "TIME_EXTENDED".equals(status)
                        || "SERVER_READY".equals(status);
                boolean isMyResponse = "JOIN".equals(type) || "BID".equals(type)
                        || "GET_SESSION_BIDS".equals(type) || "CANCEL_AUCTION".equals(type);

                if (!isBroadcast && !isMyResponse) return;

                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                switch (status) {
                    case "AUCTION_STARTED"  -> handleAuctionStarted(msg);
                    case "NEW_BID"          -> handleNewBid(json.getAsJsonObject("payload"));
                    case "SUCCESS"          -> handleSuccess(json, msg);
                    case "JOIN_SUCCESS"     -> handleJoinSuccess(json);
                    case "TIME_EXTENDED"    -> AlertUtils.showAlert(Alert.AlertType.WARNING, "Đấu giá kịch tính!", msg);
                    case "AUCTION_FINISHED" -> handleAuctionFinished(json, msg);
                    case "FAILED"           -> handleFailed(json, msg);
                    case "ERROR"            -> AlertUtils.showStatus(lblNotification, "⚠️ " + msg, STYLE_ERROR);
                    case "SERVER_READY"     -> {}
                    default -> log.warn("Không xử lý được status: {}", status);
                }
            } catch (Exception e) {
                log.error("Lỗi đọc dữ liệu từ server: {} | {}", response, e.getMessage());
            }
        });
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void handleNewBid(JsonObject payload) {
        double newPrice  = payload.get("newPrice").getAsDouble();
        String newLeader = payload.get("newLeader").getAsString();
        LocalDateTime dt = payload.has("bidTime")
                ? TimeUtils.parseServerTime(payload.get("bidTime")) : LocalDateTime.now();

        currentSession.setCurrentPrice(newPrice);
        currentSession.setHighestBidderAccount(newLeader);
        ui.updateUI(currentSession);
        ui.getListBids().add(new BidEntry(dt.format(AuctionUIHelper.TIME_FORMATTER), newLeader, newPrice));
        ui.refreshBidsAndChart();
        if (lblNotification != null) lblNotification.setText("");
    }

    private void handleSuccess(JsonObject json, String msg) {
        if (msg.contains("Lấy danh sách Bid thành công") && json.has("payload")) {
            ui.getListBids().clear();
            for (JsonElement el : json.getAsJsonArray("payload")) {
                JsonObject bid = el.getAsJsonObject();
                LocalDateTime dt = TimeUtils.parseServerTime(bid.get("bidTime"));
                String time  = dt != null ? dt.format(AuctionUIHelper.TIME_FORMATTER)
                        : LocalDateTime.now().format(AuctionUIHelper.TIME_FORMATTER);
                String user  = bid.has("bidderAccount") ? bid.get("bidderAccount").getAsString() : "Unknown";
                double price = bid.has("bidAmount")     ? bid.get("bidAmount").getAsDouble()     : 0.0;
                ui.getListBids().add(new BidEntry(time, user, price));
            }
            ui.refreshBidsAndChart();
        } else if (msg.contains("Bạn đang dẫn đầu")) {
            AlertUtils.showStatus(lblNotification, "🎉 " + msg, STYLE_SUCCESS);
        }
    }

    private void handleJoinSuccess(JsonObject json) {
        if (!json.has("payload") || json.get("payload").isJsonNull()) return;
        JsonObject payload = json.getAsJsonObject("payload");
        currentSession.setCurrentPrice(payload.get("currentPrice").getAsDouble());
        this.isUserSeller = payload.has("isSeller") && payload.get("isSeller").getAsBoolean();
        refreshAuctionState();
        ui.updateUI(currentSession);
    }

    private void handleAuctionStarted(String msg) {
        currentSession.setStatusOfAuction(StatusOfAuction.ONGOING);
        refreshAuctionState();
        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Đã đến giờ", msg);
    }

    private void handleAuctionFinished(JsonObject json, String msg) {
        currentSession.setStatusOfAuction(StatusOfAuction.ENDED);

        if (json.has("payload") && !json.get("payload").isJsonNull()) {
            JsonObject p      = json.getAsJsonObject("payload");
            String winner     = p.has("winner")         ? p.get("winner").getAsString()         : "Không có";
            String winnerNick = p.has("winnerNickname") ? p.get("winnerNickname").getAsString() : winner;
            double finalPrice = p.has("finalPrice")     ? p.get("finalPrice").getAsDouble()     : currentSession.getCurrentPrice();

            currentSession.setHighestBidderAccount(winnerNick);
            currentSession.setCurrentPrice(finalPrice);
            ui.updateUI(currentSession);
            updateUserBalance(winner, finalPrice);
        }

        refreshAuctionState();
        ui.setAuctionEndedLabel(currentSession);
        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Kết thúc", msg);
    }

    private void handleFailed(JsonObject json, String msg) {
        if (json.has("payload") && json.get("payload").isJsonPrimitive()) {
            try {
                currentSession.setCurrentPrice(json.get("payload").getAsDouble());
                ui.updateUI(currentSession);
            } catch (Exception ignored) {}
        }
        AlertUtils.showStatus(lblNotification, "❌ " + msg, STYLE_ERROR);
    }

    private void updateUserBalance(String winnerAccount, double finalPrice) {
        User me = UserSession.getInstance().getUser();
        if (me == null) return;
        String myAccount = UserSession.getInstance().getUsername();
        if (myAccount.equalsIgnoreCase(winnerAccount)) {
            me.setBalance(me.getBalance() - finalPrice);
        } else if (myAccount.equalsIgnoreCase(currentSession.getSellerAccountName())) {
            me.setBalance(me.getBalance() + finalPrice * 0.9);
        }
    }
}