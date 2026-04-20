package com.example.giaodiendau;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
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
    private Scene scene;
    private Parent root;
    private String imagePath;

    private static final double CIRCLE_RADIUS = 150.0;

    @FXML private ImageView myImageView;

    /*public void switchToPostItem(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/postItem.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }

    public void switchToMenu(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/TrangChu.fxml"));
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
    }
*/
    public void loadUserAvatar(String pathFromDB) {
        Image image;
        if (pathFromDB == null) {
            image = new Image(getClass().getResource("/avtDefault.jpg").toExternalForm());
        } else {
            image = new Image(pathFromDB);
        }
        setCircularImage(image);
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String currentAvatarPath =null;
        loadUserAvatar(currentAvatarPath);
    }

    public void signout(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/logo.png")));
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất không?");
        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);
        if (alert.showAndWait().orElse(btnNo) == btnYes) {
            try {
                root = FXMLLoader.load(getClass().getResource("/start.fxml")); // Chú ý chữ P hoa/thường tùy tên file của bạn nhé
                scene = ((Node) e.getSource()).getScene();
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
                scene.setRoot(root);
            } catch (IOException event) {
                event.printStackTrace();
            }
        }
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
                root = FXMLLoader.load(getClass().getResource("/profile.fxml")); // Chú ý chữ P hoa/thường tùy tên file của bạn nhé
                scene = ((Node) e.getSource()).getScene();
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
                scene.setRoot(root);
            } catch (IOException event) {
                event.printStackTrace();
            }
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

}
