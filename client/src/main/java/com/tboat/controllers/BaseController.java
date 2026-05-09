package com.tboat.controllers;

import com.tboat.socket.SocketListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class BaseController {

    private static final Logger log = Logger.getLogger(BaseController.class.getName());
    private Scene scene;
    private Parent root;

    public void changeScene(Node node, String fxmlFileName) {
        if (this instanceof SocketListener) {
            ((SocketListener) this).unregisterSocket();
            log.info("[System] Đã tự động hủy đăng ký Socket cho: " + this.getClass().getSimpleName());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlFileName));
            root = loader.load();
            Object nextController = loader.getController();
            if (nextController instanceof SocketListener) {
                ((SocketListener) nextController).registerSocket();
                log.info("[System] Đã tự động đăng ký Socket cho Controller mới: " + nextController.getClass().getSimpleName());
            }
            scene = node.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/styles/Button.css").toExternalForm());
            scene.setRoot(root);
        } catch (Exception e) {
            log.severe("[System] Lỗi khi chuyển scene sang " + fxmlFileName + ": " + e.getMessage());
        }
    }

    public <T> T changeSceneAndGetController(Node node, String fxmlFileName) {
        if (this instanceof SocketListener) {
            ((SocketListener) this).unregisterSocket();
            log.info("[System] Đã tự động hủy đăng ký Socket cho: " + this.getClass().getSimpleName());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlFileName));
            root = loader.load();

            T nextController = loader.getController();

            if (nextController instanceof SocketListener) {
                ((SocketListener) nextController).registerSocket();
                log.info("[System] Đã tự động đăng ký Socket cho Controller mới: " + nextController.getClass().getSimpleName());
            }

            scene = node.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/styles/Button.css").toExternalForm());
            scene.setRoot(root);

            return nextController;

        } catch (Exception e) {
            log.severe("[System] Lỗi khi chuyển scene sang " + fxmlFileName + ": " + e.getMessage());
            return null;
        }
    }

    @FXML public void switchToMenu(Event event) { changeScene((Node) event.getSource(), "TrangChu.fxml"); }
    @FXML public void switchToHistory(ActionEvent event) { changeScene((Node) event.getSource(), "history.fxml"); }
    @FXML public void switchToPostItem(ActionEvent event) { changeScene((Node) event.getSource(), "postItem.fxml"); }
    @FXML public void switchToWallet(ActionEvent event) { changeScene((Node) event.getSource(), "NapRut.fxml"); }
    @FXML public void switchToProfile(ActionEvent event) { changeScene((Node) event.getSource(), "profile.fxml"); }
    @FXML public void switchToManager(ActionEvent event) { changeScene((Node) event.getSource(), "manager.fxml"); }
    @FXML public void switchToWalletAdmin(ActionEvent event) { changeScene((Node) event.getSource(), "adminWallet.fxml"); }
    @FXML public void switchToLogin(ActionEvent event) throws IOException { changeScene((Node) event.getSource(), "login.fxml"); }
    @FXML public void switchToRegister(ActionEvent event) throws IOException { changeScene((Node) event.getSource(), "register.fxml"); }
    @FXML public void switchToStart(MouseEvent event) throws IOException { changeScene((Node) event.getSource(), "start.fxml"); }
    @FXML public void switchToAdmin(ActionEvent event) throws IOException { changeScene((Node) event.getSource(), "admin.fxml"); }
}