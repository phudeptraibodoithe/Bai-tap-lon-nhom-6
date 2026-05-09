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
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
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

    private final Gson gson = GsonUtils.getInstance();
    private static final Logger logger = Logger.getLogger(ManagerController.class.getName());
    private ObservableList<AuctionSession> listMyItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Khởi tạo các cột cho bảng
        setupTableColumns();

        // Đăng ký nhận tin nhắn từ Server
        SocketManager.getInstance().subscribe(this);

        // Lấy dữ liệu từ Server
        loadMyAuctions();
    }

    // ====================================================================
    // CẤU HÌNH CÁC CỘT CHO TABLEVIEW
    // ====================================================================
    private void setupTableColumns() {
        // Gắn tên thuộc tính của class AuctionSession vào các cột
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

                            // Ẩn/Hiện nút dựa theo trạng thái
                            if (status == StatusOfAuction.ENDED || status == StatusOfAuction.CANCELED) {
                                btn.setDisable(true); // Khóa nút không cho bấm
                                btn.setText("Đã đóng");
                                btn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white;"); // Đổi màu xám nhìn cho nó "hết hạn"
                            } else {
                                btn.setDisable(false); // Mở khóa nút
                                btn.setText("Chỉnh sửa");
                                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;"); // Màu xanh blue bình thường
                            }
                            setGraphic(btn);
                        }
                    }
                };
            }
        });
        tableMyItems.setItems(listMyItems);
    }

    // ====================================================================
    // GỬI YÊU CẦU LÊN SERVER
    // ====================================================================
    public void loadMyAuctions() {
        String myUsername = UserSession.getInstance().getUsername();
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_MY_AUCTIONS");
        request.addProperty("payload", myUsername);

        SocketManager.getInstance().send(gson.toJson(request));
    }

    // ====================================================================
    // XỬ LÝ DỮ LIỆU JSON TỪ SERVER
    // ====================================================================
    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.get("status").getAsString();

                if ("SUCCESS".equals(status) && jsonResponse.has("payload") && jsonResponse.get("payload").isJsonArray()) {
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
                        LocalDateTime startTime = LocalDateTime.now();
                        if (dataObj.has("startTime") && !dataObj.get("startTime").isJsonNull()) {
                            startTime = LocalDateTime.parse(dataObj.get("startTime").getAsString());
                        }
                        LocalDateTime endTime = LocalDateTime.now().plusDays(1);
                        if (dataObj.has("endTime") && !dataObj.get("endTime").isJsonNull()) {
                            endTime = LocalDateTime.parse(dataObj.get("endTime").getAsString());
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
            } catch (Exception e) {
                if (response.contains("{")) {
                    logger.severe("❌ LỖI ĐỌC JSON TRANG MANAGER: " + response);
                }
            }
        });
    }
}