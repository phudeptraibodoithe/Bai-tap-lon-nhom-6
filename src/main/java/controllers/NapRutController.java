package controllers;

import com.example.giaodiendau.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class NapRutController extends BaseController {

    @FXML private Label lblBalance;
    @FXML private Button btnTabDeposit;
    @FXML private Button btnTabWithdraw;
    @FXML private TextField txtAmount;
    @FXML private TextField txtPin;
    @FXML private Button btnSubmit;


    private double currentBalance = 15000000; // Số dư hiện tại: 15 triệu
    private final String CORRECT_PIN = "123456"; // Mã PIN đúng để test

    // Biến này để nhớ xem người dùng đang ở chế độ Nạp hay Rút
    private boolean isDepositMode = true;

    // 3. Hàm khởi tạo (Chạy khi mở màn hình)
    @FXML
    public void initialize() {
        // Cập nhật số dư ban đầu lên màn hình
        updateBalanceLabel();

        // Cài đặt sự kiện khi bấm 2 nút chuyển Tab
        btnTabDeposit.setOnAction(event -> switchToDepositMode());
        btnTabWithdraw.setOnAction(event -> switchToWithdrawMode());

        // Cài đặt sự kiện khi bấm nút XÁC NHẬN to đùng ở dưới
        btnSubmit.setOnAction(event -> handleTransaction());
    }

    // ================= CÁC HÀM XỬ LÝ GIAO DIỆN =================

    private void switchToDepositMode() {
        isDepositMode = true;
        btnTabDeposit.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 5; -fx-text-fill: white; -fx-cursor: hand;");
        btnTabWithdraw.setStyle("-fx-background-color: transparent; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-text-fill: #666666; -fx-cursor: hand;");

        btnSubmit.setText("XÁC NHẬN NẠP TIỀN");
        btnSubmit.setStyle("-fx-background-color: #0056b3; -fx-background-radius: 5; -fx-cursor: hand;");
    }

    private void switchToWithdrawMode() {
        isDepositMode = false;
        // Đổi nút Rút thành màu Cam, nút Nạp thành màu Xám/Trắng
        btnTabWithdraw.setStyle("-fx-background-color: #e67e22; -fx-background-radius: 5; -fx-text-fill: white; -fx-cursor: hand;");
        btnTabDeposit.setStyle("-fx-background-color: transparent; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-text-fill: #666666; -fx-cursor: hand;");

        btnSubmit.setText("XÁC NHẬN RÚT TIỀN");
        btnSubmit.setStyle("-fx-background-color: #e67e22; -fx-background-radius: 5; -fx-cursor: hand;"); // Màu cam cho nguy hiểm
    }

    private void updateBalanceLabel() {
        lblBalance.setText(String.format("%,.0f", currentBalance));
    }

    // ================= HÀM XỬ LÝ LOGIC TIỀN BẠC (QUAN TRỌNG) =================

    private void handleTransaction() {
        String amountText = txtAmount.getText();
        String pinText = txtPin.getText();

        // 1. Kiểm tra nhập thiếu
        if (amountText.trim().isEmpty() || pinText.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ Số tiền và Mã PIN!");
            return;
        }

        // 2. Kiểm tra mã PIN
        if (!pinText.equals(CORRECT_PIN)) {
            showAlert(Alert.AlertType.ERROR, "Sai mã PIN", "Mã PIN không chính xác. Vui lòng thử lại!");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            // Kiểm tra số tiền phải lớn hơn 0
            if (amount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi số tiền", "Số tiền giao dịch phải lớn hơn 0!");
                return;
            }

            // 3. Phân nhánh logic: Nạp hoặc Rút
            if (isDepositMode) {
                // LOGIC NẠP TIỀN
                currentBalance += amount;
                showAlert(Alert.AlertType.INFORMATION, "Nạp tiền thành công", "Bạn đã nạp " + String.format("%,.0f VNĐ", amount) + " vào ví.");
            } else {
                // LOGIC RÚT TIỀN (Phải kiểm tra số dư)
                if (amount > currentBalance) {
                    showAlert(Alert.AlertType.ERROR, "Số dư không đủ", "Bạn không thể rút số tiền lớn hơn số dư hiện có trong ví!");
                    return; // Chặn lại ngay lập tức
                }
                currentBalance -= amount;
                showAlert(Alert.AlertType.INFORMATION, "Rút tiền thành công", "Bạn đã rút " + String.format("%,.0f VNĐ", amount) + " về ngân hàng.");
            }

            // 4. Cập nhật lại giao diện và xóa trắng form
            updateBalanceLabel();
            txtAmount.clear();
            txtPin.clear();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng chỉ nhập số vào ô Số tiền.");
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