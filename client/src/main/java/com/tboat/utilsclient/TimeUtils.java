package com.tboat.utilsclient;

import com.google.gson.JsonElement;
import com.tboat.controllers.HomeController;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class TimeUtils {

    private static final DateTimeFormatter STANDARD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger logger = Logger.getLogger(TimeUtils.class.getName());
    private static final int ISO_LOCAL_MINUTE_LENGTH = 16;
    private static final int ISO_LOCAL_SECOND_LENGTH = 19;
    private static final int DEFAULT_SPINNER_VALUE = 0;
    private static final String ZERO_PADDED_TIME_UNIT = "00";

    // Ép kiểu thời gian từ Server (timestamp hoặc chuỗi ISO) về chuẩn hiển thị
    // Ép kiểu thời gian từ Server (timestamp hoặc chuỗi) về chuẩn LocalDateTime của Java
    public static LocalDateTime parseServerTime(JsonElement timeElement) {
        if (timeElement == null || timeElement.isJsonNull()) return null;
        try {
            // Trường hợp 1: Server trả về Timestamp (kiểu số long)
            if (timeElement.isJsonPrimitive() && timeElement.getAsJsonPrimitive().isNumber()) {
                return LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timeElement.getAsLong()),
                        ZoneId.systemDefault()
                );
            } else {
                // Trường hợp 2: Server trả về chuỗi
                String timeStr = timeElement.getAsString().replace(" ", "T");

                // Nếu chuỗi bị thiếu giây (VD: 2026-05-16T17:22 - dài 16 ký tự), tự động bù thêm ":00"
                if (timeStr.length() == ISO_LOCAL_MINUTE_LENGTH) {
                    timeStr += ":00";
                }

                // Cắt đi phần thừa nếu chuỗi dài hơn chuẩn (chống lỗi OutOfBounds)
                if (timeStr.length() > ISO_LOCAL_SECOND_LENGTH) {
                    timeStr = timeStr.substring(0, ISO_LOCAL_SECOND_LENGTH);
                }

                return LocalDateTime.parse(timeStr);
            }
        } catch (Exception e) {
            logger.severe("Lỗi parse thời gian từ Server: " + e.getMessage() + " - Dữ liệu gốc: " + timeElement.toString());
            return null;
        }
    }

    public static void setupTimeSpinner(Spinner<Integer> spinner, int min, int max, int initValue, String suffix) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initValue);
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return (value == null) ? ZERO_PADDED_TIME_UNIT + suffix : String.format("%02d%s", value, suffix);
            }
            @Override
            public Integer fromString(String string) {
                try {
                    if (string == null || string.isEmpty()) return DEFAULT_SPINNER_VALUE;
                    return Integer.parseInt(string.replace(suffix, "").trim());
                } catch (Exception e) { return DEFAULT_SPINNER_VALUE; }
            }
        });
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
        spinner.getEditor().setOnAction(event -> commitTimeSpinner(spinner));
        // Commit giá trị khi click ra ngoài, không dùng increment(0) để tránh nhảy số.
        spinner.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) commitTimeSpinner(spinner);
        });
    }

    private static void commitTimeSpinner(Spinner<Integer> spinner) {
        if (spinner == null || spinner.getValueFactory() == null) return;
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
        int value = factory.getConverter().fromString(spinner.getEditor().getText());
        value = Math.max(factory.getMin(), Math.min(value, factory.getMax()));
        factory.setValue(value);
        spinner.getEditor().setText(factory.getConverter().toString(value));
    }

    // --- ĐÃ THÊM: Đóng gói Logic ràng buộc DatePicker ---
    public static void setupDatePickers(DatePicker startDatePicker, DatePicker endDatePicker) {
        LocalDate today = LocalDate.now();

        // Khóa ngày trong quá khứ cho Ngày bắt đầu
        startDatePicker.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(today)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee;");
                }
            }
        });

        // Ràng buộc Ngày kết thúc phải >= Ngày bắt đầu
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                endDatePicker.setDayCellFactory(p -> new DateCell() {
                    @Override public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item.isBefore(newVal)) {
                            setDisable(true);
                            setStyle("-fx-background-color: #eeeeee;");
                        }
                    }
                });
                if (endDatePicker.getValue() != null && endDatePicker.getValue().isBefore(newVal)) {
                    endDatePicker.setValue(null); // Reset nếu không hợp lệ
                }
            }
        });
    }

    public static String formatTimeDisplay(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(STANDARD_FORMATTER);
    }
}
