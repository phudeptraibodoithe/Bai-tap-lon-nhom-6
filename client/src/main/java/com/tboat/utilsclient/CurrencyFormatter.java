package com.tboat.utilsclient;

import javafx.application.Platform;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyFormatter {
    private static final double ZERO_AMOUNT = 0.0;
    private static final double MIN_CURRENCY_VALUE = 0.0;
    private static final double MAX_CURRENCY_VALUE = 1e18;

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
        if (text == null || text.trim().isEmpty()) return ZERO_AMOUNT;
        try {
            String cleanString = text.replaceAll("[^\\d]", "");
            return cleanString.isEmpty() ? ZERO_AMOUNT : Double.parseDouble(cleanString);
        } catch (Exception e) {
            return ZERO_AMOUNT;
        }
    }

    // --- CÁC HÀM FORMAT ---
    public static String formatInput(Double value) {
        if (value == null || value == ZERO_AMOUNT) return "";
        return inputFormat.format(value);
    }

    public static String formatDisplay(Double value) {
        if (value == null || value == ZERO_AMOUNT) return "0 VNĐ";
        return displayFormat.format(value);
    }

    // --- BỘ LẮNG NGHE CHO TEXTFIELD ---
    public static void attachCurrencyListener(TextField textField) {
        if (textField == null) return;

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            // 👉 Đã tối ưu: Tái sử dụng hàm parse
            Double parsed = parse(newValue);
            if (parsed == ZERO_AMOUNT && !newValue.matches(".*\\d.*")) {
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

    // --- THIẾT LẬP CHO SPINNER ---
    public static void setupCurrencySpinner(Spinner<Double> spinner, double initialValue, double step) {
        SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        MIN_CURRENCY_VALUE, MAX_CURRENCY_VALUE, initialValue, step);

        factory.setConverter(new CurrencyStringConverter());
        spinner.setValueFactory(factory);
        spinner.setEditable(true);

        TextField editor = spinner.getEditor();

        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!editor.isFocused()) {
                editor.setText(formatInput(newValue));
            }
        });

        editor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                editor.setText(formatInput(spinner.getValue()));
                Platform.runLater(() -> editor.positionCaret(editor.getText().length()));
            }
        });

        editor.setOnAction(event -> commitCurrencySpinner(spinner));

        // Ép ghi nhận giá trị khi click ra ngoài, không dùng increment(0) để tránh nhảy số.
        editor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                commitCurrencySpinner(spinner);
            }
        });
    }

    private static void commitCurrencySpinner(Spinner<Double> spinner) {
        TextField editor = spinner.getEditor();
        SpinnerValueFactory<Double> valueFactory = spinner.getValueFactory();
        if (valueFactory == null) return;

        double parsed = parse(editor.getText());
        double value = Math.max(MIN_CURRENCY_VALUE, Math.min(parsed, MAX_CURRENCY_VALUE));
        valueFactory.setValue(value);
        editor.setText(formatInput(value));
        Platform.runLater(() -> editor.positionCaret(editor.getText().length()));
    }

}
