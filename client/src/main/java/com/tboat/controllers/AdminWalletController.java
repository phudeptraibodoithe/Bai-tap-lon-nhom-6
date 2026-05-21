package com.tboat.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.network.ServerEvent;
import com.tboat.session.UserSession;
import com.tboat.socket.SocketHelper;
import com.tboat.socket.SocketListener;
import com.tboat.utilsclient.AlertUtils;
import com.tboat.utilsclient.CurrencyFormatter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

    private double currentBalance;
    private final String CORRECT_PIN = "123456";
    private boolean isDepositMode = true;
    private static final Logger log = LoggerFactory.getLogger(AdminWalletController.class);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (UserSession.getInstance().getUser() != null) {
            currentBalance = UserSession.getInstance().getUser().getBalance();
        }

        updateBalanceLabel();
        CurrencyFormatter.attachCurrencyListener(txtAmount);

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
        btnTabWithdraw.setStyle("-fx-background-color: #e67e22; -fx-background-radius: 5; -fx-text-fill: white; -fx-cursor: hand;");
        btnTabDeposit.setStyle("-fx-background-color: transparent; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-text-fill: #666666; -fx-cursor: hand;");
        btnSubmit.setText("XÁC NHẬN RÚT TIỀN");
        btnSubmit.setStyle("-fx-background-color: #e67e22; -fx-background-radius: 5; -fx-cursor: hand;");
    }

    public void logout(ActionEvent e) {
        boolean isConfirmed = AlertUtils.showConfirmation(
                "Xác nhận đăng xuất",
                "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Quản trị không?"
        );

        if (isConfirmed) {
            SocketHelper.sendRequest("LOGOUT");
            UserSession.getInstance().cleanUserSession();
            Button btnSource = (Button) e.getSource();
            changeScene(btnSource, "start.fxml");
        }
    }

    private void updateBalanceLabel() {
        lblBalance.setText(CurrencyFormatter.formatDisplay(currentBalance));
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
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng nhập số hợp lệ.");
        }
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            btnSubmit.setDisable(false);
            try {
                ServerEvent type = SocketHelper.getTypeEnum(response);

                if (handleBroadcast(type)) return;
                if (type != ServerEvent.TRANSACTION) return;

                ServerEvent status = SocketHelper.getStatusEnum(response);
                if (status == ServerEvent.SUCCESS) {
                    handleTransactionSuccess(response);
                } else {
                    handleTransactionFailure(SocketHelper.getMessage(response));
                }
            } catch (Exception e) {
                AlertUtils.showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Lỗi đọc dữ liệu từ Server.");
                log.error("Không thể đọc JSON: {} | {}", response, e.getMessage(), e);
            }
        });
    }

    private boolean handleBroadcast(ServerEvent type) {
        if (type == ServerEvent.AUCTION_FINISHED) {
            SocketHelper.sendRequest("GET_PROFILE", null);
            return true;
        }
        return false;
    }

    private void handleTransactionSuccess(String response) {
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if (!json.has("payload") || json.get("payload").isJsonNull()) return;

        double changedAmount = json.get("payload").getAsDouble();
        double newBalance    = UserSession.getInstance().getUser().getBalance() + changedAmount;
        UserSession.getInstance().getUser().setBalance(newBalance);
        currentBalance = newBalance;
        updateBalanceLabel();
        AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Giao dịch đã được xử lý thành công!");
    }

    private void handleTransactionFailure(String message) {
        String text = (message == null || message.isEmpty()) ? "Giao dịch bị từ chối." : message;
        AlertUtils.showAlert(Alert.AlertType.ERROR, "Thất bại", text);
    }
}