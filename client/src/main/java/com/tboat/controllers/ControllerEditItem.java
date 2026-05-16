package com.tboat.controllers;

import com.google.gson.JsonObject;
import com.tboat.models.AuctionSession;
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

public class ControllerEditItem extends BaseController implements Initializable, SocketListener {
    @FXML private TextField nameItem;
    @FXML private TextArea inforItem;
    @FXML private Label thongbao;
    @FXML private ImageView myImageView;
    @FXML private Spinner<Double> priceSpinner, jumpSpinner;
    @FXML private DatePicker datePickerStart, datePickerEnd;
    @FXML private Spinner<Integer> hourStart, minuteStart, hourEnd, minuteEnd;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private Button btnSaveItem;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    private File selectedFile;
    private int currentSessionId;
    private String currentImageBase64 = null;

    private static final Logger log = Logger.getLogger(ControllerEditItem.class.getName());

    // --- CONSTANTS ---
    private static final String STYLE_SUCCESS = "#2ecc71";
    private static final String STYLE_ERROR = "#e74c3c";
    private static final String STYLE_PROCESSING = "#3498db";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
        if (thongbao != null) thongbao.setText("");
        typeComboBox.getItems().addAll("Điện tử", "Thời trang", "Trang sức", "Khác");

        // 👉 Áp dụng Utils xử lý tiền tệ
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

        // 👉 Áp dụng Utils xử lý thời gian
        LocalTime now = LocalTime.now();
        TimeUtils.setupDatePickers(datePickerStart, datePickerEnd);
        TimeUtils.setupTimeSpinner(hourStart, 0, 23, now.getHour(), " giờ");
        TimeUtils.setupTimeSpinner(minuteStart, 0, 59, now.getMinute(), " phút");
        TimeUtils.setupTimeSpinner(hourEnd, 0, 23, now.plusHours(1).getHour(), " giờ");
        TimeUtils.setupTimeSpinner(minuteEnd, 0, 59, now.getMinute(), " phút");
    }

    public void setEditData(AuctionSession session) {
        this.currentSessionId = session.getId();

        nameItem.setText(session.getName());
        inforItem.setText(session.getDescription());
        typeComboBox.setValue(session.getType());

        priceSpinner.getValueFactory().setValue(session.getCurrentPrice());
        jumpSpinner.getValueFactory().setValue(session.getBidIncrease());

        if (session.getStartTime() != null) {
            datePickerStart.setValue(session.getStartTime().toLocalDate());
            hourStart.getValueFactory().setValue(session.getStartTime().getHour());
            minuteStart.getValueFactory().setValue(session.getStartTime().getMinute());
        }

        if (session.getEndTime() != null) {
            datePickerEnd.setValue(session.getEndTime().toLocalDate());
            hourEnd.getValueFactory().setValue(session.getEndTime().getHour());
            minuteEnd.getValueFactory().setValue(session.getEndTime().getMinute());
        }

        if (session.getImageURL() != null && !session.getImageURL().isEmpty() && !session.getImageURL().equals("null")) {
            this.currentImageBase64 = session.getImageURL();
            Image img = ImageUtils.base64ToImage(session.getImageURL());
            if (img != null) {
                myImageView.setImage(img);
                myImageView.setPreserveRatio(true);
                ImageUtils.applyRoundedClip(myImageView, 20, 20);
            }
        }

        // Block form nếu đấu giá đã chạy
        if (session.getStartTime() != null && session.getStartTime().isBefore(LocalDateTime.now())) {
            AlertUtils.showStatus(thongbao, "Phiên đấu giá đã bắt đầu, không thể chỉnh sửa!", STYLE_ERROR);

            if (btnSaveItem != null) btnSaveItem.setDisable(true);
            nameItem.setDisable(true);
            inforItem.setDisable(true);
            typeComboBox.setDisable(true);
            priceSpinner.setDisable(true);
            jumpSpinner.setDisable(true);
            datePickerStart.setDisable(true);
            datePickerEnd.setDisable(true);
            hourStart.setDisable(true);
            minuteStart.setDisable(true);
            hourEnd.setDisable(true);
            minuteEnd.setDisable(true);
        }
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

    public void saveItem(ActionEvent e) {
        if (datePickerStart.getValue() == null || datePickerEnd.getValue() == null) {
            AlertUtils.showStatus(thongbao, "Vui lòng chọn ngày tháng đầy đủ!", STYLE_ERROR);
            return;
        }

        LocalDateTime startDT = datePickerStart.getValue().atTime(hourStart.getValue(), minuteStart.getValue());
        LocalDateTime endDT = datePickerEnd.getValue().atTime(hourEnd.getValue(), minuteEnd.getValue());

        if (nameItem.getText().isEmpty() || inforItem.getText().isEmpty() || typeComboBox.getValue() == null) {
            AlertUtils.showStatus(thongbao, "Tên, mô tả và loại sản phẩm không được để trống!", STYLE_ERROR);
            return;
        }

        AlertUtils.showStatus(thongbao, "Đang xử lý cập nhật...", STYLE_PROCESSING);

        String base64Image = (selectedFile != null) ? ImageUtils.fileToBase64(selectedFile) : currentImageBase64;

        JsonObject payload = new JsonObject();
        payload.addProperty("id", currentSessionId);
        payload.addProperty("name", nameItem.getText());
        payload.addProperty("description", inforItem.getText());
        payload.addProperty("imageURL", base64Image);
        payload.addProperty("currentPrice", priceSpinner.getValue());
        payload.addProperty("bidIncrease", jumpSpinner.getValue());
        payload.addProperty("type", typeComboBox.getValue());
        payload.addProperty("startTime", startDT.toString());
        payload.addProperty("endTime", endDT.toString());

        SocketHelper.sendRequest("EDIT_ITEM", payload);
    }

    public void deleteItem(ActionEvent e) {
        if (AlertUtils.showConfirmation("Cảnh báo Xóa", "Bạn có chắc chắn muốn xóa (hủy) sản phẩm này không?\nHành động này không thể hoàn tác!")) {
            SocketHelper.sendRequest("CANCEL_AUCTION", currentSessionId);
        }
    }

    public void cancelEdit(ActionEvent e) {
        changeScene((Node) e.getSource(), "manager.fxml");
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                String status = SocketHelper.getStatus(response);
                String message = SocketHelper.getMessage(response);

                if ("SUCCESS".equals(status)) {
                    if (message.contains("Cập nhật thông tin")) {
                        AlertUtils.showStatus(thongbao, "Cập nhật thành công!", STYLE_SUCCESS);
                        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật sản phẩm thành công!");
                        changeScene(thongbao, "manager.fxml");
                    } else if (message.contains("Đã hủy phiên")) {
                        AlertUtils.showStatus(thongbao, "Xóa thành công!", STYLE_SUCCESS);
                        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa sản phẩm thành công!");
                        changeScene(thongbao, "manager.fxml");
                    }
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