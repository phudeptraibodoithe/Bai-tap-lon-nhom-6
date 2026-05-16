package com.tboat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.AuctionFactory;
import com.tboat.models.AuctionFactoryProducer;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.socket.SocketListener;
import com.tboat.utilsclient.*;
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

    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    private static final Logger logger = Logger.getLogger(ManagerController.class.getName());
    private ObservableList<AuctionSession> listMyItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
        setupTableColumns();
        loadMyAuctions();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusOfAuction"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        // 👉 Áp dụng Utils xử lý tiền tệ
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
                        btn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white;");
                    } else {
                        btn.setDisable(false);
                        btn.setText("Chỉnh sửa");
                        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                    }
                    setGraphic(btn);
                }
            }
        });
        tableMyItems.setItems(listMyItems);
    }

    public void loadMyAuctions() {
        // 👉 Sử dụng Utils gọi API siêu ngắn gọn
        SocketHelper.sendRequest("GET_MY_AUCTIONS", UserSession.getInstance().getUsername());
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                // 👉 Sử dụng Utils đọc trạng thái
                String status = SocketHelper.getStatus(response);

                if ("SUCCESS".equals(status)) {
                    JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                    if (jsonResponse.has("payload") && jsonResponse.get("payload").isJsonArray()) {
                        JsonArray myArray = jsonResponse.getAsJsonArray("payload");
                        listMyItems.clear();

                        for (JsonElement element : myArray) {
                            JsonObject dataObj = element.getAsJsonObject();

                            String type = dataObj.has("type") ? dataObj.get("type").getAsString() : "Khác";
                            String name = dataObj.has("name") ? dataObj.get("name").getAsString() : "No name";
                            double currentPrice = dataObj.has("currentPrice") ? dataObj.get("currentPrice").getAsDouble() : 0.0;
                            double bidIncrease = dataObj.has("bidIncrease") ? dataObj.get("bidIncrease").getAsDouble() : 0.0;
                            String sellerAccount = dataObj.has("sellerAccountName") && !dataObj.get("sellerAccountName").isJsonNull() ? dataObj.get("sellerAccountName").getAsString() : "";
                            String description = dataObj.has("description") && !dataObj.get("description").isJsonNull() ? dataObj.get("description").getAsString() : "";
                            String imageURL = dataObj.has("imageURL") && !dataObj.get("imageURL").isJsonNull() ? dataObj.get("imageURL").getAsString() : "";

                            // 👉 Áp dụng Utils xử lý thời gian từ Server
                            // 👉 Xử lý thời gian bắt đầu an toàn
                            LocalDateTime startTime = LocalDateTime.now();
                            if (dataObj.has("startTime") && !dataObj.get("startTime").isJsonNull()) {
                                String startStr = dataObj.get("startTime").getAsString().replace(" ", "T");
                                // Nếu chuỗi chỉ có 16 ký tự (VD: 2026-05-16T17:22), bù thêm giây ":00" vào cho đủ form
                                if (startStr.length() == 16) startStr += ":00";
                                // Đảm bảo không bị quá giới hạn 19 ký tự
                                startTime = LocalDateTime.parse(startStr.length() > 19 ? startStr.substring(0, 19) : startStr);
                            }

                            LocalDateTime endTime = LocalDateTime.now().plusDays(1);
                            if (dataObj.has("endTime") && !dataObj.get("endTime").isJsonNull()) {
                                String endStr = dataObj.get("endTime").getAsString().replace(" ", "T");
                                if (endStr.length() == 16) endStr += ":00";
                                endTime = LocalDateTime.parse(endStr.length() > 19 ? endStr.substring(0, 19) : endStr);
                            }

                            AuctionFactory factory = AuctionFactoryProducer.getFactory(type);
                            AuctionSession session = factory.createAuctionSession(
                                    startTime, endTime, currentPrice, bidIncrease,
                                    sellerAccount, name, description, imageURL
                            );
                            session.setId(dataObj.has("id") ? dataObj.get("id").getAsInt() : 0);

                            String statusString = dataObj.has("statusOfAuction") ? dataObj.get("statusOfAuction").getAsString() : "ONGOING";
                            try {
                                session.setStatusOfAuction(StatusOfAuction.valueOf(statusString));
                            } catch (Exception ignored) {
                                session.setStatusOfAuction(StatusOfAuction.ONGOING);
                            }

                            listMyItems.add(session);
                        }
                    }
                } else {
                    String message = SocketHelper.getMessage(response);
                    logger.warning("Không thể lấy danh sách đấu giá: " + message);
                }
            } catch (Exception e) {
                if (response.contains("{")) {
                    logger.severe("❌ LỖI ĐỌC JSON TRANG MANAGER: " + response);
                }
            }
        });
    }
}