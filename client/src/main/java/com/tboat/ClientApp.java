package com.tboat;

import com.tboat.socket.SocketManager;
import com.tboat.ucb.CacheInterceptor;
import com.tboat.ucb.UCBEngine;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import com.tboat.logging.LogConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(ClientApp.class);
    @Override
    public void start(Stage stage) throws Exception {
// ── UCB System khởi động ─────────────────────────────────
        UCBEngine.getInstance();   // Load lịch sử từ Preferences
        SocketManager.getInstance().subscribe(CacheInterceptor.getInstance());
        log.info("[UCB] CacheInterceptor đã đăng ký.");

        Parent root = FXMLLoader.load(getClass().getResource("/views/start.fxml"));
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
        LogConfig logConfig = new LogConfig("logs/client", 15);
        logConfig.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("App đang tắt...");
            logConfig.stop();
        }));

        launch(args);
    }
}