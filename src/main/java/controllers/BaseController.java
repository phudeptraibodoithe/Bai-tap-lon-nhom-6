package controllers;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public abstract class BaseController {

    private Stage stage;
    private Scene scene;
    private Parent root;

    private void changeScene(ActionEvent event, String fxmlFileName) {
        try {
            System.out.println("Đang thử load trang: /" + fxmlFileName);

            // 1. Load giao diện FXML
            URL fxmlUrl = getClass().getResource("/" + fxmlFileName);
            if (fxmlUrl == null) {
                System.err.println("LỖI NGHIÊM TRỌNG: Không tìm thấy file /" + fxmlFileName);
                return; // Dừng luôn nếu không thấy file FXML
            }
            root = FXMLLoader.load(fxmlUrl);
            scene = ((Node) event.getSource()).getScene();

            // 2. Clear CSS cũ
            scene.getStylesheets().clear();

            // 3. Load CSS mới (Có kiểm tra Null để chống lỗi)
            URL cssUrl = getClass().getResource("/Button.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("CẢNH BÁO: Không tìm thấy file /Button.css (bỏ qua bước load CSS)");
            }

            // 4. Hiển thị giao diện mới
            scene.setRoot(root);
            System.out.println("=> Chuyển trang thành công!");

        } catch (Exception e) {
            System.err.println("Lỗi ngoại lệ khi chuyển sang trang: " + fxmlFileName);
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

    @FXML
    public void switchToMyItems(ActionEvent event) {
        changeScene(event, "myitems.fxml");
    }
}