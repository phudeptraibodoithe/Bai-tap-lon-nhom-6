package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class TrangChuController {
    @FXML
    private Label CurrentPrice;

    @FXML
    private Label HighestBidder;

    @FXML
    private TextField BidAmount;

    @FXML
    private Button Bid;
    @FXML
    public void handlePlaceBid() {
        String input = BidAmount.getText();
        if (input == null || input.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING,"Cảnh báo", "Vui lòng nhập mức giá bạn muốn đặt!");
            return;
        }
        try{
            double newBid= Double.parseDouble(BidAmount.getText());
            double currentHighestPrice=Double.parseDouble(CurrentPrice.getText())
            if(newBid<= currentHighestPrice)){
                showAlert(Alert.AlertType.WARNING,"Lỗi đặt giá", "Bạn phải đặt mức giá cao hơn giá hiện tại!");
                return;
            }
            currentHighestPrice = newBid;
            CurrentPrice.setText(String.format("%,.0f VNĐ", currentHighestPrice));
            HighestBidder.setText("🏆 Người dẫn đầu:", );
            BidAmount.clear();

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Bạn đã đặt giá thành công!");
        }
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
