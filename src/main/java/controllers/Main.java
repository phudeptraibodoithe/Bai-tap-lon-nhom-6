package controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;



public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root= FXMLLoader.load(getClass().getResource("/start.fxml"));
        Scene scene=new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("TBOAT-Đấu giá");
        Image icon=new Image("/logo.png");
        stage.getIcons().add(icon);
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
