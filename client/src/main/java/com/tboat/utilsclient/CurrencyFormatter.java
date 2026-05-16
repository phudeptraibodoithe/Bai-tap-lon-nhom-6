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

    // Gắn Listener định dạng tiền tệ trực tiếp vào TextField
    public static void attachCurrencyListener(TextField textField) {
        if (textField == null) return;

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            // Xóa mọi ký tự không phải số
            String cleanString = newValue.replaceAll("[^\\d]", "");
            if (cleanString.isEmpty()) {
                textField.setText("");
                return;
            }

            try {
                double parsed = Double.parseDouble(cleanString);
                String formatted = formatInput(parsed);

                // Chỉ cập nhật nếu chuỗi format khác chuỗi hiện tại (tránh lặp vô hạn)
                if (!newValue.equals(formatted)) {
                    textField.setText(formatted);
                    // Đưa con trỏ nháy về cuối dòng sau khi format
                    Platform.runLater(() -> textField.positionCaret(formatted.length()));
                }
            } catch (NumberFormatException e) {
                textField.setText(oldValue); // Nếu lỗi, khôi phục giá trị cũ
            }
        });
    }

    public static String format(Double value) {
        return formatDisplay(value);
    }
    public static String formatInput(Double value) {
        if (value == null || value == 0) return "";
        return inputFormat.format(value);
    }

    public static String formatDisplay(Double value) {
        if (value == null || value == 0) return "0 VNĐ";
        return displayFormat.format(value);
    }

    public static Double parse(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        try {
            String cleanString = text.replaceAll("[^\\d]", "");
            return cleanString.isEmpty() ? 0.0 : Double.parseDouble(cleanString);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static void setupCurrencySpinner(Spinner<Double> spinner, double initialValue, double step) {
        SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e18, initialValue, step);

        // Tận dụng chính CurrencyStringConverter ở đây!
        factory.setConverter(new CurrencyStringConverter());
        spinner.setValueFactory(factory);
        spinner.setEditable(true);

        TextField editor = spinner.getEditor();

        // 1. Format Real-time khi gõ
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            String digits = newValue.replaceAll("[^\\d]", "");
            if (digits.isEmpty()) {
                editor.setText("");
                return;
            }
            try {
                double value = Double.parseDouble(digits);
                String formatted = formatDisplay(value);
                Platform.runLater(() -> {
                    int currentCaret = editor.getCaretPosition();
                    int oldLength = editor.getText().length();
                    editor.setText(formatted);
                    int newLength = formatted.length();
                    int selection = currentCaret + (newLength - oldLength);
                    editor.positionCaret(Math.max(0, Math.min(selection, newLength - 4))); // -4 để né chữ " VNĐ"
                });
            } catch (NumberFormatException e) {
                editor.setText(oldValue);
            }
        });

        // 2. Tự động commit giá trị khi click ra ngoài (mất focus)
        editor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                Double value = new CurrencyStringConverter().fromString(editor.getText());
                spinner.getValueFactory().setValue(value);
            }
        });
    }
}