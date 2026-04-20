package com.example.giaodiendau;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public abstract class BaseController {

    private Stage stage;
    private Scene scene;
    private Parent root;

    private void changeScene(ActionEvent event, String fxmlFileName) {
        try {
            root = FXMLLoader.load(getClass().getResource("/" + fxmlFileName));
            scene = ((Node) event.getSource()).getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
            scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void switchToMenu(ActionEvent event) {
        changeScene(event, "TrangChu.fxml");
    }

    @FXML
    public void switchToHistory(ActionEvent event) {
        changeScene(event, "history.fxml"); // Đổi tên file fxml cho khớp với thực tế
    }

    @FXML
    public void switchToPostItem(ActionEvent event) {
        changeScene(event, "postItem.fxml");
    }

    @FXML
    public void switchToWallet(ActionEvent event) {
        changeScene(event, "NapRut.fxml");
    }

    @FXML
    public void switchToProfile(ActionEvent event) {
        changeScene(event, "profile.fxml");
    }
}