package com.tboat.controllers;

import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class NapRutController extends BaseController implements Initializable, SocketListener {

    @FXML private Label lblBalance;
    @FXML private Button btnTabDeposit;
    @FXML private Button btnTabWithdraw;
    @FXML private TextField txtAmount;
    @FXML private TextField txtPin;
    @FXML private Button btnSubmit;


    private double currentBalance = UserSession.getInstance().getBalance();
    private final String CORRECT_PIN = "123456"; // Mã PIN đúng để test
    private boolean isDepositMode = true;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        updateBalanceLabel();
        txtPin.setText("123456");
        btnTabDeposit.setOnAction(event -> switchToDepositMode());
        btnTabWithdraw.setOnAction(event -> switchToWithdrawMode());
        btnSubmit.setOnAction(event -> handleTransaction());
    }
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
            btnSubmit.setDisable(true);
            double amountToSend = isDepositMode ? amount : -amount;
            String message = "TRANSACTION|" + amountToSend;
            SocketManager.getInstance().send(message);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng chỉ nhập số vào ô Số tiền.");
        }
    }
    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            btnSubmit.setDisable(false);
            String[] parts = response.split("\\|");
            if (parts[0].equals("TRANSACTION_SUCCESS")) {
                double changedAmount = Double.parseDouble(parts[1]);
                double newBalance = UserSession.getInstance().getUser().getBalance() + changedAmount;
                UserSession.getInstance().getUser().setBalance(newBalance);
                currentBalance = newBalance;
                updateBalanceLabel();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Giao dịch đã được xử lý!");
            } else if (parts[0].equals("TRANSACTION_FAILED")) {
                showAlert(Alert.AlertType.ERROR, "Thất bại", parts[1]);
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}