package com.tboat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.CurrencyFormatter;
import com.tboat.utilsclient.HeaderUtils;
import com.tboat.utilsclient.SocketHelper;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ControllerHistory extends BaseController implements Initializable, SocketListener {

    @FXML private VBox lichsu;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    private static final double PAYOUT_RATE=0.9;
    private static final Logger log = Logger.getLogger(ControllerHistory.class.getName());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);

        // ── UCB: Cache-then-Network ──────────────────────────────────────────
        String screenKey = BaseController.toScreenKey("history.fxml");
        String cached    = DataCache.getInstance().get("GET_HISTORY");

        if (cached != null) {
            log.info("[History] Cache HIT → render ngay");
            NavigationContext.getInstance().reportCacheHit(screenKey, true);
            handleServerResponse(cached);
            loadlichsu();
        } else {
            log.info("[History] Cache MISS → fetch server");
            NavigationContext.getInstance().reportCacheHit(screenKey, false);
            loadlichsu();
        }
        // ────────────────────────────────────────────────────────────────────
    }

    public void loadlichsu() {
        lichsu.getChildren().clear();
        String username = UserSession.getInstance().getUsername();
        SocketHelper.sendRequest("GET_HISTORY", username);
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                if (!"GET_HISTORY".equals(SocketHelper.getType(response))) return;
                if (!"SUCCESS".equals(SocketHelper.getStatus(response))) return;

                JsonArray historyArray = SocketHelper.getPayloadArray(response);
                if (historyArray == null) return;

                lichsu.getChildren().clear();
                String me = UserSession.getInstance().getUsername();

                for (JsonElement element : historyArray) {
                    JsonObject dataObj = element.getAsJsonObject();

                    String name   = getStringSafe(dataObj, "name", "N/A");
                    String id     = getStringSafe(dataObj, "auctionSessionId", "0");
                    String role   = getStringSafe(dataObj, "roleType", "BIDDER");
                    String winner = (dataObj.has("winnerAccountName") && !dataObj.get("winnerAccountName").isJsonNull())
                            ? dataObj.get("winnerAccountName").getAsString() : null;
                    double finalPrice = dataObj.has("finalPrice") ? dataObj.get("finalPrice").getAsDouble() : 0.0;

                    String statusText, moneyDisplay, colorStatus;

                    if ("SELLER".equalsIgnoreCase(role)) {
                        name = "[BÁN] " + name;
                        if (winner == null) {
                            statusText = "Đang rao bán"; moneyDisplay = "0 VNĐ"; colorStatus = "#f39c12";
                        } else {
                            statusText = "Đã bán";
                            moneyDisplay = "+" + CurrencyFormatter.formatDisplay(finalPrice * PAYOUT_RATE);
                            colorStatus = "#27ae60";
                        }
                    } else {
                        name = "[MUA] " + name;
                        if (winner == null) {
                            statusText = "Đang diễn ra"; moneyDisplay = "0 VNĐ"; colorStatus = "#f39c12";
                        } else if (me.equalsIgnoreCase(winner)) {
                            statusText = "Thành công";
                            moneyDisplay = "-" + CurrencyFormatter.formatDisplay(finalPrice);
                            colorStatus = "#27ae60";
                        } else {
                            statusText = "Thất bại";
                            moneyDisplay = CurrencyFormatter.formatDisplay(finalPrice);
                            colorStatus = "#e74c3c";
                        }
                    }
                    lichsu.getChildren().add(createHistoryRow(name, "ID: " + id, statusText, moneyDisplay, colorStatus));
                }
            } catch (Exception e) {
                log.severe("Lỗi render: " + e.getMessage());
            }
        });
    }

    private String getStringSafe(JsonObject obj, String memberName, String defaultValue) {
        if (obj.has(memberName) && !obj.get(memberName).isJsonNull()) {
            return obj.get(memberName).getAsString();
        }
        return defaultValue;
    }

    private HBox createHistoryRow(String name, String id, String result, String bienDong, String colorCode) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(80.0);
        row.setPadding(new Insets(15, 25, 15, 25));

        row.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 10; " +
                "-fx-border-radius: 10; " +
                "-fx-border-width: 1; " +
                "-fx-border-color: #dddddd; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        Label ten = new Label(name);
        ten.setPrefWidth(280.0);
        ten.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 17px;");

        Label lblId = new Label(id);
        lblId.setAlignment(Pos.CENTER);
        lblId.setPrefWidth(100.0);
        lblId.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 15px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label kq = new Label(result);
        kq.setAlignment(Pos.CENTER);
        kq.setPrefWidth(160.0);
        kq.setStyle("-fx-text-fill: " + colorCode + "; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label lblBienDong = new Label(bienDong);
        lblBienDong.setAlignment(Pos.CENTER_RIGHT);
        lblBienDong.setPrefWidth(180.0);
        lblBienDong.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 17px;");

        row.getChildren().addAll(ten, lblId, spacer, kq, lblBienDong);
        return row;
    }
}