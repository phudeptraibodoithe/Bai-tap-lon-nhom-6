package com.tboat.utilsclient;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.logging.Logger;

public class AlertUtils {
    private static final Logger log = Logger.getLogger(AlertUtils.class.getName());

    // Hàm nội bộ để setup giao diện chung (Icon, DialogPane)
    private static void styleAlert(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(AlertUtils.class.getResource("/styles/Button.css").toExternalForm());

        alert.setHeaderText(null); // Tắt phần header thừa thãi

        try {
            Stage alertStage = (Stage) dialogPane.getScene().getWindow();
            alertStage.getIcons().add(new Image(AlertUtils.class.getResourceAsStream("/images/logo.png")));
        } catch (Exception e) {
            log.warning("Lỗi load icon cho Alert: " + e.getMessage());
        }
    }

    // 1. Hàm show thông báo bình thường (Chỉ có nút OK)
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);

        // Việt hóa nút OK mặc định
        ButtonType btnOk = new ButtonType("Đóng", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(btnOk);

        styleAlert(alert);
        alert.showAndWait();
    }

    // 2. Hàm show xác nhận (Có nút Có / Không) - Dùng cho Logout, Xóa,...
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        styleAlert(alert);

        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        // Trả về true nếu người dùng bấm "Có"
        return alert.showAndWait().orElse(btnNo) == btnYes;
    }
    public static void showStatus(Label label, String msg, String color) {
        if (label != null) {
            // Bọc Platform.runLater để đảm bảo an toàn khi gọi từ luồng Socket
            javafx.application.Platform.runLater(() -> {
                label.setText(msg);
                label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            });
        }
    }
}