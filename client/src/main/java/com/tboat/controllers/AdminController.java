package com.tboat.controllers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.tboat.models.AuctionSession;
import com.tboat.models.User;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class AdminController extends BaseController implements Initializable, SocketListener {

    @FXML private TextField txtSearch;
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
        SocketManager.getInstance().subscribe(this);

        this.colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        this.colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.colStartPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        this.colJump.setCellValueFactory(new PropertyValueFactory<>("bidIncrease"));
        this.colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerAccountName"));

        this.setupActionColumn(this.colApprove, "APPROVE_ITEM");
        this.setupActionColumn(this.colReject, "REJECT_ITEM");

        this.sessionList = FXCollections.observableArrayList();
        this.tableSessions.setItems(this.sessionList);

        // 1. Lấy danh sách sản phẩm chờ duyệt
        loadPendingItems();

        // 2. Gửi lệnh lấy Profile Admin để lưu vào UserSession (Dùng cho trang Wallet)
        JsonObject profileReq = new JsonObject();
        profileReq.addProperty("action", "PROFILE");
        SocketManager.getInstance().send(gson.toJson(profileReq));

        // 3. Cài đặt F5 reload
        Platform.runLater(() -> {
            if (txtSearch != null && txtSearch.getScene() != null) {
                txtSearch.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F5) {
                        onReload();
                        event.consume();
                    }
                });
            }
        });
    }

    private void loadPendingItems() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_PENDING_ITEMS");
        SocketManager.getInstance().send(gson.toJson(request));
    }

    @Override
    public void onReload() {
        loadPendingItems();

        // Reload cả profile luôn cho chắc chắn tiền nong khớp
        JsonObject profileReq = new JsonObject();
        profileReq.addProperty("action", "PROFILE");
        SocketManager.getInstance().send(gson.toJson(profileReq));

        if (err != null) {
            err.setStyle("-fx-text-fill: #3498db;");
            err.setText("Đang làm mới dữ liệu hệ thống...");
        }
    }

    private void setupActionColumn(TableColumn<AuctionSession, Void> column, String actionType) {
        column.setCellFactory((param) -> new TableCell<AuctionSession, Void>() {
            private final CheckBox checkBox = new CheckBox();
            {
                this.checkBox.setOnAction((event) -> {
                    if (this.checkBox.isSelected()) {
                        AuctionSession session = getTableView().getItems().get(getIndex());
                        JsonObject request = new JsonObject();
                        request.addProperty("action", actionType);
                        request.addProperty("payload", session.getId());
                        SocketManager.getInstance().send(gson.toJson(request));
                        getTableView().getItems().remove(session);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    this.setGraphic(null);
                } else {
                    this.checkBox.setSelected(false);
                    this.setGraphic(this.checkBox);
                }
            }
        });
    }

    public void switchToAdminNapRut(ActionEvent event) {
        changeScene((Node) event.getSource(), "adminNapRut.fxml");
    }

    public void logout(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất không?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "LOGOUT");
            SocketManager.getInstance().send(gson.toJson(request));
            UserSession.getInstance().cleanUserSession();
            changeScene((Button) e.getSource(), "start.fxml");
        }
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : jsonResponse.get("action").getAsString();
                String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "";

                if ("SUCCESS".equals(status)) {
                    // --- XỬ LÝ LẤY DANH SÁCH CHỜ ---
                    if ("Danh sách chờ duyệt".equals(message)) {
                        sessionList.clear();
                        Type listType = new TypeToken<ArrayList<AuctionSession>>(){}.getType();
                        List<AuctionSession> items = gson.fromJson(jsonResponse.get("payload"), listType);
                        if (items != null) sessionList.addAll(items);
                    }
                    // --- XỬ LÝ LƯU DỮ LIỆU ADMIN VÀO USERSESSION ---
                    /*else if (message.contains("Thông tin tài khoản") && jsonResponse.has("payload")) {
                        JsonObject data = jsonResponse.getAsJsonObject("payload");

                        User adminUser = UserSession.getInstance().getUser();
                        if (adminUser == null) {
                            adminUser = new User();
                            UserSession.getInstance().setUser(adminUser);
                        }

                        adminUser.setNickname(data.has("nickname") ? data.get("nickname").getAsString() : "Admin");
                        adminUser.setBalance(data.has("balance") ? data.get("balance").getAsDouble() : 0.0);
                        adminUser.setAvatarURL(data.has("avatarURL") ? data.get("avatarURL").getAsString() : "");

                        log.info("Đã đồng bộ dữ liệu Admin vào UserSession. Số dư: " + adminUser.getBalance());
                    }*/
                    // --- XỬ LÝ DUYỆT ĐƠN ---
                    else if ("Đã duyệt và bắt đầu đấu giá".equals(message)) {
                        if (err != null) {
                            err.setStyle("-fx-text-fill: green;");
                            err.setText("Đã DUYỆT sản phẩm!");
                        }
                    }
                    // --- XỬ LÝ TỪ CHỐI ĐƠN ---
                    else if ("Đã từ chối sản phẩm".equals(message)) {
                        if (err != null) {
                            err.setStyle("-fx-text-fill: #cc7a00;");
                            err.setText("Đã TỪ CHỐI sản phẩm!");
                        }
                    }
                }
                // --- XỬ LÝ REAL-TIME KHI CÓ NGƯỜI ĐĂNG BÀI MỚI ---
                else if ("NEW_PENDING_ITEM".equals(status) || "NEW_ITEM".equals(status)) {
                    loadPendingItems();
                    if (err != null) {
                        err.setStyle("-fx-text-fill: #9b59b6;");
                        err.setText("Có người dùng vừa đăng sản phẩm mới! Đã tự động cập nhật.");
                    }
                }
                else if ("ERROR".equals(status)) {
                    if (err != null) {
                        err.setStyle("-fx-text-fill: red;");
                        err.setText("Lỗi: " + message);
                    }
                }
            } catch (Exception e) {
                log.severe("LỖI JSON ADMIN: " + e.getMessage());
            }
        });
    }
}