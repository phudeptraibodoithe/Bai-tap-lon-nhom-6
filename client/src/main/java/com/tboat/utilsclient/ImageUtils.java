package com.tboat.utilsclient;

import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class ImageUtils {

    // 1. Dùng cho Trang ĐĂNG SẢN PHẨM: Biến File ảnh thành chuỗi văn bản để gửi đi
    // Lưu ý chữ "static" để gọi được ở mọi nơi mà không cần "new"
    public static String fileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 2. Dùng cho TRANG CHỦ & TRANG AUCTION: Nhận chuỗi văn bản từ Server, dịch ngược lại thành Ảnh
    public static Image base64ToImage(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return null; // Trả về null nếu không có ảnh
        }
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64String);
            return new Image(new ByteArrayInputStream(imageBytes));
        } catch (IllegalArgumentException e) {
            System.err.println("Chuỗi Base64 bị lỗi, không thể chuyển thành ảnh!");
            return null;
        }
    }
}