package com.example.giaodiendau;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class ControllerPostItem {
    @FXML private TextField nameItem;
    @FXML private TextArea inforItem;
    @FXML private Button submit,cancle;
    @FXML private Label thongbao;
    @FXML private ImageView myImageView;

    private Stage stage;
    private Scene scene;
    private Parent root;
    private String infor,name;

    public void uploadImage(MouseEvent event)throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        stage = (Stage) myImageView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            myImageView.setImage(image);
        }
    }

    public void postItem(ActionEvent e)throws IOException {
        infor=inforItem.getText();
        name=nameItem.getText();
    }

    public void canclePost(ActionEvent e)throws IOException{
        try {
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();

            Parent root = FXMLLoader.load(getClass().getResource("/postItem.fxml"));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException event) {
            event.printStackTrace();
        }
    }
}
