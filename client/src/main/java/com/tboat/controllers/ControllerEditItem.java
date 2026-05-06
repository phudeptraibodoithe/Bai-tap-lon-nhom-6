package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.AuctionSession;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.CurrencyStringConverter;
import com.tboat.utilsclient.ImageUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.net.URL;
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

    private Stage stage;
    private File selectedFile;
    private int currentSessionId;
    private String currentImageBase64 = null; // Lưu ảnh cũ nếu người dùng không chọn ảnh mới

    private Gson gson = GsonUtils.getInstance();
    private static final Logger log = Logger.getLogger(ControllerEditItem.class.getName());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SocketManager.getInstance().subscribe(this);

        setupPriceSpinners();
        setupDateTimeLogic();
        typeComboBox.getItems().addAll("Điện tử", "Thời trang", "Trang sức", "Khác");
    }

    // ====================================================================
    // HÀM NÀY ĐƯỢC GỌI TỪ MANAGERCONTROLLER ĐỂ ĐỔ DỮ LIỆU VÀO FORM
    // ====================================================================
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
                Rectangle clip = new Rectangle(myImageView.getFitWidth(), myImageView.getFitHeight());
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                myImageView.setClip(clip);
            }
        }
    }

    // ====================================================================
    // HÀM LƯU THAY ĐỔI SẢN PHẨM (EDIT_ITEM)
    // ====================================================================
    public void saveItem(ActionEvent e) {
        String name = nameItem.getText();
        String infor = inforItem.getText();

        if (name.isEmpty() || infor.isEmpty()) {
            showError("Tên và mô tả không được để trống!");
            return;
        }

        String base64Image = currentImageBase64;
        if (selectedFile != null) {
            base64Image = ImageUtils.fileToBase64(selectedFile);
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "EDIT_ITEM"); // Gọi case EDIT_ITEM ở ClientHandler

        JsonObject payload = new JsonObject();
        payload.addProperty("id", currentSessionId);
        payload.addProperty("name", name);
        payload.addProperty("description", infor);
        payload.addProperty("imageURL", base64Image);
        payload.addProperty("currentPrice", priceSpinner.getValue());
        payload.addProperty("bidIncrease", jumpSpinner.getValue());

        request.add("payload", payload);
        SocketManager.getInstance().send(gson.toJson(request));

        thongbao.setStyle("-fx-text-fill: blue;");
        thongbao.setText("Đang xử lý cập nhật...");
    }

    // ====================================================================
    // HÀM XÓA/HỦY SẢN PHẨM (CANCEL_AUCTION)
    // ====================================================================
    public void deleteItem(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cảnh báo Xóa");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/Button.css").toExternalForm());
        Stage alertStage = (Stage) dialogPane.getScene().getWindow();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn xóa (hủy) sản phẩm này không?\nHành động này không thể hoàn tác!");
        ButtonType btnYes = new ButtonType("Có, Xóa ngay", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);
        if (alert.showAndWait().orElse(btnNo) == btnYes) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "CANCEL_AUCTION");
            request.addProperty("payload", currentSessionId);
            SocketManager.getInstance().send(gson.toJson(request));
        }
    }

    // ====================================================================
    // HÀM HỦY THAY ĐỔI VÀ QUAY VỀ
    // ====================================================================
    public void cancelEdit(ActionEvent e) {
        changeScene((Node) e.getSource(), "manager.fxml");
    }

    // ====================================================================
    // CÁC HÀM TIỆN ÍCH (Giữ nguyên từ PostItem)
    // ====================================================================
    private void setupPriceSpinners() {
        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory1 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e18, 0.0, 10000.0);
        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory2 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 0.0, 0.0, 10000.0);

        CurrencyStringConverter currencyConverter = new CurrencyStringConverter();
        valueFactory1.setConverter(currencyConverter);
        valueFactory2.setConverter(currencyConverter);

        priceSpinner.setValueFactory(valueFactory1);
        jumpSpinner.setValueFactory(valueFactory2);
        priceSpinner.setEditable(true);
        jumpSpinner.setEditable(true);

        priceSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                double maxJump = newVal * 0.5;
                valueFactory2.setMax(maxJump);
                if (jumpSpinner.getValue() > maxJump) {
                    valueFactory2.setValue(maxJump);
                }
            }
        });
    }

    private void setupDateTimeLogic() {
        StringConverter<Integer> hourConverter = createTimeConverter(" giờ");
        StringConverter<Integer> minuteConverter = createTimeConverter(" phút");
        LocalTime nowTime = LocalTime.now();

        configureSpinner(hourStart, 0, 23, nowTime.getHour(), hourConverter);
        configureSpinner(minuteStart, 0, 59, nowTime.getMinute(), minuteConverter);
        configureSpinner(hourEnd, 0, 23, nowTime.plusHours(1).getHour(), hourConverter);
        configureSpinner(minuteEnd, 0, 59, nowTime.getMinute(), minuteConverter);
    }

    private StringConverter<Integer> createTimeConverter(String suffix) {
        return new StringConverter<>() {
            @Override public String toString(Integer value) { return (value == null) ? "00" + suffix : String.format("%02d%s", value, suffix); }
            @Override public Integer fromString(String string) {
                try {
                    if (string == null || string.isEmpty()) return 0;
                    return Integer.parseInt(string.replace(suffix, "").trim());
                } catch (Exception e) { return 0; }
            }
        };
    }

    private void configureSpinner(Spinner<Integer> s, int min, int max, int init, StringConverter<Integer> conv) {
        s.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, init));
        s.getValueFactory().setConverter(conv);
        s.setEditable(true);
        s.getEditor().focusedProperty().addListener((obs, oldV, newV) -> { if (!newV) s.increment(0); });
    }

    private void showError(String msg) {
        thongbao.setStyle("-fx-text-fill: red;");
        thongbao.setText(msg);
    }

    public void uploadImage(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        stage = (Stage) myImageView.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            myImageView.setPreserveRatio(true);
            Rectangle clip = new Rectangle(myImageView.getFitWidth(), myImageView.getFitHeight());
            clip.setArcWidth(20); clip.setArcHeight(20);
            myImageView.setClip(clip);
            myImageView.setImage(image);
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
                    if (message.contains("Cập nhật thông tin")) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã cập nhật sản phẩm thành công!");
                        alert.showAndWait();
                        changeScene(thongbao, "manager.fxml");
                    } else if (message.contains("Đã hủy phiên")) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã xóa sản phẩm thành công!");
                        alert.showAndWait();
                        changeScene(thongbao, "manager.fxml");
                    }
                } else if ("ERROR".equals(status) || "FAILED".equals(status)) {
                    showError(message);
                }
            } catch (Exception e) {
                showError("Lỗi đọc dữ liệu từ server.");
            }
        });
    }
}