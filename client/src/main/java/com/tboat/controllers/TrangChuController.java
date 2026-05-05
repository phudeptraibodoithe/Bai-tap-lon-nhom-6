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
import com.tboat.utilsclient.ImageUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class TrangChuController extends BaseController implements Initializable, SocketListener {

    private static final Logger logger = Logger.getLogger(TrangChuController.class.getName());

    @FXML private FlowPane itemContainer;

    // Các nút điều hướng (Nên có fx:id trong FXML)
    @FXML private Button btnHome;
    @FXML private Button btnHistory;
    @FXML private Button btnPostItem;
    @FXML private Button btnHistory1;

    private Gson gson = GsonUtils.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đăng ký class này để lắng nghe tin nhắn từ Server
        SocketManager.getInstance().subscribe(this);
        // Load danh sách ngay khi mở trang
        loadAuctions();
    }

    // =========================================================
    // 1. GỬI YÊU CẦU LẤY DANH SÁCH SẢN PHẨM LÊN SERVER
    // =========================================================
    public void loadAuctions() {
        if (itemContainer != null) {
            itemContainer.getChildren().clear();
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "LIST_AVAILABLE");
        request.addProperty("payload", "");

        SocketManager.getInstance().send(gson.toJson(request));
    }

    // =========================================================
    // 2. XỬ LÝ DỮ LIỆU JSON TỪ SERVER TRẢ VỀ
    // =========================================================
    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.get("status").getAsString();

                if ("SUCCESS".equals(status) && jsonResponse.has("payload") && jsonResponse.get("payload").isJsonArray()) {
                    JsonArray auctionArray = jsonResponse.getAsJsonArray("payload");

                    if (itemContainer != null) {
                        itemContainer.getChildren().clear();
                    }
                    for (JsonElement element : auctionArray) {
                        JsonObject dataObj = element.getAsJsonObject();

                        // 1. LẤY DỮ LIỆU CƠ BẢN (GIỮ NGUYÊN CODE CŨ AN TOÀN CỦA BẠN)
                        int id = dataObj.has("id") ? dataObj.get("id").getAsInt() : 0;
                        String name = dataObj.has("name") ? dataObj.get("name").getAsString() : "Sản phẩm chưa có tên";
                        double currentPrice = dataObj.has("currentPrice") ? dataObj.get("currentPrice").getAsDouble() : 0.0;
                        String statusString = dataObj.has("statusOfAuction") ? dataObj.get("statusOfAuction").getAsString() : "ONGOING";
                        String imageBase64 = dataObj.has("imageURL") ? dataObj.get("imageURL").getAsString() : "";
                        double bidIncrease = dataObj.has("bidIncrease") ? dataObj.get("bidIncrease").getAsDouble() : 0.0;
                        String description = dataObj.has("description") ? dataObj.get("description").getAsString() : "Không có mô tả";
                        String highestBidder = dataObj.has("highestBidderAccount") && !dataObj.get("highestBidderAccount").isJsonNull() ? dataObj.get("highestBidderAccount").getAsString() : "";

                        // 2. KHỞI TẠO SESSION
                        AuctionSession session = new AuctionSession();
                        session.setId(id);
                        session.setName(name);
                        session.setCurrentPrice(currentPrice);
                        session.setBidIncrease(bidIncrease);
                        session.setDescription(description);
                        session.setHighestBidderAccount(highestBidder);

                        try {
                            session.setStatusOfAuction(StatusOfAuction.valueOf(statusString));
                        } catch (Exception ignored) {}

                        // 3. XỬ LÝ ẢNH
                        if (imageBase64.startsWith("data:image")) {
                            imageBase64 = imageBase64.substring(imageBase64.indexOf(",") + 1);
                        }
                        session.setImageURL(imageBase64);
                        try {
                            if (dataObj.has("endTime") && !dataObj.get("endTime").isJsonNull()) {
                                String endStr = dataObj.get("endTime").getAsString();

                                // Xóa bộ Formatter cũ đi. Chuẩn hóa chuỗi (đổi dấu cách thành chữ T nếu có)
                                if (endStr.contains(" ")) endStr = endStr.replace(" ", "T");

                                // Hàm mặc định của Java xử lý cực mượt chuỗi có chữ T
                                session.setEndTime(java.time.LocalDateTime.parse(endStr));
                            }

                            if (dataObj.has("startTime") && !dataObj.get("startTime").isJsonNull()) {
                                String startStr = dataObj.get("startTime").getAsString();

                                if (startStr.contains(" ")) startStr = startStr.replace(" ", "T");

                                session.setStartTime(java.time.LocalDateTime.parse(startStr));
                            }
                        } catch (Exception e) {
                            logger.warning("⚠️ Lỗi đọc ngày tháng của sản phẩm ID " + id + ": " + e.getMessage());
                        }

                        // 5. TẠO GIAO DIỆN THẺ SẢN PHẨM VÀ HIỂN THỊ
                        VBox productCard = createProductCard(session);
                        itemContainer.getChildren().add(productCard);
                    }
                }
            } catch (Exception e) {
                if (response.contains("{")) {
                    logger.severe("❌ LỖI ĐỌC JSON TRANG CHỦ: " + response);
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================
    // 3. HÀM TẠO GIAO DIỆN THẺ SẢN PHẨM
    // =========================================================
    private VBox createProductCard(AuctionSession session) {
        VBox card = new VBox();
        card.setPrefWidth(300.0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        card.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.08)));

        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(240.0);
        imagePane.setStyle("-fx-background-color: #E8E8E8; -fx-background-radius: 20 20 0 0;");

        // --- XỬ LÝ HIỂN THỊ ẢNH ---
        String base64Data = session.getImageURL();
        if (base64Data != null && !base64Data.isEmpty()) {
            Image img = ImageUtils.base64ToImage(base64Data);
            if (img != null) {
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(300.0);
                imageView.setFitHeight(240.0);

                // Cắt ảnh bo góc cho đẹp (khớp với StackPane)
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 240);
                clip.setArcWidth(40);
                clip.setArcHeight(40);
                imageView.setClip(clip);

                imagePane.getChildren().add(imageView);
            }
        }
        // --------------------------

        Button btnHeart = new Button("🤍");
        btnHeart.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-background-radius: 50; -fx-min-width: 35; -fx-min-height: 35; -fx-cursor: hand;");
        btnHeart.setTextFill(Color.WHITE);
        btnHeart.setFont(Font.font(14));
        StackPane.setAlignment(btnHeart, Pos.TOP_RIGHT);
        StackPane.setMargin(btnHeart, new Insets(15, 15, 0, 0));
        imagePane.getChildren().add(btnHeart); // Nút tim được add vào sau nên sẽ nổi lên trên ảnh

        VBox infoBox = new VBox(15.0);
        infoBox.setPadding(new Insets(20, 25, 25, 25));

        Label lblName = new Label(session.getName());
        lblName.setFont(Font.font("System", FontWeight.BOLD, 20));
        // ĐÃ FIX: Dùng setStyle thay vì setTextFill để ép cứng màu chữ, tránh bị CSS đè
        lblName.setStyle("-fx-text-fill: #0A1128;");
        lblName.setWrapText(true);
        lblName.setPrefHeight(55.0);

        HBox priceTimeBox = new HBox(10.0);
        VBox priceCol = createInfoCol("CURRENT BID", String.format("%,.0f VNĐ", session.getCurrentPrice()), "#0A1128");

        String statusText = session.getStatusOfAuction() != null ? session.getStatusOfAuction().name() : "ONGOING";
        VBox timeCol = createInfoCol("STATUS", statusText, "#FF6B00");

        HBox.setHgrow(priceCol, Priority.ALWAYS);
        HBox.setHgrow(timeCol, Priority.ALWAYS);
        priceTimeBox.getChildren().addAll(priceCol, timeCol);

        Button btnBid = new Button("PLACE BID");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setPrefHeight(45.0);
        btnBid.setStyle("-fx-background-color: #08103F; -fx-background-radius: 10; -fx-cursor: hand;");
        btnBid.setTextFill(Color.WHITE);
        btnBid.setFont(Font.font("System", FontWeight.BOLD, 14));

        btnBid.setOnAction(event -> {
            try {
                AuctionController controller = changeSceneAndGetController(btnBid, "auction.fxml");

                if (controller != null) {
                    controller.setItemData(session); // Truyền dữ liệu sang trang mới
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        infoBox.getChildren().addAll(lblName, priceTimeBox, btnBid);
        card.getChildren().addAll(imagePane, infoBox);

        return card;
    }

    private VBox createInfoCol(String title, String value, String colorHex) {
        VBox col = new VBox(3.0);
        col.setPrefHeight(65.0);
        col.setAlignment(Pos.CENTER_LEFT);
        col.setStyle("-fx-background-color: #F4F5F7; -fx-background-radius: 12; -fx-padding: 10 15;");

        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 10));
        // ĐÃ FIX: Ép màu bằng CSS inline
        lblTitle.setStyle("-fx-text-fill: #9DA3B4;");

        Label lblValue = new Label(value);
        lblValue.setFont(Font.font("System", FontWeight.BOLD, 16));
        // ĐÃ FIX: Ép màu bằng CSS inline, nối chuỗi colorHex
        lblValue.setStyle("-fx-text-fill: " + colorHex + ";");

        col.getChildren().addAll(lblTitle, lblValue);
        return col;
    }
}