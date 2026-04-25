package com.tboat;

import com.tboat.socket.SocketManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image; // Cần thêm dòng import này
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        try {
            // Kết nối thẳng tới IP mặc định (ví dụ 127.0.0.1 hoặc IP Server của bạn)
            // Thay "127.0.0.1" bằng IP thật nếu bạn dùng 2 máy khác nhau
            SocketManager.getInstance().connect("192.168.1.253", 8888);
            System.out.println("[Client]: Kết nối thành công!");
        } catch (IOException e) {
            System.err.println("[Lỗi]: Không tìm thấy Server. Hãy đảm bảo ServerMain đã chạy!");
            // Có thể hiện một thông báo nhanh ở đây nếu muốn
        }

        Parent root = FXMLLoader.load(getClass().getResource("/views/register.fxml"));
        Scene scene = new Scene(root);

        String cssPath = getClass().getResource("/styles/Button.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        stage.setScene(scene);
        stage.setTitle("TBOAT - Đấu giá trực tuyến");

        Image icon = new Image(getClass().getResourceAsStream("/images/logo.png"));
        stage.getIcons().add(icon);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}