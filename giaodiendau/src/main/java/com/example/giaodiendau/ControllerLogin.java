package com.example.giaodiendau;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class ControllerLogin {
    @FXML
    private TextField signText;
    private PasswordField passText;
    private Button button;

    private Stage stage;
    private Scene scene;
    private Parent root;
    private String username;
    private String password;

    public void submit(ActionEvent event)throws IOException{
        username=signText.getText();
        password=passText.getText();
    }

    public void switchToStart(MouseEvent e)throws IOException {
        root= FXMLLoader.load(getClass().getResource("/start.fxml"));
        stage=(Stage) ((Node)e.getSource()).getScene().getWindow();
        scene=new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToRegister(ActionEvent e)throws IOException {
        root= FXMLLoader.load(getClass().getResource("/register.fxml"));
        stage=(Stage) ((Node)e.getSource()).getScene().getWindow();
        scene=new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
