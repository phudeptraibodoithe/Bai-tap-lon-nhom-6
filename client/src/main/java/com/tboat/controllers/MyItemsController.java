package com.tboat.controllers;

import com.tboat.models.AuctionSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MyItemsController extends BaseController {

    // Khai báo các ID trên FXML của trang Chi tiết sản phẩm
    @FXML private Label NameItem;
    @FXML private Label IdItem;
    @FXML private Label CurrentPrice;
    @FXML private Label Description;
    @FXML private Label NextPrice;
    @FXML private Label HighestBidder;
    @FXML private ImageView ItemImage;

    // Biến lưu trữ phiên đấu giá đang được mở
    private AuctionSession currentSession;

    // ====================================================================
    // HÀM ĐỔ DỮ LIỆU TỪ TRANG CHỦ / LỊCH SỬ SANG TRANG CHI TIẾT
    // ====================================================================
    public void setItemData(AuctionSession item) {
        if (item == null) {
            return;
        }

        this.currentSession = item; // Lưu lại để dùng cho nút "Đặt giá" sau này

        try {
            // 1. Đổ dữ liệu Tên và ID
            if (NameItem != null) NameItem.setText(item.getName());
            if (IdItem != null) IdItem.setText("ID : " + item.getId());

            // 2. Đổ dữ liệu Giá hiện tại
            if (CurrentPrice != null) {
                CurrentPrice.setText(String.format("%,.0f VNĐ", item.getCurrentPrice()));
            }

            // 3. Đổ dữ liệu Mô tả
            if (Description != null) {
                Description.setText(item.getDescription() != null ? item.getDescription() : "Không có mô tả.");
            }

            // 4. Tính toán Mức giá tối thiểu tiếp theo (Giá hiện tại + Bước nhảy)
            if (NextPrice != null) {
                double nextMin = item.getCurrentPrice() + item.getBidIncrease();
                NextPrice.setText(String.format("Mức giá hợp lệ tiếp theo: %,.0f VNĐ", nextMin));
            }

            // 5. Cập nhật Trạng thái và Người dẫn đầu
            if (HighestBidder != null) {
                String statusText = "🏆 Trạng thái: " + item.getStatusOfAuction().name();

                // Kiểm tra nếu đã có người đấu giá thì hiển thị thêm tên người đó
                String topBidder = item.getHighestBidderAccount();
                if (topBidder != null && !topBidder.isEmpty() && !topBidder.equals("N/A")) {
                    statusText += " | Đang dẫn đầu: " + topBidder;
                }
                HighestBidder.setText(statusText);
            }

            // 6. Xử lý Ảnh sản phẩm
            if (ItemImage != null && item.getImageURL() != null && !item.getImageURL().isEmpty()) {
                String imageUrl = item.getImageURL();
                try {
                    Image img;
                    // Kiểm tra xem ảnh là link web hay là file trong máy
                    if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                        img = new Image(imageUrl, true); // Tải ảnh từ web
                    } else {
                        // Tải ảnh từ thư mục resources/images/
                        img = new Image(getClass().getResourceAsStream("/images/" + imageUrl));
                    }

                    if (img != null && !img.isError()) {
                        ItemImage.setImage(img);
                    }
                } catch (Exception e) {
                    System.err.println("Không thể tải ảnh sản phẩm: " + imageUrl);
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi đổ dữ liệu AuctionSession vào giao diện ItemController!");
            e.printStackTrace();
        }
    }

    // (Sau này bạn có thể viết thêm hàm onAction cho nút "Xác nhận đặt giá" ở đây)
}