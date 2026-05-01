package com.tboat.controllers;

import com.tboat.models.User;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utilsclient.UserSession;
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
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Base64;
import java.util.ResourceBundle;


public class ControllerProfile extends BaseController implements Initializable, SocketListener {

    private Stage stage;
    private String imagePath;
    private User user;
    private File selectedFile;

    private static final double CIRCLE_RADIUS = 110.0;

    @FXML private ImageView myImageView;
    @FXML private Label nickname,balance,err;
    @FXML private TextArea desc;

    @Override public void initialize(URL url, ResourceBundle resourceBundle) {
        SocketManager.getInstance().send("PROFILE");

        // Hiển thị dữ liệu tạm thời từ Session trong khi đợi Server
        user = UserSession.getInstance().getUser();
        if (user != null) {
            updateUI(user.getNickname(), user.getBalance(), user.getAvatarURL(), user.getDescription());
        }
    }

    private void updateUI(String nick, double bal, String avt, String description) {
        nickname.setText(nick);
        balance.setText(String.format("%,.0f VNĐ", bal));
        desc.setText(description == null ? "" : description);
        loadUserAvatar(avt);
    }

    public void updateProfile(ActionEvent e) {
        // 1. Lấy mô tả, xóa bỏ ký tự gạch đứng để tránh lỗi split
        String mota = desc.getText() == null ? "" : desc.getText().trim().replace("|", " ");

        // 2. Xử lý dữ liệu ảnh
        String imageData;
        if (selectedFile != null) {
            imageData = fileToBase64(selectedFile);
        } else {
            imageData = UserSession.getInstance().getUser().getAvatarURL();
            if (imageData == null || imageData.isEmpty()) {
                imageData = "null";
            }
        }

        // 3. Gửi lệnh
        String message = "UPDATE_PROFILE|" + mota + "|" + imageData;
        SocketManager.getInstance().send(message);
    }

    public void loadUserAvatar(String pathOrBase64) {
        try {
            // 1. Kiểm tra trường hợp dữ liệu trống hoặc null
            if (pathOrBase64 == null || pathOrBase64.isEmpty() || pathOrBase64.equals("null")) {
                loadDefaultAvatar();
                return;
            }

            Image image;

            // 2. Kiểm tra xem đây là chuỗi Base64 hay là Đường dẫn (Path/URL)
            // Dấu hiệu nhận biết: Path thường bắt đầu bằng "file:/" hoặc "http"
            if (!pathOrBase64.startsWith("file:/") && !pathOrBase64.startsWith("http")) {
                // Đây là chuỗi Base64 -> Giải mã sang mảng byte
                byte[] imageBytes = Base64.getDecoder().decode(pathOrBase64);
                // Chuyển mảng byte thành luồng đầu vào (InputStream) để Image có thể đọc
                image = new Image(new ByteArrayInputStream(imageBytes));
            } else {
                // Đây là đường dẫn file hoặc URL cũ
                image = new Image(pathOrBase64, true);
            }

            setCircularImage(image);

        } catch (Exception e) {
            // Nếu có bất kỳ lỗi nào (giải mã lỗi, file không tồn tại), hiện ảnh mặc định
            System.err.println("[Avatar Error]: " + e.getMessage());
            loadDefaultAvatar();
        }
    }

    // Hàm phụ để code sạch hơn
    private void loadDefaultAvatar() {
        String defaultPath = getClass().getResource("/images/avtDefault.jpg").toExternalForm();
        setCircularImage(new Image(defaultPath));
    }

    public void logout(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/Button.css").toExternalForm());
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất không?");
        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);
        if (alert.showAndWait().orElse(btnNo) == btnYes) {
            SocketManager.getInstance().send("LOGOUT");
            UserSession.getInstance().cleanUserSession();
            changeScene(myImageView,"start.fxml");
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
            changeScene(myImageView,"profile.fxml");
        }
    }

    public void uploadImage(MouseEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        stage = (Stage) myImageView.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            imagePath = selectedFile.toURI().toString();
            Image image = new Image(imagePath);
            setCircularImage(image);
        }
    }

    private void setCircularImage(Image image) {
        myImageView.setImage(image);
        myImageView.setFitWidth(CIRCLE_RADIUS * 2);
        myImageView.setFitHeight(CIRCLE_RADIUS * 2);
        myImageView.setPreserveRatio(false);
        myImageView.setSmooth(true);

        Circle clipCircle = new Circle(CIRCLE_RADIUS, CIRCLE_RADIUS, CIRCLE_RADIUS);
        myImageView.setClip(clipCircle);
    }

    public void handleServerResponse(String response) {
        javafx.application.Platform.runLater(() -> {
            String[] parts = response.split("\\|", -1);

            switch (parts[0]) {
                case "PROFILE_INFO":
                    // Server gửi: PROFILE_INFO | accountName | nickname | balance | avatarURL | desc
                    if (parts.length >= 6) {
                        nickname.setText(parts[2]);
                        balance.setText(String.format("%,.0f VNĐ", Double.parseDouble(parts[3])));
                        user.setNickname(parts[2]);
                        user.setBalance(Double.parseDouble(parts[3]));
                        user.setAvatar(parts[4]);
                        user.setDescription(parts[5]);
                    }
                    break;

                case "UPDATE_PROFILE_SUCCESS":
                    // Cập nhật thông tin cục bộ ngay lập tức
                    user.setDescription(desc.getText());
                    if (selectedFile != null) {
                        user.setAvatar(selectedFile.toURI().toString());
                    }

                    err.setStyle("-fx-text-fill: green;");
                    err.setText("Cập nhật hồ sơ thành công!!");
                    break;

                case "UPDATE_PROFILE_ERROR":
                    err.setStyle("-fx-text-fill: red;");
                    err.setText("Lỗi: " + (parts.length > 1 ? parts[1] : "Cập nhật thất bại"));
                    break;

                case "ERROR":
                    err.setStyle("-fx-text-fill: red;");
                    err.setText(parts[1]);
                    break;

                default:
                    System.out.println("Lệnh không xác định: " + parts[0]);
                    break;
            }
        });
    }


    public String fileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            // Sử dụng getEncoder() - mặc định tạo 1 dòng duy nhất, không có line breaks
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
