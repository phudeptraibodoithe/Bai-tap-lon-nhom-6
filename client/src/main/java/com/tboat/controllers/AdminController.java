package com.tboat.controllers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.tboat.models.AuctionSession;
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
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class AdminController extends BaseController implements Initializable, SocketListener {
//test xem push lên main được khônh
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
        this.colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        this.colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.colStartPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        this.colJump.setCellValueFactory(new PropertyValueFactory<>("bidIncrease"));
        this.colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerAccountName"));

        this.setupActionColumn(this.colApprove, "APPROVE_ITEM");
        this.setupActionColumn(this.colReject, "REJECT_ITEM");

        this.sessionList = FXCollections.observableArrayList();
        this.tableSessions.setItems(this.sessionList);

        Platform.runLater(() -> {
            JsonObject request = new JsonObject();
            request.addProperty("action", "GET_PENDING_ITEMS");
            SocketManager.getInstance().send(gson.toJson(request));
        });
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
                        log.info("Gửi lệnh (" + actionType + ") cho: " + session.getName());
                        SocketManager.getInstance().send(gson.toJson(request));

                        getTableView().getItems().remove(session);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    this.setGraphic((Node) null);
                } else {
                    this.checkBox.setSelected(false);
                    this.setGraphic(this.checkBox);
                }
            }
        });
    }

    public void logout(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        DialogPane dialogPane = alert.getDialogPane();
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Quản trị không?");
        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        if (alert.showAndWait().orElse(btnNo) == btnYes) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "LOGOUT");
            SocketManager.getInstance().send(gson.toJson(request));

            UserSession.getInstance().cleanUserSession();

            Button btnSource = (Button) e.getSource();
            changeScene(btnSource, "start.fxml");
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
                    if ("Danh sách chờ duyệt".equals(message)) {
                        sessionList.clear();
                        Type listType = new TypeToken<ArrayList<AuctionSession>>(){}.getType();
                        List<AuctionSession> items = gson.fromJson(jsonResponse.get("payload"), listType);

                        if (items != null) {
                            sessionList.addAll(items);
                        }
                    } else if ("Đã duyệt và bắt đầu đấu giá".equals(message)) {
                        if (err != null) {
                            err.setStyle("-fx-text-fill: green;");
                            err.setText("Đã DUYỆT sản phẩm ID: " + jsonResponse.get("payload").getAsString());
                        }
                    } else if ("Đã từ chối sản phẩm".equals(message)) {
                        if (err != null) {
                            err.setStyle("-fx-text-fill: #cc7a00;");
                            err.setText("Đã TỪ CHỐI sản phẩm ID: " + jsonResponse.get("payload").getAsString());
                        }
                    }
                } else if ("ERROR".equals(status)) {
                    if (err != null) {
                        err.setStyle("-fx-text-fill: red;");
                        err.setText("Lỗi: " + message);
                    }
                    log.severe("LỖI TỪ SERVER: " + message);
                }
            } catch (Exception e) {
                log.severe("KHÔNG THỂ ĐỌC JSON TỪ SERVER: " + response);
            }
        });
    }
}