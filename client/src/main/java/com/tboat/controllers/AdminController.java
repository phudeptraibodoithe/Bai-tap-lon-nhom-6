package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.item.Item;
import com.tboat.models.item.factory.ItemFactory;
import com.tboat.models.item.factory.ItemFactoryProducer;
import com.tboat.session.UserSession;
import com.tboat.socket.SocketHelper;
import com.tboat.socket.SocketListener;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.AlertUtils;
import com.tboat.utilsclient.CurrencyFormatter;
import com.tboat.utilsclient.TimeUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class AdminController extends BaseController implements Initializable, SocketListener {

    @FXML private TableView<AuctionSession> tableSessions;
    @FXML private TableColumn<AuctionSession, Integer> colId;
    @FXML private TableColumn<AuctionSession, String> colName;
    @FXML private TableColumn<AuctionSession, Double> colStartPrice;
    @FXML private TableColumn<AuctionSession, Double> colJump;
    @FXML private TableColumn<AuctionSession, String> colSeller;
    @FXML private TableColumn<AuctionSession, StatusOfAuction> colStatus; // ← thêm
    @FXML private TableColumn<AuctionSession, Void> colApprove;
    @FXML private TableColumn<AuctionSession, Void> colReject;
    @FXML private Label err;

    private static final Logger log = Logger.getLogger(AdminController.class.getName());
    private ObservableList<AuctionSession> sessionList;
    private final Gson gson = GsonUtils.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerAccountName"));
        setupCurrencyColumn(colStartPrice, "currentPrice");
        setupCurrencyColumn(colJump, "bidIncrease");
        setupStatusColumn();       // ← thêm
        setupApproveColumn();      // ← tách riêng, có disable logic
        setupRejectColumn();       // ← tách riêng, có disable logic
        sessionList = FXCollections.observableArrayList();
        tableSessions.setItems(sessionList);

        String screenKey = BaseController.toScreenKey("admin.fxml");
        String cached    = DataCache.getInstance().get("GET_ALL_ITEMS"); // ← đổi key

        if (cached != null) {
            log.info("[Admin] Cache HIT → render ngay");
            NavigationContext.getInstance().reportCacheHit(screenKey, true);
            renderAllItems(cached);
            loadAllItems();
        } else {
            log.info("[Admin] Cache MISS → fetch server");
            NavigationContext.getInstance().reportCacheHit(screenKey, false);
            loadAllItems();
        }
    }

    private void renderAllItems(String response) {
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (!"GET_ALL_ITEMS".equals(json.has("type") ? json.get("type").getAsString() : "")) return;
            if (!json.has("payload") || !json.get("payload").isJsonArray()) return;

            sessionList.clear();
            for (JsonElement element : json.getAsJsonArray("payload")) {
                AuctionSession session = parseSingleAuctionSession(element.getAsJsonObject());
                if (session != null) sessionList.add(session);
            }
        } catch (Exception e) {
            log.warning("[Admin] Lỗi render: " + e.getMessage());
        }
    }

    private AuctionSession parseSingleAuctionSession(JsonObject dataObj) {
        JsonObject itemObj = dataObj.has("item") && dataObj.get("item").isJsonObject()
                ? dataObj.getAsJsonObject("item") : dataObj;

        String type          = getStringJson(itemObj, "type", "Khác");
        String name          = getStringJson(itemObj, "name", "");
        String description   = getStringJson(itemObj, "description", "");
        String imageURL      = getStringJson(itemObj, "imageURL", "");
        String sellerAccount = getStringJson(itemObj, "sellerAccountName", "");
        double currentPrice  = getDoubleJson(dataObj, "currentPrice", 0.0);
        double bidIncrease   = getDoubleJson(dataObj, "bidIncrease", 0.0);

        ItemFactory factory = ItemFactoryProducer.getFactory(type);
        Item item = factory.createItem(sellerAccount, name, description, imageURL);

        AuctionSession session = new AuctionSession(
                dataObj.has("startTime") ? TimeUtils.parseServerTime(dataObj.get("startTime")) : LocalDateTime.now(),
                dataObj.has("endTime")   ? TimeUtils.parseServerTime(dataObj.get("endTime"))   : LocalDateTime.now().plusDays(1),
                currentPrice, bidIncrease, item
        );
        session.setId(dataObj.has("id") ? dataObj.get("id").getAsInt() : 0);

        try {
            session.setStatusOfAuction(StatusOfAuction.valueOf(
                    getStringJson(dataObj, "statusOfAuction", "ONGOING")));
        } catch (Exception ignored) {
            session.setStatusOfAuction(StatusOfAuction.ONGOING);
        }
        return session;
    }

    private String getStringJson(JsonObject json, String key, String def) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : def;
    }
    private double getDoubleJson(JsonObject json, String key, double def) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : def;
    }

    private void loadAllItems() {
        SocketHelper.sendRequest("GET_ALL_ITEMS", null);
    }

    @Override
    public void onReload() {
        loadAllItems();
        AlertUtils.showStatus(err, "Đang làm mới dữ liệu hệ thống...", "#3498db");
    }

    // ── Cell factories ───────────────────────────────────────────────────────

    private void setupCurrencyColumn(TableColumn<AuctionSession, Double> column, String propertyName) {
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : CurrencyFormatter.formatDisplay(price));
            }
        });
    }

    private void setupStatusColumn() {
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusOfAuction"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StatusOfAuction status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                switch (status) {
                    case PENDING     -> { setText("Chờ duyệt");  setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); }
                    case NOT_STARTED -> { setText("Sắp diễn ra"); setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;"); }
                    case ONGOING     -> { setText("Đang diễn ra"); setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); }
                    case ENDED       -> { setText("Đã kết thúc"); setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;"); }
                    case CANCELED    -> { setText("Từ chối");    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); }
                    default          -> { setText(status.name()); setStyle(""); }
                }
            }
        });
    }

    private void setupApproveColumn() {
        colApprove.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Duyệt");
            {
                btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    AuctionSession session = getTableView().getItems().get(getIndex());
                    SocketHelper.sendRequest("APPROVE_ITEM", session.getId());
                    // Cập nhật status local ngay để UI phản hồi nhanh
                    session.setStatusOfAuction(StatusOfAuction.NOT_STARTED);
                    getTableView().refresh();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AuctionSession session = getTableView().getItems().get(getIndex());
                btn.setDisable(!StatusOfAuction.PENDING.equals(session.getStatusOfAuction()));
                setGraphic(btn);
            }
        });
    }

    private void setupRejectColumn() {
        colReject.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Từ chối");
            {
                btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    AuctionSession session = getTableView().getItems().get(getIndex());
                    SocketHelper.sendRequest("REJECT_ITEM", session.getId());
                    session.setStatusOfAuction(StatusOfAuction.CANCELED);
                    getTableView().refresh();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AuctionSession session = getTableView().getItems().get(getIndex());
                btn.setDisable(!StatusOfAuction.PENDING.equals(session.getStatusOfAuction()));
                setGraphic(btn);
            }
        });
    }

    public void switchToAdminNapRut(ActionEvent event) {
        changeScene((Node) event.getSource(), "adminWallet.fxml");
    }

    public void logout(ActionEvent e) {
        if (AlertUtils.showConfirmation("Xác nhận đăng xuất", "Bạn có chắc chắn muốn đăng xuất không?")) {
            SocketHelper.sendRequest("LOGOUT", null);
            UserSession.getInstance().cleanUserSession();
            changeScene((Node) e.getSource(), "start.fxml");
        }
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                String type = SocketHelper.getType(response);

                if (handleBroadcast(type)) return;

                boolean isMyType = "GET_ALL_ITEMS".equals(type)
                        || "APPROVE_ITEM".equals(type)
                        || "REJECT_ITEM".equals(type);
                if (!isMyType) return;

                String status = SocketHelper.getStatus(response);
                if ("SUCCESS".equals(status)) {
                    handleAdminSuccess(type, response);
                } else if ("ERROR".equals(status)) {
                    AlertUtils.showStatus(err, "Lỗi: " + SocketHelper.getMessage(response), "red");
                }

            } catch (Exception e) {
                log.severe("LỖI JSON ADMIN: " + e.getMessage());
            }
        });
    }

    private boolean handleBroadcast(String type) {
        if ("RELOAD_ALL_ITEMS".equals(type) || "RELOAD_PENDING_ITEMS".equals(type)) {
            loadAllItems();
            AlertUtils.showStatus(err, "Dữ liệu vừa được cập nhật tự động.", "#9b59b6");
            return true;
        }
        return false;
    }

    private void handleAdminSuccess(String type, String response) {
        switch (type) {
            case "GET_ALL_ITEMS" -> {
                DataCache.getInstance().put("GET_ALL_ITEMS", response);
                renderAllItems(response);
            }
            case "APPROVE_ITEM" -> AlertUtils.showStatus(err, "Đã DUYỆT sản phẩm!", "green");
            case "REJECT_ITEM"  -> AlertUtils.showStatus(err, "Đã TỪ CHỐI sản phẩm!", "#cc7a00");
        }
    }
}