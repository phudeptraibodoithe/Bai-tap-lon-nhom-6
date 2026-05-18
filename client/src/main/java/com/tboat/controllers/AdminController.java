package com.tboat.controllers;

import com.google.gson.*;
import com.tboat.models.*;
import com.tboat.socket.SocketListener;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.*;
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
    @FXML private TableColumn<AuctionSession, Void> colApprove;
    @FXML private TableColumn<AuctionSession, Void> colReject;
    @FXML private Label err;

    private static final Logger log = Logger.getLogger(AdminController.class.getName());
    private ObservableList<AuctionSession> sessionList;
    private final Gson gson = GsonUtils.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        this.colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerAccountName"));
        setupCurrencyColumn(this.colStartPrice, "currentPrice");
        setupCurrencyColumn(this.colJump, "bidIncrease");
        this.setupActionColumn(this.colApprove, "APPROVE_ITEM");
        this.setupActionColumn(this.colReject, "REJECT_ITEM");
        this.sessionList = FXCollections.observableArrayList();
        this.tableSessions.setItems(this.sessionList);

        // ── UCB: Cache-then-Network ──────────────────────────────────────────
        String screenKey = BaseController.toScreenKey("admin.fxml");
        String cached    = DataCache.getInstance().get("GET_PENDING_ITEMS");

        if (cached != null) {
            log.info("[Admin] Cache HIT → render ngay");
            NavigationContext.getInstance().reportCacheHit(screenKey, true);
            renderPendingItems(cached);
            loadPendingItems(); // refresh ngầm
        } else {
            log.info("[Admin] Cache MISS → fetch server");
            NavigationContext.getInstance().reportCacheHit(screenKey, false);
            loadPendingItems();
        }
    }

    private void renderPendingItems(String response) {
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (!"GET_PENDING_ITEMS".equals(json.has("type") ? json.get("type").getAsString() : "")) return;
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
            session.setStatusOfAuction(StatusOfAuction.valueOf(getStringJson(dataObj, "statusOfAuction", "ONGOING")));
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

    private void loadPendingItems() {
        SocketHelper.sendRequest("GET_PENDING_ITEMS", null);
    }

    @Override
    public void onReload() {
        // Hàm này giữ lại phòng trường hợp BaseController gọi đến khi mất kết nối rồi có lại
        loadPendingItems();
        AlertUtils.showStatus(err, "Đang làm mới dữ liệu hệ thống...", "#3498db");
    }

    private void setupCurrencyColumn(TableColumn<AuctionSession, Double> column, String propertyName) {
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(CurrencyFormatter.formatDisplay(price));
                }
            }
        });
    }

    private void setupActionColumn(TableColumn<AuctionSession, Void> column, String actionType) {
        column.setCellFactory(param -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                this.checkBox.setOnAction(event -> {
                    if (this.checkBox.isSelected()) {
                        AuctionSession session = getTableView().getItems().get(getIndex());
                        SocketHelper.sendRequest(actionType, session.getId());
                        // Tạm thời xóa khỏi bảng ngay lập tức cho mượt UI, kết quả thật tính sau
                        getTableView().getItems().remove(session);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : this.checkBox);
                if (!empty) this.checkBox.setSelected(false);
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
                String type    = SocketHelper.getType(response);
                String status  = SocketHelper.getStatus(response);
                String message = SocketHelper.getMessage(response);

                boolean isBroadcast  = "NEW_PENDING_ITEM".equals(status) || "NEW_ITEM".equals(status);
                boolean isMyResponse = "GET_PENDING_ITEMS".equals(type) || "APPROVE_ITEM".equals(type)
                        || "REJECT_ITEM".equals(type) || "LOGOUT".equals(type);

                if (!isBroadcast && !isMyResponse) return;

                switch (status) {
                    case "SUCCESS" -> handleSuccessCase(JsonParser.parseString(response).getAsJsonObject(), message);
                    case "NEW_PENDING_ITEM", "NEW_ITEM" -> {
                        loadPendingItems();
                        AlertUtils.showStatus(err, "Có người dùng vừa đăng sản phẩm mới! Đã tự động cập nhật.", "#9b59b6");
                    }
                    case "ERROR" -> AlertUtils.showStatus(err, "Lỗi: " + message, "red");
                }
            } catch (Exception e) {
                log.severe("LỖI JSON ADMIN: " + e.getMessage());
            }
        });
    }

    private void handleSuccessCase(JsonObject jsonResponse, String message) {
        if ("Danh sách chờ duyệt".equals(message)) {
            renderPendingItems(jsonResponse.toString()); // ← dùng method mới
        } else if ("Đã duyệt và bắt đầu đấu giá".equals(message)) {
            AlertUtils.showStatus(err, "Đã DUYỆT sản phẩm!", "green");
        } else if ("Đã từ chối sản phẩm".equals(message)) {
            AlertUtils.showStatus(err, "Đã TỪ CHỐI sản phẩm!", "#cc7a00");
        }
    }
}