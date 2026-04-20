package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ControllerHistory extends BaseController implements Initializable {

    @FXML VBox lichsu;

    private Stage stage;
    private Scene scene;
    private Parent root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadlichsu();
    }
    public void loadlichsu() {
        lichsu.getChildren().clear();

        // Tự động gọi "nhà máy" nặn ra 4 cái lịch sử và ném vào VBox
        lichsu.getChildren().add(createHistoryRow("Laptop ThinkPad X1", "#ID-9901", "Thành công", "- 25.000.000 đ", true));
        lichsu.getChildren().add(createHistoryRow("Bàn phím cơ Keychron", "#ID-9902", "Thất bại", "0 đ", false));
        lichsu.getChildren().add(createHistoryRow("Chuột Logitech MX Master 3", "#ID-9903", "Thành công", "- 2.500.000 đ", true));
        lichsu.getChildren().add(createHistoryRow("Màn hình Dell UltraSharp", "#ID-9904", "Đang đấu giá", "???", true));
        lichsu.getChildren().add(createHistoryRow("Laptop ThinkPad X1", "#ID-9901", "Thành công", "- 25.000.000 đ", true));
        lichsu.getChildren().add(createHistoryRow("Bàn phím cơ Keychron", "#ID-9902", "Thất bại", "0 đ", false));
        lichsu.getChildren().add(createHistoryRow("Chuột Logitech MX Master 3", "#ID-9903", "Thành công", "- 2.500.000 đ", true));
        lichsu.getChildren().add(createHistoryRow("Màn hình Dell UltraSharp", "#ID-9904", "Đang đấu giá", "???", true));
        lichsu.getChildren().add(createHistoryRow("Laptop ThinkPad X1", "#ID-9901", "Thành công", "- 25.000.000 đ", true));
        lichsu.getChildren().add(createHistoryRow("Bàn phím cơ Keychron", "#ID-9902", "Thất bại", "0 đ", false));
        lichsu.getChildren().add(createHistoryRow("Chuột Logitech MX Master 3", "#ID-9903", "Thành công", "- 2.500.000 đ", true));
        lichsu.getChildren().add(createHistoryRow("Màn hình Dell UltraSharp", "#ID-9904", "Đang đấu giá", "???", true));
    }

    private HBox createHistoryRow(String name, String id, String result, String bienDong, boolean success) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(80.0);
        row.setPadding(new Insets(15, 25, 15, 25));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; " +
                "-fx-border-width: 1; -fx-border-color: #dddddd; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        Label ten = new Label(name);
        ten.setPrefWidth(250.0);
        ten.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 17px;");

        Label lblId = new Label(id);
        lblId.setAlignment(Pos.CENTER);
        lblId.setPrefWidth(120.0);
        lblId.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 15px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label kq = new Label(result);
        kq.setAlignment(Pos.CENTER);
        kq.setPrefWidth(160.0);
        String colorStatus = success ? "#27ae60" : "#e74c3c";
        kq.setStyle("-fx-text-fill: " + colorStatus + "; -fx-font-weight: bold; -fx-font-size: 16px;");


        Label lblBienDong = new Label(bienDong);
        lblBienDong.setAlignment(Pos.CENTER_RIGHT);
        lblBienDong.setPrefWidth(160.0);
        lblBienDong.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 17px;");

        row.getChildren().addAll(ten, lblId, spacer, kq, lblBienDong);

        return row;
    }
}
