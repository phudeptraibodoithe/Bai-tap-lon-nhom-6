package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.AuctionFactory;
import com.tboat.models.AuctionFactoryProducer;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.HeaderUtils; // Import class dùng chung
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ManagerController extends BaseController implements Initializable, SocketListener {

    @FXML private TableView<AuctionSession> tableMyItems;
    @FXML private TableColumn<AuctionSession, Integer> colId;
    @FXML private TableColumn<AuctionSession, String> colName;
    @FXML private TableColumn<AuctionSession, String> colType;
    @FXML private TableColumn<AuctionSession, Double> colPrice;
    @FXML private TableColumn<AuctionSession, StatusOfAuction> colStatus;
    @FXML private TableColumn<AuctionSession, Void> colAction;

    // ĐÃ THÊM: Khai báo 2 biến UI cho Header
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    private final Gson gson = GsonUtils.getInstance();
    private static final Logger logger = Logger.getLogger(ManagerController.class.getName());
    private ObservableList<AuctionSession> listMyItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);

        // ── UCB: Cache-then-Network ──────────────────────────────────────────
        String screenKey = BaseController.toScreenKey("manager.fxml"); // = "managerFxml"
        String cached = DataCache.getInstance().get("GET_MY_AUCTIONS");

        if (cached != null) {
            // CACHE HIT: hiển thị ngay, không chờ server
            log.info("[Manager] Cache HIT → render ngay");
            NavigationContext.getInstance().reportCacheHit(screenKey, true);
            renderMyAuctions(cached);                    // render từ cache
            loadMyAuctions();                            // vẫn refresh ngầm

        } else {
            // CACHE MISS: fetch bình thường
            log.info("[Manager] Cache MISS → fetch server");
            NavigationContext.getInstance().reportCacheHit(screenKey, false);
            loadMyAuctions();
        }
        // ────────────────────────────────────────────────────────────────────
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusOfAuction"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colPrice.setCellFactory(column -> new TableCell<AuctionSession, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                }
            }
        });

        colAction.setCellFactory(new Callback<TableColumn<AuctionSession, Void>, TableCell<AuctionSession, Void>>() {
            @Override
            public TableCell<AuctionSession, Void> call(final TableColumn<AuctionSession, Void> param) {
                return new TableCell<AuctionSession, Void>() {
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
                                btn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white;");
                            } else {
                                btn.setDisable(false);
                                btn.setText("Chỉnh sửa");
                                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                            }
                            setGraphic(btn);
                        }
                    }
                };
            }
        });
        tableMyItems.setItems(listMyItems);
    }

    public void loadMyAuctions() {
        String myUsername = UserSession.getInstance().getUsername();
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_MY_AUCTIONS");
        request.addProperty("payload", myUsername);

        SocketManager.getInstance().send(gson.toJson(request));
    }

    // Thêm method này vào ManagerController
    private void renderMyAuctions(String response) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            String status = jsonResponse.get("status").getAsString();

            if ("SUCCESS".equals(status)
                    && jsonResponse.has("payload")
                    && jsonResponse.get("payload").isJsonArray()) {

                JsonArray myArray = jsonResponse.getAsJsonArray("payload");
                listMyItems.clear();

                for (JsonElement element : myArray) {
                    JsonObject dataObj = element.getAsJsonObject();

                    String type        = dataObj.has("type") ? dataObj.get("type").getAsString() : "Khác";
                    String name        = dataObj.has("name") ? dataObj.get("name").getAsString() : "No name";
                    double currentPrice= dataObj.has("currentPrice") ? dataObj.get("currentPrice").getAsDouble() : 0.0;
                    double bidIncrease = dataObj.has("bidIncrease") ? dataObj.get("bidIncrease").getAsDouble() : 0.0;
                    String sellerAccount = dataObj.has("sellerAccountName") && !dataObj.get("sellerAccountName").isJsonNull()
                            ? dataObj.get("sellerAccountName").getAsString() : "";
                    String description = dataObj.has("description") && !dataObj.get("description").isJsonNull()
                            ? dataObj.get("description").getAsString() : "";
                    String imageURL    = dataObj.has("imageURL") && !dataObj.get("imageURL").isJsonNull()
                            ? dataObj.get("imageURL").getAsString() : "";

                    LocalDateTime startTime = LocalDateTime.now();
                    if (dataObj.has("startTime") && !dataObj.get("startTime").isJsonNull())
                        startTime = LocalDateTime.parse(dataObj.get("startTime").getAsString());

                    LocalDateTime endTime = LocalDateTime.now().plusDays(1);
                    if (dataObj.has("endTime") && !dataObj.get("endTime").isJsonNull())
                        endTime = LocalDateTime.parse(dataObj.get("endTime").getAsString());

                    AuctionFactory factory = AuctionFactoryProducer.getFactory(type);
                    AuctionSession session = factory.createAuctionSession(
                            startTime, endTime, currentPrice, bidIncrease,
                            sellerAccount, name, description, imageURL);

                    session.setId(dataObj.has("id") ? dataObj.get("id").getAsInt() : 0);
                    String statusStr = dataObj.has("statusOfAuction")
                            ? dataObj.get("statusOfAuction").getAsString() : "ONGOING";
                    try {
                        session.setStatusOfAuction(StatusOfAuction.valueOf(statusStr));
                    } catch (Exception ignored) {
                        session.setStatusOfAuction(StatusOfAuction.ONGOING);
                    }
                    listMyItems.add(session);
                }
            }
        } catch (Exception e) {
            logger.warning("[Manager] Lỗi render: " + e.getMessage());
        }
    }

    // Sửa handleServerResponse → gọi renderMyAuctions
    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                String status = json.has("status") ? json.get("status").getAsString() : "";
                // Chỉ xử lý khi SUCCESS + payload là array (tránh nhầm với các response khác)
                if ("SUCCESS".equals(status)
                        && json.has("payload")
                        && json.get("payload").isJsonArray()) {
                    renderMyAuctions(response);
                }
            } catch (Exception ignored) {}
        });
    }
}