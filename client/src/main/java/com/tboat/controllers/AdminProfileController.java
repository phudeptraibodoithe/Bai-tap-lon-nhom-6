package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.User;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.ImageUtils;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class AdminProfileController extends BaseController implements Initializable, SocketListener {

    private static final Logger log = Logger.getLogger(AdminProfileController.class.getName());

    private Stage stage;
    private String imagePath;
    private User user;
    private File selectedFile;

    private static final double CIRCLE_RADIUS = 110.0;

    @FXML private ImageView myImageView;
    @FXML private Label nickname, balance, err, thongbao;
    @FXML private TextArea desc;

    private Gson gson = GsonUtils.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SocketManager.getInstance().subscribe(this);

        JsonObject request = new JsonObject();
        request.addProperty("action", "PROFILE");
        SocketManager.getInstance().send(gson.toJson(request));

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
        String mota = desc.getText() == null ? "" : desc.getText().trim();

        String imageData;
        if (selectedFile != null) {
            imageData = ImageUtils.fileToBase64(selectedFile);
        } else {
            imageData = UserSession.getInstance().getUser().getAvatarURL();
            if (imageData == null || imageData.isEmpty()) {
                imageData = "null";
            }
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "UPDATE_PROFILE");

        JsonObject payload = new JsonObject();
        payload.addProperty("description", mota);
        payload.addProperty("avatarURL", imageData);
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
                image = ImageUtils.base64ToImage(pathOrBase64);
            } else {
                image = new Image(pathOrBase64, true);
            }

            if (image != null) {
                setCircularImage(image);
            } else {
                loadDefaultAvatar();
            }

        } catch (Exception e) {
            log.warning("[Avatar Error]: " + e.getMessage());
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
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Quản trị không?");
        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        if (alert.showAndWait().orElse(btnNo) == btnYes) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "LOGOUT");
            SocketManager.getInstance().send(gson.toJson(request));

            UserSession.getInstance().cleanUserSession();

            Button btnSource = (Button) e.getSource();
            changeScene(btnSource, "start.fxml");
        }
    }

    public void canclePost(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận hủy thay đổi");
        alert.setHeaderText(null);
        alert.setContentText("Toàn bộ thông tin bạn vừa nhập sẽ không được lưu lại.\nBạn có chắc chắn muốn hủy thay đổi không?");
        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        if (alert.showAndWait().orElse(btnNo) == btnYes) {
            Button btnSource = (Button) e.getSource();
            changeScene(btnSource, "adminProfile.fxml");
        }
    }

    public void uploadImage(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện Admin");
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

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : jsonResponse.get("action").getAsString();
                String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "";

                if ("SUCCESS".equals(status)) {
                    if (message.contains("Thông tin") && jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
                        JsonObject data = jsonResponse.getAsJsonObject("payload");

                        String nick = data.has("nickname") ? data.get("nickname").getAsString() : "";
                        double bal = data.has("balance") ? data.get("balance").getAsDouble() : 0.0;
                        String avt = data.has("avatarURL") ? data.get("avatarURL").getAsString() : "";
                        String description = data.has("description") ? data.get("description").getAsString() : "";

                        updateUI(nick, bal, avt, description);

                        user.setNickname(nick);
                        user.setBalance(bal);
                        user.setAvatar(avt);
                        user.setDescription(description);
                    }
                    else if (message.contains("Cập nhật")) {
                        user.setDescription(desc.getText());
                        if (selectedFile != null) {
                            user.setAvatar(ImageUtils.fileToBase64(selectedFile));
                        }
                        if (err != null) {
                            err.setStyle("-fx-text-fill: green;");
                            err.setText("Cập nhật hồ sơ Admin thành công!!");
                        }
                    }
                    else if (message.contains("Đã đăng xuất")) {
                        log.info("Admin đã đăng xuất hoàn tất.");
                    }
                }
                else if ("ERROR".equals(status) || "FAILED".equals(status)) {
                    if (err != null) {
                        err.setStyle("-fx-text-fill: red;");
                        err.setText("Lỗi: " + message);
                    }
                }

            } catch (Exception e) {
                log.severe("KHÔNG THỂ ĐỌC JSON TỪ SERVER: " + response);
            }
        });
    }
}