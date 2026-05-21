package com.tboat.utilsclient;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.logging.Logger;

public class ImageUtils {

    private static final Logger log = Logger.getLogger(ImageUtils.class.getName());

    public static File chooseImageFile(Window ownerWindow, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title != null ? title : "Chọn ảnh");
        // Gộp chung các định dạng phổ biến nhất
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        return fileChooser.showOpenDialog(ownerWindow);
    }

    // --- ĐÃ THÊM: Tiện ích cắt ảnh bo góc cho ImageView ---
    public static void applyRoundedClip(ImageView imageView, double arcWidth, double arcHeight) {
        if (imageView == null) return;
        Rectangle clip = new Rectangle(imageView.getFitWidth(), imageView.getFitHeight());
        clip.setArcWidth(arcWidth);
        clip.setArcHeight(arcHeight);
        imageView.setClip(clip);
    }
    public static String fileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            log.severe("Lỗi khi đọc file ảnh: " + e.getMessage());
            return null;
        }
    }
    public static Image base64ToImage(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return null;
        }
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64String);
            return new Image(new ByteArrayInputStream(imageBytes));
        } catch (IllegalArgumentException e) {
            log.warning("Chuỗi Base64 bị lỗi, không thể chuyển thành ảnh!");
            return null;
        }
    }
}