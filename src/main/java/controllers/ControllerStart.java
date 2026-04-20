package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class ControllerStart {
    @FXML
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    public void switchToRegister(ActionEvent e)throws IOException {
        root = FXMLLoader.load(getClass().getResource("/register.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }
    @FXML
    public void switchToLogin(ActionEvent e)throws IOException {
        root = FXMLLoader.load(getClass().getResource("/login.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }
}
