package com.tboat.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.SocketHelper;
import com.tboat.socket.SocketListener;
import com.tboat.utilsclient.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class PostItemController extends BaseController implements Initializable, SocketListener {
    @FXML private TextField nameItem;
    @FXML private TextArea inforItem;
    @FXML private Label thongbao;
    @FXML private ImageView myImageView;
    @FXML private Spinner<Double> priceSpinner, jumpSpinner, buyNowSpinner;
    @FXML private DatePicker datePickerStart, datePickerEnd;
    @FXML private Spinner<Integer> hourStart, minuteStart, hourEnd, minuteEnd;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    private File selectedFile;
    private static final Logger log = Logger.getLogger(PostItemController.class.getName());

    private static final String STYLE_SUCCESS = "#2ecc71";
    private static final String STYLE_ERROR = "#e74c3c";
    private static final String STYLE_PROCESSING = "#3498db";
    private static final double STARTING_PRICE_INITIAL = 0.0;
    private static final double STARTING_PRICE_STEP = 10_000.0;
    private static final double BID_INCREASE_INITIAL = 0.0;
    private static final double BID_INCREASE_STEP = 5_000.0;
    private static final double BUY_NOW_INITIAL = 0.0;
    private static final double BUY_NOW_STEP = 10_000.0;
    private static final double MAX_BID_INCREASE_RATE = 0.5;
    private static final double NO_BUY_NOW_PRICE = 0.0;
    private static final int HOUR_MIN = 0;
    private static final int HOUR_MAX = 23;
    private static final int MINUTE_MIN = 0;
    private static final int MINUTE_MAX = 59;
    private static final int DEFAULT_END_OFFSET_HOURS = 1;
    private static final int IMAGE_CORNER_ARC_WIDTH = 20;
    private static final int IMAGE_CORNER_ARC_HEIGHT = 20;
    private static final int MIN_START_DELAY_MINUTES = 5;
    private static final int MIN_AUCTION_DURATION_MINUTES = 5;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
        if (thongbao != null) thongbao.setText("");
        typeComboBox.getItems().addAll("Điện tử", "Thời trang", "Trang sức", "Khác");
        CurrencyFormatter.setupCurrencySpinner(priceSpinner, STARTING_PRICE_INITIAL, STARTING_PRICE_STEP);
        CurrencyFormatter.setupCurrencySpinner(jumpSpinner, BID_INCREASE_INITIAL, BID_INCREASE_STEP);
        CurrencyFormatter.setupCurrencySpinner(buyNowSpinner, BUY_NOW_INITIAL, BUY_NOW_STEP);
        priceSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal > STARTING_PRICE_INITIAL) {
                double maxJump = newVal * MAX_BID_INCREASE_RATE;
                SpinnerValueFactory.DoubleSpinnerValueFactory jumpFactory =
                        (SpinnerValueFactory.DoubleSpinnerValueFactory) jumpSpinner.getValueFactory();
                jumpFactory.setMax(maxJump);
                if (jumpSpinner.getValue() > maxJump) jumpFactory.setValue(maxJump);
            }
        });

        // 2. Thiết lập logic thời gian bằng TimeUtils
        LocalTime now = LocalTime.now();
        TimeUtils.setupDatePickers(datePickerStart, datePickerEnd);
        TimeUtils.setupTimeSpinner(hourStart, HOUR_MIN, HOUR_MAX, now.getHour(), " giờ");
        TimeUtils.setupTimeSpinner(minuteStart, MINUTE_MIN, MINUTE_MAX, now.getMinute(), " phút");
        TimeUtils.setupTimeSpinner(hourEnd, HOUR_MIN, HOUR_MAX,
                now.plusHours(DEFAULT_END_OFFSET_HOURS).getHour(), " giờ");
        TimeUtils.setupTimeSpinner(minuteEnd, MINUTE_MIN, MINUTE_MAX, now.getMinute(), " phút");
    }

    public void uploadImage(MouseEvent event) {
        selectedFile = ImageUtils.chooseImageFile(myImageView.getScene().getWindow(), "Chọn ảnh sản phẩm");
        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            myImageView.setPreserveRatio(true);
            ImageUtils.applyRoundedClip(myImageView, IMAGE_CORNER_ARC_WIDTH, IMAGE_CORNER_ARC_HEIGHT);
            myImageView.setImage(image);
        }
    }

    public void postItem(ActionEvent e) {
        if (datePickerStart.getValue() == null || datePickerEnd.getValue() == null) {
            AlertUtils.showStatus(thongbao, "Vui lòng chọn ngày tháng đầy đủ!", STYLE_ERROR);
            return;
        }

        LocalDateTime startDT = datePickerStart.getValue().atTime(hourStart.getValue(), minuteStart.getValue());
        LocalDateTime endDT = datePickerEnd.getValue().atTime(hourEnd.getValue(), minuteEnd.getValue());

        if (nameItem.getText().isEmpty() || inforItem.getText().isEmpty() || selectedFile == null || typeComboBox.getValue() == null) {
            AlertUtils.showStatus(thongbao, "Vui lòng điền đầy đủ thông tin và chọn ảnh sản phẩm!", STYLE_ERROR);
            return;
        }
        if (startDT.isBefore(LocalDateTime.now().plusMinutes(MIN_START_DELAY_MINUTES))) {
            AlertUtils.showStatus(thongbao, "Thời gian bắt đầu phải sau hiện tại ít nhất 5 phút!", STYLE_ERROR);
            return;
        }
        if (endDT.isBefore(startDT.plusMinutes(MIN_AUCTION_DURATION_MINUTES))) {
            AlertUtils.showStatus(thongbao, "Thời gian kết thúc phải cách thời gian bắt đầu ít nhất 5 phút!", STYLE_ERROR);
            return;
        }
        if (buyNowSpinner.getValue() != null && buyNowSpinner.getValue() > NO_BUY_NOW_PRICE
                && buyNowSpinner.getValue() <= priceSpinner.getValue()) {
            AlertUtils.showStatus(thongbao, "Giá bán ngay phải lớn hơn giá khởi điểm!", STYLE_ERROR);
            return;
        }

        AlertUtils.showStatus(thongbao, "Đang xử lý, vui lòng đợi...", STYLE_PROCESSING);

        JsonObject payload = new JsonObject();
        payload.addProperty("name", nameItem.getText());
        payload.addProperty("description", inforItem.getText());
        payload.addProperty("type", typeComboBox.getValue());
        payload.addProperty("imageURL", ImageUtils.fileToBase64(selectedFile));
        payload.addProperty("currentPrice", priceSpinner.getValue());
        payload.addProperty("bidIncrease", jumpSpinner.getValue());
        payload.addProperty("buyNowPrice", buyNowSpinner.getValue());
        payload.addProperty("startTime", startDT.toString());
        payload.addProperty("endTime", endDT.toString());

        SocketHelper.sendRequest(ServerEvent.POST_ITEM, payload);
    }

    public void canclePost(ActionEvent e) {
        if (AlertUtils.showConfirmation("Xác nhận hủy", "Toàn bộ thông tin sẽ không được lưu lại.\nBạn có chắc muốn hủy không?")) {
            changeScene((Node) e.getSource(), "post-item.fxml");
        }
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                if (SocketHelper.getTypeEnum(response) != ServerEvent.POST_ITEM) return;

                ServerEvent status = SocketHelper.getStatusEnum(response);
                String message     = SocketHelper.getMessage(response);

                if (status == ServerEvent.SUCCESS) {
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    String id = json.has("payload") && !json.get("payload").isJsonNull()
                            ? json.get("payload").getAsString() : "N/A";
                    AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng sản phẩm thành công!");
                    changeScene(thongbao, "home.fxml");
                } else if (status == ServerEvent.ERROR || status == ServerEvent.FAILED) {
                    AlertUtils.showStatus(thongbao, message, STYLE_ERROR);
                }
            } catch (Exception e) {
                AlertUtils.showStatus(thongbao, "Lỗi đọc dữ liệu từ server.", STYLE_ERROR);
                log.severe("Lỗi parse JSON: " + response);
            }
        });
    }
}
