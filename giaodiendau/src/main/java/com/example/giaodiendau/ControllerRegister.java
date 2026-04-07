package com.example.giaodiendau;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class ControllerRegister {

    @FXML private TextField signText;
    @FXML private TextField emailText;
    @FXML private PasswordField passText;
    @FXML private PasswordField repassText;
    @FXML private TextField phoneText;
    @FXML private Label err;
    @FXML private TextField usernameText;


    private Stage stage;
    private Scene scene;
    private Parent root;
    private String username,phone,email,password,account;

    public void submit(ActionEvent event)throws IOException{
        err.setStyle("-fx-text-fill: red;");
        username=usernameText.getText();
        account=signText.getText();
        password=passText.getText();
        email=emailText.getText();
        phone=phoneText.getText();
        if (username.trim().isEmpty() || password.trim().isEmpty() || repassText.getText().trim().isEmpty() ||
                email.trim().isEmpty() || phone.trim().isEmpty() || account.trim().isEmpty()) {
            err.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }
        if(!email.endsWith("@gmail.com")){
            err.setText("Email không hợp lệ");
            return;
        }
        if(!password.equals(repassText.getText())){
            err.setText("Mật khẩu phải trùng nhau");
            return;
        }
        if (!phone.matches("\\d{10}")) {
            err.setText("Số điện thoại không hợp lệ");
            return;
        }
        err.setStyle("-fx-text-fill: green;");
        err.setText("Đăng ký thành công!");

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> {
            try {
                switchToLogin(event);
            } catch (Exception ex) {
                err.setText("Vui lòng đăng nhập lại");
            }
        });
        delay.play();
    }

    public void switchToLogin(ActionEvent e)throws IOException {
        root= FXMLLoader.load(getClass().getResource("/login.fxml"));
        stage=(Stage) ((Node)e.getSource()).getScene().getWindow();
        scene=new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public void switchToStart(MouseEvent e)throws IOException {
        root= FXMLLoader.load(getClass().getResource("/start.fxml"));
        stage=(Stage) ((Node)e.getSource()).getScene().getWindow();
        scene=new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
