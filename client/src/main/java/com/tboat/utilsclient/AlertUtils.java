package com.tboat.utilsclient;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.logging.Logger;

public class AlertUtils {

    private static final Logger log = Logger.getLogger(AlertUtils.class.getName());

    // Thời gian hiển thị thông báo status (giây)
    private static final double STATUS_DISPLAY_SECONDS = 6.0;

    // ── Internal helper ──────────────────────────────────────────────────────

    private static void styleAlert(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(
                AlertUtils.class.getResource("/styles/Button.css").toExternalForm());
        alert.setHeaderText(null);

        try {
            Stage stage = (Stage) pane.getScene().getWindow();
            stage.getIcons().add(
                    new Image(AlertUtils.class.getResourceAsStream("/images/logo.png")));
        } catch (Exception e) {
            log.warning("Lỗi load icon cho Alert: " + e.getMessage());
        }
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Hiển thị hộp thoại thông báo thông thường (chỉ nút Đóng).
     */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);

        ButtonType btnOk = new ButtonType("Đóng", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(btnOk);

        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Hiển thị hộp thoại xác nhận (Có / Không).
     * Trả về true nếu người dùng bấm "Có".
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setContentText(message);

        ButtonType btnYes = new ButtonType("Có",   ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo  = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        styleAlert(alert);
        return alert.showAndWait().orElse(btnNo) == btnYes;
    }

    /**
     * Hiển thị thông báo inline lên Label, rồi tự động xóa sau {@value STATUS_DISPLAY_SECONDS} giây.
     * An toàn khi gọi từ bất kỳ thread nào.
     */
    public static void showStatus(Label label, String msg, String color) {
        if (label == null) return;

        Platform.runLater(() -> {
            label.setText(msg);
            label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");

            // Tự động xóa sau STATUS_DISPLAY_SECONDS giây
            PauseTransition pause = new PauseTransition(Duration.seconds(STATUS_DISPLAY_SECONDS));
            pause.setOnFinished(e -> {
                // Chỉ xóa nếu label vẫn đang hiển thị đúng message này
                // (tránh xóa nhầm message mới hơn)
                if (msg.equals(label.getText())) {
                    label.setText("");
                    label.setStyle("");
                }
            });
            pause.play();
        });
    }

    /**
     * Overload cho phép tuỳ chỉnh thời gian hiển thị (giây).
     */
    public static void showStatus(Label label, String msg, String color, double seconds) {
        if (label == null) return;

        Platform.runLater(() -> {
            label.setText(msg);
            label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");

            PauseTransition pause = new PauseTransition(Duration.seconds(seconds));
            pause.setOnFinished(e -> {
                if (msg.equals(label.getText())) {
                    label.setText("");
                    label.setStyle("");
                }
            });
            pause.play();
        });
    }
}