package com.tboat.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.SocketListener;
import com.tboat.utilsclient.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class NapRutController extends BaseController implements Initializable, SocketListener {

    @FXML private Label lblBalance;
    @FXML private Button btnTabDeposit;
    @FXML private Button btnTabWithdraw;
    @FXML private TextField txtAmount;
    @FXML private TextField txtPin;
    @FXML private Button btnSubmit;
    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    private double currentBalance = UserSession.getInstance().getBalance();
    private final String CORRECT_PIN = "123456";
    private boolean isDepositMode = true;
    private static final Logger logger = Logger.getLogger(NapRutController.class.getName());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
        updateBalanceLabel();
        setupAmountFieldFormat();
        txtPin.setText("123456");
        btnTabDeposit.setOnAction(event -> switchToDepositMode());
        btnTabWithdraw.setOnAction(event -> switchToWithdrawMode());
        btnSubmit.setOnAction(event -> handleTransaction());
    }

    private void setupAmountFieldFormat() {
        txtAmount.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            String cleanString = newValue.replaceAll("[^\\d]", "");
            if (cleanString.isEmpty()) {
                txtAmount.setText("");
                return;
            }
            try {
                double parsed = Double.parseDouble(cleanString);
                String formatted = CurrencyFormatter.formatInput(parsed);
                if (!newValue.equals(formatted)) {
                    txtAmount.setText(formatted);
                    Platform.runLater(() -> txtAmount.positionCaret(formatted.length()));
                }
            } catch (NumberFormatException e) {
                txtAmount.setText(oldValue);
            }
        });
    }

    private void handleTransaction() {
        String amountText = txtAmount.getText();
        String pinText = txtPin.getText();

        if (amountText.trim().isEmpty() || pinText.trim().isEmpty()) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ Số tiền và Mã PIN!");
            return;
        }
        if (!pinText.equals(CORRECT_PIN)) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Sai mã PIN", "Mã PIN không chính xác. Vui lòng thử lại!");
            return;
        }
        try {
            double amount = CurrencyFormatter.parse(amountText);
            if (amount <= 0) {
                AlertUtils.showAlert(Alert.AlertType.ERROR, "Lỗi số tiền", "Số tiền giao dịch phải lớn hơn 0!");
                return;
            }
            btnSubmit.setDisable(true);
            double amountToSend = isDepositMode ? amount : -amount;
            SocketHelper.sendRequest("TRANSACTION", amountToSend);
        } catch (Exception e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Đã xảy ra lỗi khi xử lý số tiền.");
        }
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
        btnTabWithdraw.setStyle("-fx-background-color: #e67e22; -fx-background-radius: 5; -fx-text-fill: white; -fx-cursor: hand;");
        btnTabDeposit.setStyle("-fx-background-color: transparent; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-text-fill: #666666; -fx-cursor: hand;");
        btnSubmit.setText("XÁC NHẬN RÚT TIỀN");
        btnSubmit.setStyle("-fx-background-color: #e67e22; -fx-background-radius: 5; -fx-cursor: hand;");
    }

    private void updateBalanceLabel() {
        lblBalance.setText(CurrencyFormatter.formatDisplay(currentBalance));
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            btnSubmit.setDisable(false);
            try {
                if (!"TRANSACTION".equals(SocketHelper.getType(response))) return;

                String status  = SocketHelper.getStatus(response);
                String message = SocketHelper.getMessage(response);

                if ("SUCCESS".equals(status)) {
                    double changedAmount = SocketHelper.getPayloadObject(response) == null
                            ? JsonParser.parseString(response).getAsJsonObject().get("payload").getAsDouble()
                            : 0;
                    // payload ở đây là số, không phải object → parse thẳng
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    if (json.has("payload") && !json.get("payload").isJsonNull()) {
                        changedAmount = json.get("payload").getAsDouble();
                        double newBalance = UserSession.getInstance().getUser().getBalance() + changedAmount;
                        UserSession.getInstance().getUser().setBalance(newBalance);
                        currentBalance = newBalance;
                        updateBalanceLabel();
                        txtAmount.setText("");
                        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                message.isEmpty() ? "Giao dịch đã được xử lý thành công!" : message);
                    }
                } else if ("FAILED".equals(status) || "ERROR".equals(status)) {
                    AlertUtils.showAlert(Alert.AlertType.ERROR, "Thất bại",
                            message.isEmpty() ? "Giao dịch bị từ chối." : message);
                }
            } catch (Exception e) {
                AlertUtils.showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Lỗi đọc dữ liệu từ Server.");
                logger.severe("❌ KHÔNG THỂ ĐỌC JSON NẠP/RÚT: " + response);
            }
        });
    }
}