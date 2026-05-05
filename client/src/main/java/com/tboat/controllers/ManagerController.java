package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

public class ManagerController extends BaseController implements Initializable, SocketListener {

    // 1. LIÊN KẾT CÁC ID TỪ FXML
    @FXML private TableView<AuctionSession> tableMyItems;
    @FXML private TableColumn<AuctionSession, Integer> colId;
    @FXML private TableColumn<AuctionSession, String> colName;
    @FXML private TableColumn<AuctionSession, String> colType;
    @FXML private TableColumn<AuctionSession, Double> colPrice;
    @FXML private TableColumn<AuctionSession, StatusOfAuction> colStatus;
    @FXML private TableColumn<AuctionSession, Void> colAction;

    private Gson gson = GsonUtils.getInstance();

    // Danh sách để chứa dữ liệu cho TableView
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

        // Định dạng cột Giá tiền (Thêm dấu phẩy và chữ VNĐ)
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
                    private final Button btn = new Button("Chi tiết");
                    {
                        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                        btn.setOnAction((ActionEvent event) -> {
                            AuctionSession data = getTableView().getItems().get(getIndex());
                            System.out.println("Bạn vừa bấm vào sản phẩm: " + data.getName());
                            // Bạn có thể viết code chuyển sang trang Item/Auction ở đây
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        });

        // Gắn danh sách trống vào bảng
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

                    // Xóa dữ liệu cũ trong bảng
                    listMyItems.clear();

                    for (JsonElement element : myArray) {
                        JsonObject dataObj = element.getAsJsonObject();

                        AuctionSession session = new AuctionSession();
                        session.setId(dataObj.has("id") ? dataObj.get("id").getAsInt() : 0);
                        session.setName(dataObj.has("name") ? dataObj.get("name").getAsString() : "No name");
                        session.setCurrentPrice(dataObj.has("currentPrice") ? dataObj.get("currentPrice").getAsDouble() : 0.0);
                        session.setType(dataObj.has("type") ? dataObj.get("type").getAsString() : "Chưa phân loại");
                        String statusString = dataObj.has("statusOfAuction") ? dataObj.get("statusOfAuction").getAsString() : "ONGOING";
                        try {
                            session.setStatusOfAuction(StatusOfAuction.valueOf(statusString));
                        } catch (Exception ignored) {}

                        // Thêm sản phẩm vào danh sách, TableView sẽ tự động hiển thị
                        listMyItems.add(session);
                    }
                }
            } catch (Exception e) {
                if (response.contains("{")) {
                    System.out.println("❌ LỖI ĐỌC JSON TRANG MANAGER: " + response);
                    e.printStackTrace();
                }
            }
        });
    }
}