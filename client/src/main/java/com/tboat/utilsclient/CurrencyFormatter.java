package com.tboat.utilsclient;

import javafx.application.Platform;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyFormatter {
    private static final DecimalFormat inputFormat;
    private static final DecimalFormat displayFormat;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        inputFormat = new DecimalFormat("#,###", symbols);
        displayFormat = new DecimalFormat("#,### VNĐ", symbols);
    }

    // --- HÀM PARSE CHUẨN (Tái sử dụng ở mọi nơi) ---
    public static Double parse(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        try {
            String cleanString = text.replaceAll("[^\\d]", "");
            return cleanString.isEmpty() ? 0.0 : Double.parseDouble(cleanString);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // --- CÁC HÀM FORMAT ---
    public static String formatInput(Double value) {
        if (value == null || value == 0) return "";
        return inputFormat.format(value);
    }

    public static String formatDisplay(Double value) {
        if (value == null || value == 0) return "0 VNĐ";
        return displayFormat.format(value);
    }

    // --- LISTENER CHO TEXTFIELD ---
    public static void attachCurrencyListener(TextField textField) {
        if (textField == null) return;

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            // 👉 Đã tối ưu: Tái sử dụng hàm parse
            Double parsed = parse(newValue);
            if (parsed == 0.0 && !newValue.matches(".*\\d.*")) {
                textField.setText("");
                return;
            }

            String formatted = formatInput(parsed);
            if (!newValue.equals(formatted)) {
                textField.setText(formatted);
                Platform.runLater(() -> textField.positionCaret(formatted.length()));
            }
        });
    }

    // --- SETUP CHO SPINNER ---
    public static void setupCurrencySpinner(Spinner<Double> spinner, double initialValue, double step) {
        SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e18, initialValue, step);

        factory.setConverter(new CurrencyStringConverter());
        spinner.setValueFactory(factory);
        spinner.setEditable(true);

        TextField editor = spinner.getEditor();

        // 1. Format Real-time khi gõ
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            // 👉 Đã tối ưu: Tái sử dụng hàm parse
            Double parsed = parse(newValue);
            if (parsed == 0.0 && !newValue.matches(".*\\d.*")) {
                editor.setText("");
                return;
            }

            String formatted = formatDisplay(parsed);
            Platform.runLater(() -> {
                int currentCaret = editor.getCaretPosition();
                int oldLength = editor.getText().length();
                editor.setText(formatted);
                int newLength = formatted.length();
                int selection = currentCaret + (newLength - oldLength);
                editor.positionCaret(Math.max(0, Math.min(selection, newLength - 4))); // -4 để né chữ " VNĐ"
            });
        });

        // 2. 👉 Đã tối ưu: Ép commit giá trị siêu ngắn gọn khi click ra ngoài
        editor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                spinner.increment(0); // Trigger commit logic mặc định của Spinner
            }
        });
    }

}