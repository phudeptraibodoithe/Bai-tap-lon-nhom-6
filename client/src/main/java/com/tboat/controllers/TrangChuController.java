package com.tboat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.AuctionSession;
import com.tboat.models.Item;
import com.tboat.models.ItemFactory;
import com.tboat.models.ItemFactoryProducer;
import com.tboat.models.StatusOfAuction;
import com.tboat.socket.SocketListener;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
import com.tboat.utilsclient.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

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
        String cached = DataCache.getInstance().get("LIST_AVAILABLE");

        if (cached != null) {
            logger.info("[TrangChu] Cache HIT → render ngay");
            NavigationContext.getInstance().reportCacheHit(screenKey, true);
            handleServerResponse(cached);
            loadAuctions();
        } else {
            logger.info("[TrangChu] Cache MISS → fetch server");
            NavigationContext.getInstance().reportCacheHit(screenKey, false);
            loadAuctions();
        }
    }

    @Override
    public void onReload() {
        loadAuctions();
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
                    if (!"LIST_AVAILABLE".equals(SocketHelper.getType(response))) return;

                    JsonArray auctionArray = SocketHelper.getPayloadArray(response);
                    if (auctionArray == null) return;

                    allCards.clear();
                    if (itemContainer != null) itemContainer.getChildren().clear();

                    for (JsonElement element : auctionArray) {
                        try {
                            JsonObject dataObj = element.getAsJsonObject();
                            AuctionSession session = parseSingleAuctionSession(dataObj);

                            // Lấy type để filter
                            String type = getStringJson(dataObj.has("item") ? dataObj.getAsJsonObject("item") : dataObj, "type", "Khác");

                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/productCard.fxml"));
                            VBox productCard = loader.load();

                            ProductCardController cardController = loader.getController();
                            cardController.setData(session);

                            productCard.setUserData(type);
                            allCards.add(productCard);

                        } catch (Exception ex) {
                            logger.severe("[TrangChu] Lỗi nạp FXML hoặc xử lý thẻ sản phẩm: " + ex.getMessage());
                        }
                    }
                    applyFilter();
                }
            } catch (Exception e) {
                logger.severe("LỖI ĐỌC JSON TRANG CHỦ: " + e.getMessage());
            }
        });
    }

    // =======================================================
    // HELPER METHODS (Dùng chung chuẩn giống ManagerController)
    // =======================================================

    private AuctionSession parseSingleAuctionSession(JsonObject dataObj) {
        // Hỗ trợ cả object lồng nhau (item) hoặc object phẳng
        JsonObject itemObj = dataObj.has("item") && dataObj.get("item").isJsonObject()
                ? dataObj.getAsJsonObject("item")
                : dataObj;

        String type          = getStringJson(itemObj, "type", "Khác");
        String name          = getStringJson(itemObj, "name", "Sản phẩm chưa có tên");
        String description   = getStringJson(itemObj, "description", "Không có mô tả");
        String imageURL      = getStringJson(itemObj, "imageURL", "");
        String sellerAccount = getStringJson(itemObj, "sellerAccountName", "N/A");
        String highestBidder = getStringJson(dataObj, "highestBidderAccount", "");

        double currentPrice  = getDoubleJson(dataObj, "currentPrice", 0.0);
        double bidIncrease   = getDoubleJson(dataObj, "bidIncrease", 0.0);

        LocalDateTime startTime = dataObj.has("startTime") ? TimeUtils.parseServerTime(dataObj.get("startTime")) : LocalDateTime.now();
        LocalDateTime endTime   = dataObj.has("endTime")   ? TimeUtils.parseServerTime(dataObj.get("endTime"))   : LocalDateTime.now().plusDays(1);

        if (imageURL.startsWith("data:image")) {
            imageURL = imageURL.substring(imageURL.indexOf(",") + 1);
        }

        ItemFactory factory = ItemFactoryProducer.getFactory(type);
        Item item = factory.createItem(sellerAccount, name, description, imageURL);

        AuctionSession session = new AuctionSession(startTime, endTime, currentPrice, bidIncrease, item);
        session.setId(dataObj.has("id") ? dataObj.get("id").getAsInt() : 0);
        session.setHighestBidderAccount(highestBidder);

        String statusStr = getStringJson(dataObj, "statusOfAuction", "ONGOING");
        try {
            session.setStatusOfAuction(StatusOfAuction.valueOf(statusStr));
        } catch (Exception ignored) {
            session.setStatusOfAuction(StatusOfAuction.ONGOING);
        }

        return session;
    }

    private String getStringJson(JsonObject json, String key, String defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : defaultValue;
    }

    private double getDoubleJson(JsonObject json, String key, double defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : defaultValue;
    }
}