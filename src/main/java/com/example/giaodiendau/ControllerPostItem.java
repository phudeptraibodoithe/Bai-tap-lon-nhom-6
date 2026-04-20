package com.example.giaodiendau;

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
import java.util.ResourceBundle;

public class ControllerPostItem extends BaseController implements Initializable {
    @FXML private TextField nameItem;
    @FXML private TextArea inforItem;
    @FXML private Button submit,cancle;
    @FXML private Label thongbao;
    @FXML private ImageView myImageView;
    @FXML private Spinner<Double> priceSpinner,jumpSpinner;
    @FXML private DatePicker datePickerStart,datePickerEnd;

    private Stage stage;
    private Scene scene;
    private Parent root;
    private String infor,name;
    private LocalDate dateStart,dateEnd;
    private double price,jump;
    private String imagePath=null;

    /*public void switchToMenu(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/TrangChu.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }

    public void switchToProfile(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/profile.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }

    public void switchToHistory(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/history.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }*/


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

    public void postItem(ActionEvent e)throws IOException {
        infor=inforItem.getText();
        name=nameItem.getText();
        price=priceSpinner.getValue();
        jump=jumpSpinner.getValue();
        dateEnd=datePickerEnd.getValue();
        dateStart=datePickerStart.getValue();
        if (infor.trim().isEmpty() || name.trim().isEmpty() || price<=0 ||
                jump<=0 || dateStart==null || dateEnd==null || imagePath==null) {
            thongbao.setStyle("-fx-text-fill: red;");
            thongbao.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }


        thongbao.setStyle("-fx-text-fill: green;");
        thongbao.setText("Sản phẩm đã được đăng và đang chờ duyệt!");

    }

    public void canclePost(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận hủy thay đổi");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/logo.png")));
        alert.setHeaderText(null);
        alert.setContentText("Toàn bộ thông tin bạn vừa nhập sẽ không được lưu lại.\nBạn có chắc chắn muốn hủy thay đổi không?");
        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);
        if (alert.showAndWait().orElse(btnNo) == btnYes) {
            try {
                root = FXMLLoader.load(getClass().getResource("/postItem.fxml")); // Chú ý chữ P hoa/thường tùy tên file của bạn nhé
                scene = ((Node) e.getSource()).getScene();
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
                scene.setRoot(root);
            } catch (IOException event) {
                event.printStackTrace();
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SpinnerValueFactory<Double> valueFactory1 =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e18, 0.0, 10000.0);
        SpinnerValueFactory<Double> valueFactory2 =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e18, 0.0, 10000.0);
        DecimalFormat formatter = new DecimalFormat("#,###");
        valueFactory1.setConverter(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                if (value == null) return "0";
                return formatter.format(value) + " VNĐ";
            }

            @Override
            public Double fromString(String string) {
                try {
                    if (string == null || string.trim().isEmpty()) return 0.0;
                    String cleanString = string.replaceAll("[^\\d.]", "");
                    return Double.parseDouble(cleanString);
                } catch (Exception e) {
                    return 0.0;
                }
            }
        });
        valueFactory2.setConverter(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                if (value == null) return "0";
                return formatter.format(value) + " VNĐ";
            }

            @Override
            public Double fromString(String string) {
                try {
                    if (string == null || string.trim().isEmpty()) return 0.0;
                    String cleanString = string.replaceAll("[^\\d.]", "");
                    return Double.parseDouble(cleanString);
                } catch (Exception e) {
                    return 0.0;
                }
            }
        });
        priceSpinner.setValueFactory(valueFactory1);
        jumpSpinner.setValueFactory(valueFactory2);

        LocalDate today = LocalDate.now();

        // Vô hiệu hóa các ngày trước ngày hôm nay cho datePickerStart
        datePickerStart.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);

                // Nếu ngày trong lịch (item) trước ngày hôm nay (today)
                if (item.isBefore(today)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee;"); // Làm xám các ngày cũ
                }
            }
        });

        datePickerStart.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // Cập nhật luật cho datePickerEnd khi datePickerStart thay đổi
                datePickerEnd.setDayCellFactory(picker -> new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item.isBefore(newValue)) {
                            setDisable(true);
                            setStyle("-fx-background-color: #eeeeee;");
                        }
                    }
                });

                if (datePickerEnd.getValue() != null && datePickerEnd.getValue().isBefore(newValue)) {
                    datePickerEnd.setValue(null);
                }
            }
        });
    }

}
