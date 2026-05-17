package com.tboat.controllers;

import com.google.gson.*;
import com.tboat.models.*;
import com.tboat.socket.SocketListener;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
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
import javafx.scene.image.ImageView;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ManagerController extends BaseController implements Initializable, SocketListener {

    private static final Logger logger = Logger.getLogger(ManagerController.class.getName());
    private final ObservableList<AuctionSession> listMyItems = FXCollections.observableArrayList();

    private static final String STYLE_BTN_EDIT   = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;";
    private static final String STYLE_BTN_CLOSED = "-fx-background-color: #bdc3c7; -fx-text-fill: white;";

    @FXML private TableView<AuctionSession> tableMyItems;
    @FXML private TableColumn<AuctionSession, Integer> colId;
    @FXML private TableColumn<AuctionSession, String> colName;
    @FXML private TableColumn<AuctionSession, String> colType;
    @FXML private TableColumn<AuctionSession, Double> colPrice;
    @FXML private TableColumn<AuctionSession, StatusOfAuction> colStatus;
    @FXML private TableColumn<AuctionSession, Void> colAction;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
        handleCacheAndNetworkAndRefresh();
    }

    private void handleCacheAndNetworkAndRefresh() {
        String screenKey = BaseController.toScreenKey("manager.fxml");
        String cachedData = DataCache.getInstance().get("GET_MY_AUCTIONS");

        if (cachedData != null) {
            logger.info("[Manager] Cache HIT → Hiển thị giao diện ngay lập tức.");
            NavigationContext.getInstance().reportCacheHit(screenKey, true);
            renderMyAuctions(cachedData);
            loadMyAuctions();
        } else {
            logger.info("[Manager] Cache MISS → Đợi dữ liệu từ mạng.");
            NavigationContext.getInstance().reportCacheHit(screenKey, false);
            loadMyAuctions();
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusOfAuction"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : CurrencyFormatter.formatDisplay(price));
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Chỉnh sửa");
            {
                btn.setOnAction((ActionEvent event) -> {
                    AuctionSession data = getTableView().getItems().get(getIndex());
                    logger.info("Bạn vừa bấm vào sản phẩm để sửa: " + data.getName());
                    ControllerEditItem editController = changeSceneAndGetController((Node) event.getSource(), "editItem.fxml");
                    if (editController != null) {
                        editController.setEditData(data);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    AuctionSession session = getTableRow().getItem();
                    StatusOfAuction status = session.getStatusOfAuction();
                    if (status == StatusOfAuction.ENDED || status == StatusOfAuction.CANCELED) {
                        btn.setDisable(true);
                        btn.setText("Đã đóng");
                        btn.setStyle(STYLE_BTN_CLOSED);
                    } else {
                        btn.setDisable(false);
                        btn.setText("Chỉnh sửa");
                        btn.setStyle(STYLE_BTN_EDIT);
                    }
                    setGraphic(btn);
                }
            }
        });
        tableMyItems.setItems(listMyItems);
    }

    public void loadMyAuctions() {
        SocketHelper.sendRequest("GET_MY_AUCTIONS", UserSession.getInstance().getUsername());
    }

    private void renderMyAuctions(String response) {
        try {
            if (!"SUCCESS".equals(SocketHelper.getStatus(response))) return;
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            if (!jsonResponse.has("payload") || !jsonResponse.get("payload").isJsonArray()) return;

            JsonArray myArray = jsonResponse.getAsJsonArray("payload");
            listMyItems.clear();

            for (JsonElement element : myArray) {
                try {
                    AuctionSession session = parseSingleAuctionSession(element.getAsJsonObject());
                    if (session != null) listMyItems.add(session);
                } catch (Exception e) {
                    logger.warning("[Manager] Lỗi xử lý một phiên: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.severe("[Manager] Khởi tạo dữ liệu bảng thất bại: " + e.getMessage());
        }
    }

    private AuctionSession parseSingleAuctionSession(JsonObject dataObj) {
        String type          = getStringJson(dataObj, "type", "Khác");
        String name          = getStringJson(dataObj, "name", "");
        String description   = getStringJson(dataObj, "description", "");
        String imageURL      = getStringJson(dataObj, "imageURL", "");
        String sellerAccount = getStringJson(dataObj, "sellerAccountName", "");
        double currentPrice  = getDoubleJson(dataObj, "currentPrice", 0.0);
        double bidIncrease   = getDoubleJson(dataObj, "bidIncrease", 0.0);

        LocalDateTime startTime = dataObj.has("startTime") ? TimeUtils.parseServerTime(dataObj.get("startTime")) : LocalDateTime.now();
        LocalDateTime endTime   = dataObj.has("endTime")   ? TimeUtils.parseServerTime(dataObj.get("endTime"))   : LocalDateTime.now().plusDays(1);

        ItemFactory factory = ItemFactoryProducer.getFactory(type);
        Item item = factory.createItem(sellerAccount, name, description, imageURL);

        AuctionSession session = new AuctionSession(startTime, endTime, currentPrice, bidIncrease, item);
        session.setId(dataObj.has("id") ? dataObj.get("id").getAsInt() : 0);

        String statusStr = getStringJson(dataObj, "statusOfAuction", "ONGOING");
        try {
            session.setStatusOfAuction(StatusOfAuction.valueOf(statusStr));
        } catch (Exception ignored) {
            session.setStatusOfAuction(StatusOfAuction.ONGOING);
        }
        return session;
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                if (!"GET_MY_AUCTIONS".equals(SocketHelper.getType(response))) return;

                if ("SUCCESS".equals(SocketHelper.getStatus(response))
                        && SocketHelper.getPayloadArray(response) != null) {
                    DataCache.getInstance().put("GET_MY_AUCTIONS", response);
                    renderMyAuctions(response);
                }
            } catch (Exception e) {
                logger.warning("[Manager] Lỗi xử lý phản hồi: " + e.getMessage());
            }
        });
    }

    private String getStringJson(JsonObject json, String key, String defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : defaultValue;
    }

    private double getDoubleJson(JsonObject json, String key, double defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : defaultValue;
    }
}