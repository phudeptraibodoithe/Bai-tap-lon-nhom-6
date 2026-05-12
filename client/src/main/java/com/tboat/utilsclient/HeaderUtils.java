package com.tboat.utilsclient;

import com.tboat.controllers.BaseController;
import com.tboat.models.User;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderUtils {

    // Khởi tạo logger để thay thế
    private static final Logger logger = LoggerFactory.getLogger(HeaderUtils.class);

    public static void setupHeader(Label lblGreeting, ImageView userAvatar, BaseController controller) {
        User currentUser = UserSession.getInstance().getUser();

        if (currentUser != null) {
            if (lblGreeting != null) lblGreeting.setText("Hello, " + currentUser.getNickname() + "!");

            if (userAvatar != null) {
                String pathOrBase64 = currentUser.getAvatarURL();
                Image image = null;
                try {
                    if (pathOrBase64 == null || pathOrBase64.trim().isEmpty() || pathOrBase64.equals("null")) {
                        image = new Image(HeaderUtils.class.getResource("/images/avtDefault.jpg").toExternalForm());
                    } else {
                        if (!pathOrBase64.startsWith("file:/") && !pathOrBase64.startsWith("http")) {
                            image = ImageUtils.base64ToImage(pathOrBase64);
                        } else {
                            image = new Image(pathOrBase64, true);
                        }
                    }
                    if (image != null) {
                        userAvatar.setImage(image);
                        double radius = Math.min(userAvatar.getFitWidth(), userAvatar.getFitHeight()) / 2;
                        if (radius <= 0) radius = 20;
                        userAvatar.setClip(new Circle(radius, radius, radius));
                    }
                } catch (Exception e) {
                    try {
                        image = new Image(HeaderUtils.class.getResource("/images/avtDefault.jpg").toExternalForm());
                        userAvatar.setImage(image);
                        userAvatar.setClip(new Circle(20, 20, 20));
                    } catch (Exception ignored) {}
                }
            }
        }

        Platform.runLater(() -> {
            if (lblGreeting != null && lblGreeting.getScene() != null && controller != null) {
                lblGreeting.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F5) {
                        logger.info("Đã bắt được phím F5! Đang tải lại dữ liệu...");
                        controller.onReload();
                        event.consume();
                    }
                });
            }
        });
    }
}