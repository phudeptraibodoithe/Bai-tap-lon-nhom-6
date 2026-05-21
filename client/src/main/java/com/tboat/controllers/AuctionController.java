package com.tboat.controllers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.controllers.helper.AuctionUIHelper;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.auction.BidEntry;
import com.tboat.models.core.User;
import com.tboat.models.network.ServerEvent;
import com.tboat.session.UserSession;
import com.tboat.socket.SocketHelper;
import com.tboat.socket.SocketListener;
import com.tboat.utilsclient.AlertUtils;
import com.tboat.utilsclient.CurrencyFormatter;
import com.tboat.utilsclient.HeaderUtils;
import com.tboat.utilsclient.TimeUtils;
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

    @FXML private ImageView itemImage;
    @FXML private Label timeRemaining, nameItem, idItem, sellerName;
    @FXML private Label description, currentPrice, buyNowPrice, highestBidder, lblNotification;
    @FXML private TextField bidAmount;
    @FXML private CheckBox autoBidCheckBox;
    @FXML private Button btnBid;
    @FXML private TableView<BidEntry> tableBidHistory;
    @FXML private TableColumn<BidEntry, String> colBidTime, colBidUser;
    @FXML private TableColumn<BidEntry, Double> colBidPrice;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;
    @FXML private LineChart<String, Number> bidLineChart;

    private AuctionSession currentSession;
    private AuctionUIHelper ui;
    private boolean isUserSeller = false;

    @FXML
    public void initialize() {
        ObservableList<BidEntry> listBids = FXCollections.observableArrayList();
        ui = new AuctionUIHelper(
                itemImage, timeRemaining, nameItem, idItem,
                sellerName, description, currentPrice, buyNowPrice, highestBidder, lblNotification,
                bidAmount, btnBid,
                tableBidHistory, colBidTime, colBidUser, colBidPrice,
                bidLineChart, listBids
        );
        ui.setupBidHistoryTable();
        if (lblNotification != null) lblNotification.setText("");
        CurrencyFormatter.attachCurrencyListener(bidAmount);
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
        if (autoBidCheckBox != null) {
            autoBidCheckBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (!isNowSelected && bidAmount != null) {
                    bidAmount.setOpacity(1.0);
                    bidAmount.clear();
                }
            });
        }
    }

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
        boolean disableAutoBid = isUserSeller
                || status == StatusOfAuction.ENDED
                || status == StatusOfAuction.CANCELED
                || status == StatusOfAuction.NOT_STARTED;
        if (autoBidCheckBox != null) {
            autoBidCheckBox.setDisable(disableAutoBid);
            if (disableAutoBid) autoBidCheckBox.setSelected(false);
        }
    }

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

            boolean isAutoBid = autoBidCheckBox != null && autoBidCheckBox.isSelected();
            if (isAutoBid) {
                JsonObject payload = new JsonObject();
                payload.addProperty("maxBid", bidValue);
                SocketHelper.sendRequest("REGISTER_AUTO_BID", payload);

                // Giữ checkbox + giá trị, chỉ làm mờ ô nhập
                if (bidAmount != null) bidAmount.setOpacity(0.85);
            } else {
                SocketHelper.sendRequest("BID", bidValue);
                bidAmount.clear();
            }

        } catch (Exception e) {
            AlertUtils.showStatus(lblNotification, "Định dạng số không hợp lệ.", STYLE_ERROR);
        }
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                ServerEvent type   = SocketHelper.getTypeEnum(response);   // ← parse thành enum
                ServerEvent status = SocketHelper.getStatusEnum(response);
                String msg         = SocketHelper.getMessage(response);
                JsonObject json    = JsonParser.parseString(response).getAsJsonObject();

                if (type == ServerEvent.GET_PROFILE || type == ServerEvent.UPDATE_PROFILE) return;

                if (type == ServerEvent.NEW_BID || status == ServerEvent.NEW_BID) {
                    if (json.has("payload") && json.get("payload").isJsonObject())
                        handleNewBid(json.getAsJsonObject("payload"));
                    return;
                }
                if (type == ServerEvent.AUCTION_STARTED || status == ServerEvent.AUCTION_STARTED) {
                    handleAuctionStarted(msg); return;
                }
                if (type == ServerEvent.AUCTION_FINISHED || status == ServerEvent.AUCTION_FINISHED) {
                    handleAuctionFinished(json, msg); return;
                }
                if (type == ServerEvent.TIME_EXTENDED || status == ServerEvent.TIME_EXTENDED) {
                    handleTimeExtended(json, msg); return;
                }
                if (type == ServerEvent.AUCTION_CANCELED || status == ServerEvent.AUCTION_CANCELED) {
                    handleAuctionCanceled(); return;
                }
                if (type == ServerEvent.AUTO_BID_OUT || status == ServerEvent.AUTO_BID_OUT) {
                    handleAutoBidOut(msg); return;
                }

                switch (status) {
                    case SUCCESS             -> handleSuccess(json, msg);
                    case AUTO_BID_REGISTERED -> AlertUtils.showStatus(
                            lblNotification, "✅ " + msg, STYLE_SUCCESS);
                    case JOIN_SUCCESS        -> handleJoinSuccess(json);
                    case FAILED              -> handleFailed(json, msg);
                    case ERROR               -> AlertUtils.showStatus(lblNotification, "⚠️ " + msg, STYLE_ERROR);
                    case SYSTEM, SERVER_READY -> {}
                    default -> log.warn("Không khớp handler: type={}, status={}", type, status);
                }
            } catch (Exception e) {
                log.error("Lỗi đọc dữ liệu từ server: {} | {}", response, e.getMessage());
            }
        });
    }

// ── Handlers (các method đã thay đổi / thêm mới) ─────────────────────────────

    /**
     * FIX: Reset countdown timer theo newEndTime server gửi về.
     * Trước đây chỉ show Alert, bộ đếm vẫn chạy theo endTime cũ.
     */
    private void handleTimeExtended(JsonObject json, String msg) {
        AlertUtils.showAlert(Alert.AlertType.WARNING, "Đấu giá kịch tính!", msg);
        try {
            if (json.has("payload") && json.get("payload").isJsonObject()) {
                String newEndStr = json.getAsJsonObject("payload").get("newEndTime").getAsString();
                LocalDateTime newEnd = LocalDateTime.parse(newEndStr);
                currentSession.setEndTime(newEnd);
                ui.updateEndTime(newEnd);   // reset AuctionTimer với endTime mới
            }
        } catch (Exception e) {
            log.warn("Không thể parse newEndTime từ TIME_EXTENDED: {}", e.getMessage());
        }
    }

    /**
     * FIX: Phân biệt rõ 2 case:
     *   - Có winner → hiển thị người thắng + trừ/cộng tiền
     *   - Không winner → clear highestBidder, hiển thị "Không có người thắng"
     */
    private void handleAuctionFinished(JsonObject json, String msg) {
        currentSession.setStatusOfAuction(StatusOfAuction.ENDED);

        boolean hasWinner = json.has("payload") && !json.get("payload").isJsonNull()
                && json.get("payload").isJsonObject();

        if (hasWinner) {
            JsonObject p      = json.getAsJsonObject("payload");
            String winner     = p.has("winner")         ? p.get("winner").getAsString()        : null;
            String winnerNick = p.has("winnerNickname")  ? p.get("winnerNickname").getAsString()
                    : (winner != null ? winner : null);
            double finalPrice = p.has("finalPrice")      ? p.get("finalPrice").getAsDouble()
                    : currentSession.getCurrentPrice();

            currentSession.setHighestBidderAccount(winnerNick);
            currentSession.setCurrentPrice(finalPrice);
            ui.updateUI(currentSession);
            if (winner != null) updateUserBalance(winner, finalPrice);
        } else {
            // FIX: không có ai bid → clear winner rõ ràng, tránh sót nickname cũ
            currentSession.setHighestBidderAccount(null);
            ui.updateUI(currentSession);
        }

        refreshAuctionState();
        ui.setAuctionEndedLabel(currentSession);   // hiển thị đúng theo null/non-null winner
        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Kết thúc", msg);
    }

    /** Tách ra method riêng cho gọn handleServerResponse */
    private void handleAuctionCanceled() {
        currentSession.setStatusOfAuction(StatusOfAuction.CANCELED);
        refreshAuctionState();
        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Thông báo",
                "Phiên đấu giá này đã bị hủy bởi người bán!");
    }


    // ── Handlers ─────────────────────────────────────────────────────────────

    private void handleNewBid(JsonObject payload) {
        double newPrice    = payload.get("newPrice").getAsDouble();
        String newLeader   = payload.get("newLeader").getAsString(); // account — cho logic
        // Dùng nickname để hiển thị, fallback về account nếu server chưa gửi
        String displayName = payload.has("newLeaderNickname")
                ? payload.get("newLeaderNickname").getAsString() : newLeader;
        LocalDateTime dt   = payload.has("bidTime")
                ? TimeUtils.parseServerTime(payload.get("bidTime")) : LocalDateTime.now();

        currentSession.setCurrentPrice(newPrice);
        currentSession.setHighestBidderAccount(displayName); // hiển thị nickname
        ui.updateUI(currentSession);

        // Thêm vào bảng (mới nhất lên đầu)
        BidEntry entry = new BidEntry(dt.format(AuctionUIHelper.TIME_FORMATTER), displayName, newPrice);
        ui.getListBids().add(0, entry);

        // Append vào chart — không clear, không giật
        ui.appendBidToChart(entry);

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
            ui.refreshBidsAndChart(); // redraw toàn bộ — chỉ gọi ở đây
        } else if (msg.contains("Bạn đang dẫn đầu")) {
            AlertUtils.showStatus(lblNotification, "🎉 " + msg, STYLE_SUCCESS);
        }
    }

    private void handleJoinSuccess(JsonObject json) {
        if (!json.has("payload") || json.get("payload").isJsonNull()) return;
        JsonObject payload = json.getAsJsonObject("payload");

        currentSession.setCurrentPrice(payload.get("currentPrice").getAsDouble());
        if (payload.has("buyNowPrice"))
            currentSession.setBuyNowPrice(payload.get("buyNowPrice").getAsDouble());
        this.isUserSeller = payload.has("isSeller") && payload.get("isSeller").getAsBoolean();
        if (payload.has("sellerNickname"))
            currentSession.setSellerAccountName(payload.get("sellerNickname").getAsString());
        if (payload.has("leaderNickname"))
            currentSession.setHighestBidderAccount(payload.get("leaderNickname").getAsString());

        refreshAuctionState();
        ui.updateUI(currentSession);
    }

    private void handleAuctionStarted(String msg) {
        currentSession.setStatusOfAuction(StatusOfAuction.ONGOING);
        refreshAuctionState();
        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Đã đến giờ", msg);
    }

    private void handleAutoBidOut(String msg) {
        // Bỏ tích checkbox
        if (autoBidCheckBox != null) {
            autoBidCheckBox.setSelected(false);
            autoBidCheckBox.setDisable(false);
        }
        // Xóa ô nhập và restore opacity
        if (bidAmount != null) {
            bidAmount.clear();
            bidAmount.setOpacity(1.0);
        }
        // Thông báo cho user
        AlertUtils.showStatus(lblNotification,
                "⚠️ " + msg, STYLE_ERROR);
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
