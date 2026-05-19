package com.tboat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tboat.models.auction.AuctionSession;
import com.tboat.socket.SocketHelper;
import com.tboat.socket.SocketListener;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
import com.tboat.utilsclient.HeaderUtils;
import com.tboat.utilsclient.JsonMapperUtils;
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
                            AuctionSession session = JsonMapperUtils.parseAuctionSession(dataObj);

                            String type = session.getItem().getType();
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
}