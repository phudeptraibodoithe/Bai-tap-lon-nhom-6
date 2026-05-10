package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.CurrencyFormatter;
import com.tboat.utilsclient.CurrencyStringConverter;
import com.tboat.utilsclient.HeaderUtils;
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
import java.time.LocalDate;
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

    // ĐÃ THÊM: Khai báo 2 biến UI cho Header
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    private Stage stage;
    private File selectedFile;
    private final Gson gson = GsonUtils.getInstance();
    private static final Logger log = Logger.getLogger(ControllerPostItem.class.getName());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SocketManager.getInstance().subscribe(this);

        setupPriceSpinners();
        setupDateTimeLogic();
        typeComboBox.getItems().addAll("Điện tử", "Thời trang", "Trang sức", "Khác");

        // ĐÃ THÊM: Gọi class dùng chung để hiển thị Avatar và tên User
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
    }

    private void setupPriceSpinners() {
        SpinnerValueFactory.DoubleSpinnerValueFactory priceFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e18, 0.0, 10000.0);

        SpinnerValueFactory.DoubleSpinnerValueFactory jumpFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e18, 0.0, 5000.0);

        CurrencyStringConverter currencyConverter = new CurrencyStringConverter();
        priceFactory.setConverter(currencyConverter);
        jumpFactory.setConverter(currencyConverter);

        priceSpinner.setValueFactory(priceFactory);
        jumpSpinner.setValueFactory(jumpFactory);

        priceSpinner.setEditable(true);
        jumpSpinner.setEditable(true);

        addRealTimeFormatter(priceSpinner);
        addRealTimeFormatter(jumpSpinner);
        commitEditorText(priceSpinner);
        commitEditorText(jumpSpinner);

        priceSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                double maxJump = newVal * 0.5;
                jumpFactory.setMax(maxJump > 0 ? maxJump : 1e18);
                if (jumpSpinner.getValue() > maxJump && newVal > 0) {
                    jumpFactory.setValue(maxJump);
                }
            }
        });
    }

    private void addRealTimeFormatter(Spinner<Double> spinner) {
        TextField editor = spinner.getEditor();
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            String digits = newValue.replaceAll("[^\\d]", "");
            if (digits.isEmpty()) {
                editor.setText("");
                return;
            }

            try {
                double value = Double.parseDouble(digits);
                String formatted = CurrencyFormatter.formatDisplay(value);

                Platform.runLater(() -> {
                    int currentCaret = editor.getCaretPosition();
                    int oldLength = editor.getText().length();

                    editor.setText(formatted);

                    int newLength = formatted.length();
                    int selection = currentCaret + (newLength - oldLength);
                    editor.positionCaret(Math.max(0, Math.min(selection, newLength - 4)));
                });
            } catch (NumberFormatException e) {
                editor.setText(oldValue);
            }
        });
    }

    private <T> void commitEditorText(Spinner<T> spinner) {
        spinner.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = spinner.getEditor().getText();
                StringConverter<T> converter = spinner.getValueFactory().getConverter();
                if (converter != null) {
                    T value = converter.fromString(text);
                    spinner.getValueFactory().setValue(value);
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

        LocalDate today = LocalDate.now();
        datePickerStart.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(today)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee;");
                }
            }
        });

        datePickerStart.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                datePickerEnd.setDayCellFactory(p -> new DateCell() {
                    @Override public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item.isBefore(newVal)) {
                            setDisable(true);
                            setStyle("-fx-background-color: #eeeeee;");
                        }
                    }
                });
                if (datePickerEnd.getValue() != null && datePickerEnd.getValue().isBefore(newVal)) {
                    datePickerEnd.setValue(null);
                }
            }
        });
    }

    private StringConverter<Integer> createTimeConverter(String suffix) {
        return new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return (value == null) ? "00" + suffix : String.format("%02d%s", value, suffix);
            }

            @Override
            public Integer fromString(String string) {
                try {
                    if (string == null || string.isEmpty()) return 0;
                    return Integer.parseInt(string.replace(suffix, "").trim());
                } catch (Exception e) {
                    return 0;
                }
            }
        };
    }

    private void configureSpinner(Spinner<Integer> s, int min, int max, int init, StringConverter<Integer> conv) {
        s.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, init));
        s.getValueFactory().setConverter(conv);
        s.setEditable(true);
        s.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) s.increment(0);
        });
    }

    public void postItem(ActionEvent e) {
        String name = nameItem.getText();
        String infor = inforItem.getText();
        String type = typeComboBox.getValue();

        if(datePickerStart.getValue() == null || datePickerEnd.getValue() == null) {
            showError("Vui lòng chọn ngày tháng đầy đủ!");
            return;
        }

        LocalDateTime startDT = datePickerStart.getValue().atTime(hourStart.getValue(), minuteStart.getValue());
        LocalDateTime endDT = datePickerEnd.getValue().atTime(hourEnd.getValue(), minuteEnd.getValue());
        LocalDateTime now = LocalDateTime.now();

        if (name.isEmpty() || infor.isEmpty() || selectedFile == null || type == null) {
            showError("Vui lòng điền đầy đủ thông tin và chọn ảnh sản phẩm!");
            return;
        }

        if (startDT.isBefore(now.plusMinutes(5))) {
            showError("Thời gian bắt đầu phải sau hiện tại ít nhất 5 phút!");
            return;
        }

        if (endDT.isBefore(startDT.plusMinutes(5))) {
            showError("Thời gian kết thúc phải cách thời gian bắt đầu ít nhất 5 phút!");
            return;
        }

        double startPrice = priceSpinner.getValue();
        double bidInc = jumpSpinner.getValue();

        String base64Image = ImageUtils.fileToBase64(selectedFile);

        JsonObject request = new JsonObject();
        request.addProperty("action", "POST_ITEM");

        JsonObject payload = new JsonObject();
        payload.addProperty("name", name);
        payload.addProperty("description", infor);
        payload.addProperty("type", type);
        payload.addProperty("imageURL", base64Image);
        payload.addProperty("currentPrice", startPrice);
        payload.addProperty("bidIncrease", bidInc);
        payload.addProperty("startTime", startDT.toString());
        payload.addProperty("endTime", endDT.toString());

        request.add("payload", payload);
        SocketManager.getInstance().send(gson.toJson(request));

        thongbao.setStyle("-fx-text-fill: blue;");
        thongbao.setText("Đang xử lý, vui lòng đợi...");
    }

    private void showError(String msg) {
        thongbao.setStyle("-fx-text-fill: red;");
        thongbao.setText(msg);
    }

    public void uploadImage(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        stage = (Stage) myImageView.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            myImageView.setPreserveRatio(true);
            double width = myImageView.getFitWidth();
            double height = myImageView.getFitHeight();
            Rectangle clip = new Rectangle(width, height);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            myImageView.setClip(clip);
            myImageView.setImage(image);
        }
    }

    public void canclePost(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận hủy thay đổi");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/Button.css").toExternalForm());
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        alert.setHeaderText(null);
        alert.setContentText("Toàn bộ thông tin bạn vừa nhập sẽ không được lưu lại.\nBạn có chắc chắn muốn hủy thay đổi không?");
        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        if (alert.showAndWait().orElse(btnNo) == btnYes) {
            changeScene((Node) e.getSource(), "postItem.fxml");
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
                    if (message.contains("Đăng sản phẩm")) {
                        thongbao.setStyle("-fx-text-fill: green;");
                        String id = jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull() ? jsonResponse.get("payload").getAsString() : "N/A";
                        thongbao.setText("Đăng bán thành công! ID Phiên: " + id);

                        changeScene(thongbao, "trangchu.fxml");
                    }
                } else if ("ERROR".equals(status) || "FAILED".equals(status)) {
                    showError(message);
                }
            } catch (Exception e) {
                showError("Lỗi đọc dữ liệu từ server.");
                log.severe("KHÔNG THỂ ĐỌC JSON POST ITEM: " + response);
            }
        });
    }
}