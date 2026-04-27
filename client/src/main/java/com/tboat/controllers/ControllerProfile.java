package com.tboat.controllers;

import com.tboat.models.User;
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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;


public class ControllerProfile extends BaseController implements Initializable {

    private Stage stage;
    private String imagePath;
    private User user;

    private static final double CIRCLE_RADIUS = 110.0;

    @FXML private ImageView myImageView;
    @FXML private Label nickname,balance,err;
    @FXML private TextArea desc;

    @Override public void initialize(URL url, ResourceBundle resourceBundle) {
        SocketManager.getInstance().setOnMessageReceived(this::handleServerResponse);
        user = UserSession.getInstance().getUser();
        imagePath =user.getAvatarURL();
        loadUserAvatar(imagePath);
        desc.setText(user.getDescription());
        nickname.setText(user.getNickname());
        balance.setText(String.format("%,.0f VNĐ", user.getBalance()));
    }

    public void updateProfile(ActionEvent e) {
        String mota = desc.getText() == null ? "" : desc.getText().trim().replace("|", " ");
        String path = imagePath == null ? "null" : imagePath;
        String message = "UPDATE_PROFILE " + mota + "|" + path;
        SocketManager.getInstance().send(message);
    }

    public void loadUserAvatar(String pathFromDB) {
        try {
            Image image;
            if (pathFromDB == null || pathFromDB.isEmpty() || pathFromDB.equals("null")) {
                image = new Image(getClass().getResource("/images/avtDefault.jpg").toExternalForm());
            } else {
                image = new Image(pathFromDB, true);
            }
            setCircularImage(image);
        } catch (Exception e) {
            setCircularImage(new Image(getClass().getResource("/images/avtDefault.jpg").toExternalForm()));
        }
    }

    public void signout(ActionEvent e) {
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
        File selectedFile = fileChooser.showOpenDialog(stage);

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

    private void handleServerResponse(String response) {
        javafx.application.Platform.runLater(() -> {
            if (response.equals("UPDATE_PROFILE_SUCCESS")) {
                user.setDescription(desc.getText());
                if (imagePath != null) user.setAvatar(imagePath);
                err.setStyle("-fx-text-fill: green;");
                err.setText("Cập nhật hồ sơ thành công!!");

            } else if (response.equals("UPDATE_PROFILE_ERROR")) {
                err.setStyle("-fx-text-fill: red;");
                err.setText("Cập nhật hồ sơ thất bại, vui lòng thử lại!");
            }
        });
    }
}
