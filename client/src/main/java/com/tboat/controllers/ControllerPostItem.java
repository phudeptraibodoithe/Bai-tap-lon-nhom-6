package com.tboat.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class ControllerPostItem extends BaseController implements Initializable {
    @FXML private TextField nameItem;
    @FXML private TextArea inforItem;
    @FXML private Button submit, cancle;
    @FXML private Label thongbao;
    @FXML private ImageView myImageView;
    @FXML private Spinner<Double> priceSpinner, jumpSpinner;
    @FXML private DatePicker datePickerStart, datePickerEnd;
    @FXML private Spinner<Integer> hourStart, minuteStart, hourEnd, minuteEnd;

    private Stage stage;
    private Scene scene;
    private Parent root;
    private String imagePath = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupPriceSpinners();
        setupDateTimeLogic();
    }

    private void setupPriceSpinners() {
        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory1 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e18, 0.0, 10000.0);
        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory2 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 0.0, 0.0, 10000.0);
        DecimalFormat formatter = new DecimalFormat("#,###");

        StringConverter<Double> converter = new StringConverter<>() {
            @Override public String toString(Double v) { return v == null ? "0" : formatter.format(v) + " VNĐ"; }
            @Override public Double fromString(String s) {
                try { return s == null || s.isEmpty() ? 0.0 : Double.parseDouble(s.replaceAll("[^\\d.]", "")); }
                catch (Exception e) { return 0.0; }
            }
        };

        valueFactory1.setConverter(converter);
        valueFactory2.setConverter(converter);
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
        // --- 1. ĐỊNH DẠNG HIỂN THỊ (CÓ CHỮ GIỜ/PHÚT) ---
        StringConverter<Integer> hourConverter = createTimeConverter(" giờ");
        StringConverter<Integer> minuteConverter = createTimeConverter(" phút");

        // --- 2. KHỞI TẠO GIÁ TRỊ BAN ĐẦU ---
        LocalTime nowTime = LocalTime.now();

        // Bắt đầu: Giờ hiện tại
        configureSpinner(hourStart, 0, 23, nowTime.getHour(), hourConverter);
        configureSpinner(minuteStart, 0, 59, nowTime.getHour(), minuteConverter);

        // Kết thúc: Giờ hiện tại + 1
        configureSpinner(hourEnd, 0, 23, nowTime.plusHours(1).getHour(), hourConverter);
        configureSpinner(minuteEnd, 0, 59, nowTime.getMinute(), minuteConverter);

        // --- 3. LOGIC DATEPICKER (VÔ HIỆU HÓA NGÀY CŨ) ---
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

        // Khi ngày bắt đầu thay đổi, cập nhật giới hạn cho ngày kết thúc
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
        // Đảm bảo cập nhật giá trị khi mất focus hoặc gõ phím
        s.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) s.increment(0);
        });
    }

    public void postItem(ActionEvent e) {
        String name = nameItem.getText();
        String infor = inforItem.getText();

        // Kiểm tra cơ bản
        if (name.isEmpty() || infor.isEmpty() || imagePath == null || datePickerStart.getValue() == null || datePickerEnd.getValue() == null) {
            showError("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        // Lấy thời gian đầy đủ
        LocalDateTime startDT = datePickerStart.getValue().atTime(hourStart.getValue(), minuteStart.getValue());
        LocalDateTime endDT = datePickerEnd.getValue().atTime(hourEnd.getValue(), minuteEnd.getValue());
        LocalDateTime now = LocalDateTime.now();

        // RÀNG BUỘC 1: Bắt đầu phải cách hiện tại ít nhất 10 phút
        if (startDT.isBefore(now.plusMinutes(10))) {
            showError("Thời gian bắt đầu phải sau hiện tại ít nhất 10 phút!");
            return;
        }

        // RÀNG BUỘC 2: Kết thúc phải cách bắt đầu ít nhất 10 phút
        if (endDT.isBefore(startDT.plusMinutes(10))) {
            showError("Thời gian kết thúc phải cách thời gian bắt đầu ít nhất 10 phút!");
            return;
        }

        thongbao.setStyle("-fx-text-fill: green;");
        thongbao.setText("Sản phẩm đã được đăng thành công!");
    }

    private void showError(String msg) {
        thongbao.setStyle("-fx-text-fill: red;");
        thongbao.setText(msg);
    }

    public void uploadImage(MouseEvent event)throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        stage = (Stage) myImageView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            imagePath = selectedFile.toURI().toString();
            Image image = new Image(imagePath);
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
            try {
                root = FXMLLoader.load(getClass().getResource("/views/postItem.fxml")); // Chú ý chữ P hoa/thường tùy tên file của bạn nhé
                scene = ((Node) e.getSource()).getScene();
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/styles/Button.css").toExternalForm());
                scene.setRoot(root);
            } catch (IOException event) {
                event.printStackTrace();
            }
        }
    }
}
