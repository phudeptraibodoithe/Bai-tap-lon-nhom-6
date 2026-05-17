package com.tboat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.AuctionSession;
import com.tboat.models.ItemFactory;
import com.tboat.models.ItemFactoryProducer;
import com.tboat.models.StatusOfAuction;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.HeaderUtils;
import com.tboat.utilsclient.ImageUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class TrangChuController extends BaseController implements Initializable, SocketListener {

    @FXML private FlowPane itemContainer;
    @FXML private Button btnHome, btnHistory, btnPostItem, btnHistory1;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    // Các nút Bộ Lọc
    @FXML private Button btnFilterAll, btnFilterDienTu, btnFilterThoiTrang, btnFilterTrangSuc, btnFilterKhac;

    private final List<VBox> allCards = new ArrayList<>();
    private String currentFilter = "Tất cả"; // Bộ lọc mặc định

    private static final Logger logger = Logger.getLogger(TrangChuController.class.getName());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);

        // ── UCB: Cache-then-Network ──────────────────────────────────────────
        String screenKey = BaseController.toScreenKey("TrangChu.fxml"); // "TrangChuFxml"
        String cached    = DataCache.getInstance().get("LIST_AVAILABLE");

        if (cached != null) {
            log.info("[TrangChu] Cache HIT → render ngay");
            NavigationContext.getInstance().reportCacheHit(screenKey, true);
            handleServerResponse(cached);   // render từ cache (gọi lại chính hàm xử lý)
            loadAuctions();                 // refresh ngầm
        } else {
            log.info("[TrangChu] Cache MISS → fetch server");
            NavigationContext.getInstance().reportCacheHit(screenKey, false);
            loadAuctions();
        }
        // ────────────────────────────────────────────────────────────────────
    }

    @Override
    public void onReload() {
        loadAuctions();
        log.info("Đã tải lại danh sách sản phẩm trang chủ!");
    }

    @Override
    public void onReload() {
        loadAuctions();
        System.out.println("Đã tải lại danh sách sản phẩm trang chủ!");
    }

    public void loadAuctions() {
        if (itemContainer != null) itemContainer.getChildren().clear();
        // 👉 Rút gọn siêu cấp: Gửi request thông qua SocketHelper
        SocketHelper.sendRequest("LIST_AVAILABLE", "");
    }

    // =======================================================
    // CÁC HÀM XỬ LÝ BỘ LỌC KHI BẤM NÚT
    // =======================================================
    @FXML public void filterAll(ActionEvent event) { setFilter("Tất cả"); }
    @FXML public void filterDienTu(ActionEvent event) { setFilter("Điện tử"); }
    @FXML public void filterThoiTrang(ActionEvent event) { setFilter("Thời trang"); }
    @FXML public void filterTrangSuc(ActionEvent event) { setFilter("Trang sức"); }
    @FXML public void filterKhac(ActionEvent event) { setFilter("Khác"); }

    private void setFilter(String category) {
        this.currentFilter = category;
        applyFilter();
    }

    private void applyFilter() {
        if (itemContainer == null) return;
        itemContainer.getChildren().clear();

        for (VBox card : allCards) {
            String cardType = (String) card.getUserData();
            if ("Tất cả".equals(currentFilter) || currentFilter.equals(cardType)) {
                itemContainer.getChildren().add(card);
            }
        }
        updateButtonStyles();
    }

    private void updateButtonStyles() {
        Button[] buttons = {btnFilterAll, btnFilterDienTu, btnFilterThoiTrang, btnFilterTrangSuc, btnFilterKhac};
        String[] filters = {"Tất cả", "Điện tử", "Thời trang", "Trang sức", "Khác"};

        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                if (filters[i].equals(currentFilter)) {
                    buttons[i].setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 8 20; -fx-cursor: hand; -fx-text-fill: #0A1128;");
                } else {
                    buttons[i].setStyle("-fx-background-color: transparent; -fx-padding: 8 20; -fx-cursor: hand; -fx-text-fill: #666666;");
                }
            }
        }
    }

    // =======================================================
    // XỬ LÝ DỮ LIỆU TỪ SERVER TRẢ VỀ
    // =======================================================
    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                String status = SocketHelper.getStatus(response);

                if ("SUCCESS".equals(status)) {
                    JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                    if (jsonResponse.has("payload") && jsonResponse.get("payload").isJsonArray()) {
                        JsonArray auctionArray = jsonResponse.getAsJsonArray("payload");

                        allCards.clear();
                        if (itemContainer != null) itemContainer.getChildren().clear();

                        for (JsonElement element : auctionArray) {
                            JsonObject dataObj = element.getAsJsonObject();

                            int id = dataObj.has("id") ? dataObj.get("id").getAsInt() : 0;
                            String type = dataObj.has("type") ? dataObj.get("type").getAsString() : "Khác";
                            String name = dataObj.has("name") ? dataObj.get("name").getAsString() : "Sản phẩm chưa có tên";
                            double currentPrice = dataObj.has("currentPrice") ? dataObj.get("currentPrice").getAsDouble() : 0.0;
                            double bidIncrease = dataObj.has("bidIncrease") ? dataObj.get("bidIncrease").getAsDouble() : 0.0;
                            String description = dataObj.has("description") ? dataObj.get("description").getAsString() : "Không có mô tả";
                            String highestBidder = dataObj.has("highestBidderAccount") && !dataObj.get("highestBidderAccount").isJsonNull() ? dataObj.get("highestBidderAccount").getAsString() : "";
                            String sellerAccount = dataObj.has("sellerAccountName") && !dataObj.get("sellerAccountName").isJsonNull() ? dataObj.get("sellerAccountName").getAsString() : "N/A";
                            String imageBase64 = dataObj.has("imageURL") ? dataObj.get("imageURL").getAsString() : "";

                            // 👉 Đã tối ưu: Tận dụng TimeUtils để lấy chuẩn LocalDateTime, không cần cắt chuỗi lặp 2 lần
                            LocalDateTime startTime = dataObj.has("startTime") ? TimeUtils.parseServerTime(dataObj.get("startTime")) : LocalDateTime.now();
                            LocalDateTime endTime = dataObj.has("endTime") ? TimeUtils.parseServerTime(dataObj.get("endTime")) : LocalDateTime.now().plusDays(1);

                            if (imageBase64.startsWith("data:image")) {
                                imageBase64 = imageBase64.substring(imageBase64.indexOf(",") + 1);
                            }

                            ItemFactory factory = ItemFactoryProducer.getFactory(type);
                            AuctionSession session = new AuctionSession(
                                    startTime, endTime, currentPrice, bidIncrease,
                                    factory.createItem()
                            );
                            session.setId(id);
                            session.setHighestBidderAccount(highestBidder);

                            String statusString = dataObj.has("statusOfAuction") ? dataObj.get("statusOfAuction").getAsString() : "ONGOING";
                            try {
                                session.setStatusOfAuction(StatusOfAuction.valueOf(statusString));
                            } catch (IllegalArgumentException e) {
                                session.setStatusOfAuction(StatusOfAuction.ONGOING);
                            }

                            VBox productCard = createProductCard(session);
                            productCard.setUserData(type);
                            allCards.add(productCard);
                        }

                        applyFilter();
                    }

                    // Lọc và hiển thị ra màn hình theo Filter đang chọn (mặc định là Tất cả)
                    applyFilter();
                }
            } catch (Exception e) {
                if (response.contains("{")) {
                    logger.severe("LỖI ĐỌC JSON TRANG CHỦ: " + response);
                }
            }
        });
    }

    // =======================================================
    // TẠO GIAO DIỆN (UI) THẺ SẢN PHẨM
    // =======================================================
    private VBox createProductCard(AuctionSession session) {
        VBox card = new VBox();
        card.setPrefWidth(300.0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        card.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.08)));

        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(240.0);
        imagePane.setStyle("-fx-background-color: #E8E8E8; -fx-background-radius: 20 20 0 0;");

        String base64Data = session.getImageURL();
        if (base64Data != null && !base64Data.isEmpty()) {
            Image img = ImageUtils.base64ToImage(base64Data);
            if (img != null) {
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(300.0);
                imageView.setFitHeight(240.0);

                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 240);
                clip.setArcWidth(40);
                clip.setArcHeight(40);
                imageView.setClip(clip);

                imagePane.getChildren().add(imageView);
            }
        }

        Button btnHeart = new Button("🤍");
        btnHeart.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-background-radius: 50; -fx-min-width: 35; -fx-min-height: 35; -fx-cursor: hand;");
        btnHeart.setTextFill(Color.WHITE);
        btnHeart.setFont(Font.font(14));
        StackPane.setAlignment(btnHeart, Pos.TOP_RIGHT);
        StackPane.setMargin(btnHeart, new Insets(15, 15, 0, 0));
        imagePane.getChildren().add(btnHeart);

        VBox infoBox = new VBox(15.0);
        infoBox.setPadding(new Insets(20, 25, 25, 25));

        Label lblName = new Label(session.getName());
        lblName.setFont(Font.font("System", FontWeight.BOLD, 20));
        lblName.setStyle("-fx-text-fill: #0A1128;");
        lblName.setWrapText(true);
        lblName.setPrefHeight(55.0);

        HBox priceTimeBox = new HBox(10.0);

        VBox priceCol = createInfoCol("CURRENT BID", CurrencyFormatter.formatDisplay(session.getCurrentPrice()), "#0A1128");

        String statusText = session.getStatusOfAuction() != null ? session.getStatusOfAuction().name() : "ONGOING";
        VBox timeCol = createInfoCol("STATUS", statusText, "#FF6B00");

        HBox.setHgrow(priceCol, Priority.ALWAYS);
        HBox.setHgrow(timeCol, Priority.ALWAYS);
        priceTimeBox.getChildren().addAll(priceCol, timeCol);

        Button btnBid = new Button("PLACE BID");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setPrefHeight(45.0);
        btnBid.setStyle("-fx-background-color: #08103F; -fx-background-radius: 10; -fx-cursor: hand;");
        btnBid.setTextFill(Color.WHITE);
        btnBid.setFont(Font.font("System", FontWeight.BOLD, 14));

        btnBid.setOnAction(event -> {
            try {
                AuctionController controller = changeSceneAndGetController(btnBid, "auction.fxml");
                if (controller != null) {
                    controller.setItemData(session);
                }
            } catch (Exception e) {
                logger.severe("Lỗi truyền dữ liệu vào Auction");
            }
        });

        infoBox.getChildren().addAll(lblName, priceTimeBox, btnBid);
        card.getChildren().addAll(imagePane, infoBox);

        return card;
    }

    private VBox createInfoCol(String title, String value, String colorHex) {
        VBox col = new VBox(3.0);
        col.setPrefHeight(65.0);
        col.setAlignment(Pos.CENTER_LEFT);
        col.setStyle("-fx-background-color: #F4F5F7; -fx-background-radius: 12; -fx-padding: 10 15;");

        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 10));
        lblTitle.setStyle("-fx-text-fill: #9DA3B4;");

        Label lblValue = new Label(value);
        lblValue.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblValue.setStyle("-fx-text-fill: " + colorHex + ";");

        col.getChildren().addAll(lblTitle, lblValue);
        return col;
    }
}