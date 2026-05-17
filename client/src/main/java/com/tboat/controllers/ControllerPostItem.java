package com.tboat.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class ControllerPostItem extends BaseController implements Initializable, SocketListener {
    @FXML private TextField nameItem;
    @FXML private TextArea inforItem;
    @FXML private Label thongbao;
    @FXML private ImageView myImageView;
    @FXML private Spinner<Double> priceSpinner, jumpSpinner;
    @FXML private DatePicker datePickerStart, datePickerEnd;
    @FXML private Spinner<Integer> hourStart, minuteStart, hourEnd, minuteEnd;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    private File selectedFile;
    private static final Logger log = Logger.getLogger(ControllerPostItem.class.getName());

    private static final String STYLE_SUCCESS = "#2ecc71";
    private static final String STYLE_ERROR = "#e74c3c";
    private static final String STYLE_PROCESSING = "#3498db";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
        if (thongbao != null) thongbao.setText("");
        typeComboBox.getItems().addAll("Điện tử", "Thời trang", "Trang sức", "Khác");
        CurrencyFormatter.setupCurrencySpinner(priceSpinner, 0.0, 10000.0);
        CurrencyFormatter.setupCurrencySpinner(jumpSpinner, 0.0, 5000.0);
        priceSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal > 0) {
                double maxJump = newVal * 0.5;
                SpinnerValueFactory.DoubleSpinnerValueFactory jumpFactory =
                        (SpinnerValueFactory.DoubleSpinnerValueFactory) jumpSpinner.getValueFactory();
                jumpFactory.setMax(maxJump);
                if (jumpSpinner.getValue() > maxJump) jumpFactory.setValue(maxJump);
            }
        });

        // 2. Setup Logic Thời gian bằng TimeUtils
        LocalTime now = LocalTime.now();
        TimeUtils.setupDatePickers(datePickerStart, datePickerEnd);
        TimeUtils.setupTimeSpinner(hourStart, 0, 23, now.getHour(), " giờ");
        TimeUtils.setupTimeSpinner(minuteStart, 0, 59, now.getMinute(), " phút");
        TimeUtils.setupTimeSpinner(hourEnd, 0, 23, now.plusHours(1).getHour(), " giờ");
        TimeUtils.setupTimeSpinner(minuteEnd, 0, 59, now.getMinute(), " phút");
    }

    public void uploadImage(MouseEvent event) {
        selectedFile = ImageUtils.chooseImageFile(myImageView.getScene().getWindow(), "Chọn ảnh sản phẩm");
        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            myImageView.setPreserveRatio(true);
            ImageUtils.applyRoundedClip(myImageView, 20, 20);
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
        if (startDT.isBefore(LocalDateTime.now().plusMinutes(5))) {
            AlertUtils.showStatus(thongbao, "Thời gian bắt đầu phải sau hiện tại ít nhất 5 phút!", STYLE_ERROR);
            return;
        }
        if (endDT.isBefore(startDT.plusMinutes(5))) {
            AlertUtils.showStatus(thongbao, "Thời gian kết thúc phải cách thời gian bắt đầu ít nhất 5 phút!", STYLE_ERROR);
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
        payload.addProperty("startTime", startDT.toString());
        payload.addProperty("endTime", endDT.toString());

        SocketHelper.sendRequest("POST_ITEM", payload);
    }

    public void canclePost(ActionEvent e) {
        if (AlertUtils.showConfirmation("Xác nhận hủy", "Toàn bộ thông tin sẽ không được lưu lại.\nBạn có chắc muốn hủy không?")) {
            changeScene((Node) e.getSource(), "postItem.fxml");
        }
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                if (!"POST_ITEM".equals(SocketHelper.getType(response))) return;

                String status  = SocketHelper.getStatus(response);
                String message = SocketHelper.getMessage(response);

                if ("SUCCESS".equals(status)) {
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    String id = json.has("payload") && !json.get("payload").isJsonNull()
                            ? json.get("payload").getAsString() : "N/A";
                    AlertUtils.showStatus(thongbao, "Đăng bán thành công! ID Phiên: " + id, STYLE_SUCCESS);
                    AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng sản phẩm thành công!");
                    changeScene(thongbao, "trangchu.fxml");
                } else if ("ERROR".equals(status) || "FAILED".equals(status)) {
                    AlertUtils.showStatus(thongbao, message, STYLE_ERROR);
                }
            } catch (Exception e) {
                AlertUtils.showStatus(thongbao, "Lỗi đọc dữ liệu từ server.", STYLE_ERROR);
                log.severe("Lỗi parse JSON: " + response);
            }
        });
    }
}