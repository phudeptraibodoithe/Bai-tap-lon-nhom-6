package com.tboat.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import com.tboat.utils.GsonUtils;
import com.tboat.utilsclient.CurrencyFormatter;
import com.tboat.utilsclient.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminWalletController extends BaseController implements Initializable, SocketListener {

    @FXML private Label lblBalance;
    @FXML private Button btnTabDeposit;
    @FXML private Button btnTabWithdraw;
    @FXML private TextField txtAmount;
    @FXML private TextField txtPin;
    @FXML private Button btnSubmit;

    private double currentBalance = UserSession.getInstance().getBalance();
    private final String CORRECT_PIN = "123456";
    private boolean isDepositMode = true;
    private static final Logger log = LoggerFactory.getLogger(AdminWalletController.class);
    private final Gson gson = GsonUtils.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        SocketManager.getInstance().subscribe(this); // Đừng quên subscribe socket
        updateBalanceLabel();
        setupAmountFieldFormat(); // Thêm hàm format khi gõ
        txtPin.setText("123456");
        btnTabDeposit.setOnAction(event -> switchToDepositMode());
        btnTabWithdraw.setOnAction(event -> switchToWithdrawMode());
        btnSubmit.setOnAction(event -> handleTransaction());
    }

    private void setupAmountFieldFormat() {
        txtAmount.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            // Chỉ lấy số
            String cleanString = newValue.replaceAll("[^\\d]", "");
            if (cleanString.isEmpty()) {
                txtAmount.setText("");
                return;
            }

            try {
                double parsed = Double.parseDouble(cleanString);
                // Dùng formatInput để chỉ có dấu phẩy, không có chữ "VNĐ" khi đang gõ
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

    public void logout(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        DialogPane dialogPane = alert.getDialogPane();
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Quản trị không?");
        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        if (alert.showAndWait().orElse(btnNo) == btnYes) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "LOGOUT");
            SocketManager.getInstance().send(gson.toJson(request));

            UserSession.getInstance().cleanUserSession();

            Button btnSource = (Button) e.getSource();
            changeScene(btnSource, "start.fxml");
        }
    }

    private void updateBalanceLabel() {
        lblBalance.setText(String.format("%,.0f", currentBalance));
    }

    private void handleTransaction() {
        String amountText = txtAmount.getText();
        String pinText = txtPin.getText();

        if (amountText.trim().isEmpty() || pinText.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ Số tiền và Mã PIN!");
            return;
        }

        if (!pinText.equals(CORRECT_PIN)) {
            showAlert(Alert.AlertType.ERROR, "Sai mã PIN", "Mã PIN không chính xác. Vui lòng thử lại!");
            return;
        }

        try {
            double amount = CurrencyFormatter.parse(amountText);

            if (amount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi số tiền", "Số tiền giao dịch phải lớn hơn 0!");
                return;
            }
            btnSubmit.setDisable(true);
            double amountToSend = isDepositMode ? amount : -amount;

            JsonObject request = new JsonObject();
            request.addProperty("action", "TRANSACTION");
            request.addProperty("payload", amountToSend);

            SocketManager.getInstance().send(gson.toJson(request));

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng nhập số hợp lệ.");
        }
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            btnSubmit.setDisable(false);

            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : "";
                String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "";

                if ("SUCCESS".equals(status)) {
                    // Đọc lượng tiền thay đổi từ payload thay vì data
                    if (jsonResponse.has("payload") && !jsonResponse.get("payload").isJsonNull()) {
                        double changedAmount = jsonResponse.get("payload").getAsDouble();
                        double newBalance = UserSession.getInstance().getUser().getBalance() + changedAmount;

                        UserSession.getInstance().getUser().setBalance(newBalance);
                        currentBalance = newBalance;
                        updateBalanceLabel();

                        String successMsg = message.isEmpty() ? "Giao dịch đã được xử lý thành công!" : message;
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", successMsg);
                    }
                } else if ("FAILED".equals(status) || "ERROR".equals(status)) {
                    String errorMsg = message.isEmpty() ? "Giao dịch bị từ chối." : message;
                    showAlert(Alert.AlertType.ERROR, "Thất bại", errorMsg);
                }

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Lỗi đọc dữ liệu từ Server.");
                log.error("Không thể đọc JSON nạp/rút từ server: {} | Exception: {}", response, e.getMessage(), e);
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