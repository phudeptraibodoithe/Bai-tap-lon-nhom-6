package com.example.giaodiendau;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class ControllerLogin extends BaseController {

    @FXML private TextField signText;
    @FXML private PasswordField passText;
    @FXML private Label err;

    private Stage stage;
    private Scene scene;
    private Parent root;
    private String username;
    private String password;

    public void submit(ActionEvent event)throws IOException{
        username=signText.getText();
        password=passText.getText();
        if (username.trim().isEmpty() || password.trim().isEmpty()){
            err.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }
        if(username.trim().equals("user") && password.trim().equals("user")){
            root = FXMLLoader.load(getClass().getResource("/profile.fxml"));
            scene = ((Node) event.getSource()).getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
            scene.setRoot(root);
        }
        else {
            err.setText("Tên tài khoản và mật khẩu không khớp!!");
        }
    }
    @FXML
    public void switchToStart(MouseEvent e)throws IOException {
        root = FXMLLoader.load(getClass().getResource("/start.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }
    @FXML
    public void switchToRegister(ActionEvent e)throws IOException {
        root = FXMLLoader.load(getClass().getResource("/register.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }
}
