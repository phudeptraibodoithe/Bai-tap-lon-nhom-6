package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.User;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import java.util.Base64;
import java.util.ResourceBundle;

public class ControllerProfile extends BaseController implements Initializable, SocketListener {

    private Stage stage;
    private String imagePath;
    private User user;
    private File selectedFile;

    private static final double CIRCLE_RADIUS = 110.0;

    @FXML private ImageView myImageView;
    @FXML private Label nickname, balance, err;
    @FXML private TextArea desc;

    private Gson gson = new Gson(); // Khởi tạo Gson

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // TẠO JSON REQUEST GỬI LÊN SERVER (Thay cho "PROFILE")
        JsonObject request = new JsonObject();
        request.addProperty("action", "PROFILE");
        SocketManager.getInstance().send(gson.toJson(request));

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
        // 1. Lấy mô tả (BÂY GIỜ KHÔNG CẦN DÙNG .replace("|", " ") NỮA VÌ ĐÃ CÓ JSON BẢO VỆ)
        String mota = desc.getText() == null ? "" : desc.getText().trim();

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

        // 3. TẠO JSON REQUEST GỬI LÊN SERVER
        JsonObject request = new JsonObject();
        request.addProperty("action", "UPDATE_PROFILE");

        // Đóng gói payload
        JsonObject payload = new JsonObject();
        payload.addProperty("description", mota);
        payload.addProperty("avatar", imageData);
        request.add("payload", payload);

        SocketManager.getInstance().send(gson.toJson(request));
    }

    public void loadUserAvatar(String pathOrBase64) {
        try {
            if (pathOrBase64 == null || pathOrBase64.isEmpty() || pathOrBase64.equals("null")) {
                loadDefaultAvatar();
                return;
            }

            Image image;
            if (!pathOrBase64.startsWith("file:/") && !pathOrBase64.startsWith("http")) {
                byte[] imageBytes = Base64.getDecoder().decode(pathOrBase64);
                image = new Image(new ByteArrayInputStream(imageBytes));
            } else {
                image = new Image(pathOrBase64, true);
            }
            setCircularImage(image);

        } catch (Exception e) {
            System.err.println("[Avatar Error]: " + e.getMessage());
            loadDefaultAvatar();
        }
    }

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
            // TẠO JSON REQUEST CHO LOGOUT
            JsonObject request = new JsonObject();
            request.addProperty("action", "LOGOUT");
            SocketManager.getInstance().send(gson.toJson(request));

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

    // ====================================================================
    // LẮNG NGHE PHẢN HỒI TỪ SERVER
    // ====================================================================
    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.get("status").getAsString();

                switch (status) {
                    case "PROFILE_INFO":
                        JsonObject data = jsonResponse.getAsJsonObject("data");

                        String nick = data.has("nickname") ? data.get("nickname").getAsString() : "";
                        double bal = data.has("balance") ? data.get("balance").getAsDouble() : 0.0;
                        String avt = data.has("avatarURL") ? data.get("avatarURL").getAsString() : "";
                        String description = data.has("description") ? data.get("description").getAsString() : "";

                        nickname.setText(nick);
                        balance.setText(String.format("%,.0f VNĐ", bal));

                        user.setNickname(nick);
                        user.setBalance(bal);
                        user.setAvatar(avt);
                        user.setDescription(description);
                        break;

                    case "UPDATE_PROFILE_SUCCESS":
                        user.setDescription(desc.getText());
                        if (selectedFile != null) {
                            user.setAvatar(selectedFile.toURI().toString());
                        }

                        err.setStyle("-fx-text-fill: green;");
                        err.setText("Cập nhật hồ sơ thành công!!");
                        break;

                    case "UPDATE_PROFILE_ERROR":
                        String errorMsg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Cập nhật thất bại";
                        err.setStyle("-fx-text-fill: red;");
                        err.setText("Lỗi: " + errorMsg);
                        break;

                    case "ERROR":
                        String msg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Lỗi hệ thống";
                        err.setStyle("-fx-text-fill: red;");
                        err.setText(msg);
                        break;
                }
            } catch (Exception e) {
                System.out.println("❌ KHÔNG THỂ ĐỌC JSON TỪ SERVER: " + response);
            }
        });
    }

    public String fileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}