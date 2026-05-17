package com.tboat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.AuctionSession;
import com.tboat.models.ItemFactory;
import com.tboat.models.ItemFactoryProducer;
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
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/productCard.fxml"));
                            VBox productCard = loader.load();

                            ProductCardController cardController = loader.getController();
                            cardController.setData(session);

                            productCard.setUserData(type);
                            allCards.add(productCard);
                        } catch (Exception ex) {
                            logger.severe("Lỗi nạp FXML thẻ sản phẩm: " + ex.getMessage());
                        }
                        applyFilter();
                    }
                }
            } catch (Exception e) {
                // Đã cấu trúc lại khối Try-Catch và đóng ngoặc đúng cách
                logger.severe("LỖI ĐỌC JSON TRANG CHỦ: " + e.getMessage());
            }
        });
    }
}