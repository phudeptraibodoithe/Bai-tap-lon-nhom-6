package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ItemController extends BaseController {
    @FXML private Label lblProductName;
    @FXML private Label lblPrice;
    // ... các fx:id khác của trang chi tiết

    // Hàm này dùng để nhận dữ liệu từ trang khác gửi sang
    public void setItemData(MyItem item) {
        lblProductName.setText(item.getName());
        lblPrice.setText(String.format("%,.0f VNĐ", item.getCurrentPrice()));
        // Đổ thêm dữ liệu vào các field khác ở đây
    }
}