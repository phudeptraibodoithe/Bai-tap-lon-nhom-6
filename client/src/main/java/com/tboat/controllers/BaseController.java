package com.tboat.controllers;

import com.tboat.socket.SocketListener;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
import com.tboat.ucb.PrefetchManager;
import com.tboat.ucb.UCBEngine;
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

    protected static final Logger log = Logger.getLogger(BaseController.class.getName());
    private Scene scene;
    private Parent root;

    // ── Track màn hình hiện tại (static = share toàn app) ────────────────────
    private static String currentScreenKey = null;

    // ─────────────────────────────────────────────────────────────────────────
    // CORE: changeScene — thêm UCB hooks, giữ nguyên logic socket cũ
    // ─────────────────────────────────────────────────────────────────────────
    public void changeScene(Node node, String fxmlFileName) {
        // [Cũ] Hủy socket của màn hình hiện tại
        if (this instanceof SocketListener) {
            ((SocketListener) this).unregisterSocket();
            log.info("[System] Đã hủy Socket: " + this.getClass().getSimpleName());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlFileName));
            root = loader.load();

            // [Cũ] Đăng ký socket cho màn mới
            Object nextController = loader.getController();
            if (nextController instanceof SocketListener) {
                ((SocketListener) nextController).registerSocket();
                log.info("[System] Đã đăng ký Socket: " + nextController.getClass().getSimpleName());
            }

            scene = node.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(
                    getClass().getResource("/styles/Button.css").toExternalForm());
            scene.setRoot(root);

            // [UCB] Ghi nhận navigate + trigger prefetch
            trackNavigation(fxmlFileName);

        } catch (Exception e) {
            log.severe("[System] Lỗi chuyển scene → " + fxmlFileName + ": " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORE: changeSceneAndGetController — thêm UCB hooks tương tự
    // ─────────────────────────────────────────────────────────────────────────
    public <T> T changeSceneAndGetController(Node node, String fxmlFileName) {
        if (this instanceof SocketListener) {
            ((SocketListener) this).unregisterSocket();
            log.info("[System] Đã hủy Socket: " + this.getClass().getSimpleName());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlFileName));
            root = loader.load();

            T nextController = loader.getController();
            if (nextController instanceof SocketListener) {
                ((SocketListener) nextController).registerSocket();
                log.info("[System] Đã đăng ký Socket: " + nextController.getClass().getSimpleName());
            }

            scene = node.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(
                    getClass().getResource("/styles/Button.css").toExternalForm());
            scene.setRoot(root);

            // [UCB] Ghi nhận navigate + trigger prefetch
            trackNavigation(fxmlFileName);

            return nextController;

        } catch (Exception e) {
            log.severe("[System] Lỗi chuyển scene → " + fxmlFileName + ": " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UCB: Hàm trung tâm — gọi sau mỗi lần navigate thành công
    // ─────────────────────────────────────────────────────────────────────────
    private void trackNavigation(String fxmlFileName) {
        String toKey   = toScreenKey(fxmlFileName);
        String fromKey = currentScreenKey;

        // Lấy kết quả cache hit (controller vừa vào đã báo chưa?)
        boolean wasCacheHit = NavigationContext.getInstance()
                .popCacheHit(toKey);

        // Ghi nhận vào UCB để học pattern
        UCBEngine.getInstance().recordNavigation(fromKey, toKey, wasCacheHit);

        // Cập nhật màn hình hiện tại
        currentScreenKey = toKey;

        // Trigger prefetch cho màn hình tiếp theo (ngầm, không block UI)
        PrefetchManager.getInstance().onScreenEntered(toKey);

        log.info(String.format("[UCB-Nav] %s → %s | cacheHit=%b",
                fromKey, toKey, wasCacheHit));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: "TrangChu.fxml" → "TrangChuFxml", "admin.fxml" → "adminFxml"
    // ─────────────────────────────────────────────────────────────────────────
    public static String toScreenKey(String fxmlFile) {
        return fxmlFile.replace(".fxml", "") + "Fxml";
    }

    public static String getCurrentScreenKey() {
        return currentScreenKey;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Các method cũ — GIỮ NGUYÊN 100%
    // ─────────────────────────────────────────────────────────────────────────
    public void onReload() {
        log.info("Trang này chưa có dữ liệu động cần làm mới.");
    }

    @FXML
    public void handleReloadClick(ActionEvent event) { onReload(); }

    @FXML public void switchToMenu(Event event)         { changeScene((Node) event.getSource(), "TrangChu.fxml"); }
    @FXML public void switchToHistory(ActionEvent e)    { changeScene((Node) e.getSource(), "history.fxml"); }
    @FXML public void switchToPostItem(ActionEvent e)   { changeScene((Node) e.getSource(), "postItem.fxml"); }
    @FXML public void switchToWallet(ActionEvent e)     { changeScene((Node) e.getSource(), "NapRut.fxml"); }
    @FXML public void switchToProfile(ActionEvent e)    { changeScene((Node) e.getSource(), "profile.fxml"); }
    @FXML public void switchToManager(ActionEvent e)    { changeScene((Node) e.getSource(), "manager.fxml"); }
    @FXML public void switchToWalletAdmin(ActionEvent e){ changeScene((Node) e.getSource(), "adminWallet.fxml"); }
    @FXML public void switchToLogin(ActionEvent e) throws IOException     { changeScene((Node) e.getSource(), "login.fxml"); }
    @FXML public void switchToRegister(ActionEvent e) throws IOException  { changeScene((Node) e.getSource(), "register.fxml"); }
    @FXML public void switchToStart(MouseEvent e) throws IOException      { changeScene((Node) e.getSource(), "start.fxml"); }
    @FXML public void switchToAdmin(ActionEvent e) throws IOException     { changeScene((Node) e.getSource(), "admin.fxml"); }
}