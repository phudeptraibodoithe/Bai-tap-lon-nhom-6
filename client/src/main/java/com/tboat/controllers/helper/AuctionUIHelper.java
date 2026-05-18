package com.tboat.controllers.helper;

import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.auction.BidEntry;
import com.tboat.utilsclient.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.time.format.DateTimeFormatter;

/**
 * Tách toàn bộ logic cập nhật UI và timer khỏi AuctionController.
 * AuctionController chỉ cần gọi các method ở đây.
 */
public class AuctionUIHelper {

    private static final String STYLE_SUCCESS = "-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 17px;";
    private static final String STYLE_WARNING = "-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 17px;";
    private static final String STYLE_ENDED   = "-fx-text-fill: red;    -fx-font-weight: bold; -fx-font-size: 17px;";

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── FXML refs (được inject từ controller) ─────────────────────────────────
    private final ImageView itemImage;
    private final Label timeRemaining, nameItem, idItem, sellerName;
    private final Label description, currentPrice, highestBidder, lblNotification;
    private final TextField bidAmount;
    private final Button btnBid;
    private final TableView<BidEntry> tableBidHistory;
    private final TableColumn<BidEntry, String> colBidTime, colBidUser;
    private final TableColumn<BidEntry, Double> colBidPrice;
    private final XYChart.Series<String, Number> priceSeries;
    private final ObservableList<BidEntry> listBids;

    // ── State ─────────────────────────────────────────────────────────────────
    private AuctionTimer auctionTimer;

    public AuctionUIHelper(
            ImageView itemImage, Label timeRemaining, Label nameItem, Label idItem,
            Label sellerName, Label description, Label currentPrice,
            Label highestBidder, Label lblNotification, TextField bidAmount, Button btnBid,
            TableView<BidEntry> tableBidHistory, TableColumn<BidEntry, String> colBidTime,
            TableColumn<BidEntry, String> colBidUser, TableColumn<BidEntry, Double> colBidPrice,
            LineChart<String, Number> bidLineChart, ObservableList<BidEntry> listBids
    ) {
        this.itemImage       = itemImage;
        this.timeRemaining   = timeRemaining;
        this.nameItem        = nameItem;
        this.idItem          = idItem;
        this.sellerName      = sellerName;
        this.description     = description;
        this.currentPrice    = currentPrice;
        this.highestBidder   = highestBidder;
        this.lblNotification = lblNotification;
        this.bidAmount       = bidAmount;
        this.btnBid          = btnBid;
        this.tableBidHistory = tableBidHistory;
        this.colBidTime      = colBidTime;
        this.colBidUser      = colBidUser;
        this.colBidPrice     = colBidPrice;
        this.listBids        = listBids;

        this.priceSeries = new XYChart.Series<>();
        this.priceSeries.setName("Diễn biến giá (VNĐ)");
        bidLineChart.getData().add(priceSeries);
        bidLineChart.setAnimated(true);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    public void setupBidHistoryTable() {
        if (colBidTime == null) return;

        colBidTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTime()));
        colBidUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUser()));
        colBidPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrice()));
        colBidPrice.setCellFactory(col -> new TableCell<>() {
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

    // ── Cập nhật UI ──────────────────────────────────────────────────────────

    public void updateUI(AuctionSession session) {
        nameItem.setText(session.getName());
        idItem.setText("ID : " + session.getId());

        if (sellerName != null)
            sellerName.setText("Người bán: " + session.getSellerAccountName());

        description.setText(session.getDescription() != null
                ? "Mô tả: " + session.getDescription() : "Mô tả: Không có.");

        currentPrice.setText(CurrencyFormatter.formatDisplay(session.getCurrentPrice()));
        updateHighestBidderLabel(session);
        loadItemImage(session);
    }

    private void loadItemImage(AuctionSession session) {
        if (itemImage == null) return;
        String url = session.getImageURL();
        if (url != null && !url.isEmpty()) {
            Image img = ImageUtils.base64ToImage(url);
            if (img != null) itemImage.setImage(img);
        }
    }

    public void updateHighestBidderLabel(AuctionSession session) {
        String status = session.getStatusOfAuction() != null
                ? session.getStatusOfAuction().name() : "ONGOING";
        StringBuilder sb = new StringBuilder("🏆 Trạng thái: ").append(status);

        String top = session.getHighestBidderAccount();
        if (top != null && !top.isEmpty() && !top.equals("N/A"))
            sb.append(" | Đang dẫn đầu: ").append(top);

        highestBidder.setText(sb.toString());
    }

    public void setAuctionEndedLabel(AuctionSession session) {
        highestBidder.setText("🏆 KẾT THÚC | Người chiến thắng: "
                + session.getHighestBidderAccount());
    }

    // ── Input state ──────────────────────────────────────────────────────────

    public void setupInputState(StatusOfAuction status, boolean isUserSeller) {
        boolean disable = isUserSeller
                || status == StatusOfAuction.ENDED
                || status == StatusOfAuction.CANCELED
                || status == StatusOfAuction.NOT_STARTED;

        btnBid.setDisable(disable);
        bidAmount.setDisable(disable);

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

    // ── Timer ────────────────────────────────────────────────────────────────

    public void setupTimerState(StatusOfAuction status, AuctionSession session) {
        stopTimer();
        switch (status) {
            case ENDED, CANCELED -> {
                timeRemaining.setText("00 : 00 : 00");
                timeRemaining.setStyle(STYLE_ENDED);
            }
            case NOT_STARTED -> {
                String startStr = session.getStartTime() != null
                        ? session.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM"))
                        : "Sắp diễn ra...";
                timeRemaining.setText("Bắt đầu: " + startStr);
                timeRemaining.setStyle(STYLE_WARNING);
            }
            case ONGOING -> {
                timeRemaining.setStyle(STYLE_SUCCESS);
                startTimer(session);
            }
        }
    }

    private void startTimer(AuctionSession session) {
        if (session.getEndTime() == null) return;

        auctionTimer = new AuctionTimer(
                session.getEndTime(),
                timeStr -> Platform.runLater(() -> {
                    if (timeRemaining != null) timeRemaining.setText(timeStr);
                }),
                () -> Platform.runLater(() -> {
                    if (timeRemaining != null) {
                        timeRemaining.setText("00 : 00 : 00");
                        timeRemaining.setStyle(STYLE_ENDED);
                    }
                    if (btnBid   != null) btnBid.setDisable(true);
                    if (bidAmount != null) {
                        bidAmount.setDisable(true);
                        bidAmount.setPromptText("Đã kết thúc...");
                    }
                    session.setStatusOfAuction(StatusOfAuction.ENDED);
                })
        );
        auctionTimer.start();
    }

    public void stopTimer() {
        if (auctionTimer != null) auctionTimer.stop();
    }

    // ── Bids & Chart ─────────────────────────────────────────────────────────

    public void refreshBidsAndChart() {
        if (listBids.isEmpty()) return;

        // Bảng: mới nhất lên đầu
        listBids.sort((a, b) -> b.getTime().compareTo(a.getTime()));

        // Chart: cũ → mới
        Platform.runLater(() -> {
            priceSeries.getData().clear();
            ObservableList<BidEntry> chrono = FXCollections.observableArrayList(listBids);
            chrono.sort((a, b) -> a.getTime().compareTo(b.getTime()));
            for (BidEntry bid : chrono) {
                String label = bid.getTime().length() > 11
                        ? bid.getTime().substring(11) : bid.getTime();
                priceSeries.getData().add(new XYChart.Data<>(label, bid.getPrice()));
            }
        });
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public ObservableList<BidEntry> getListBids()  { return listBids; }
    public Label getLblNotification()              { return lblNotification; }
}