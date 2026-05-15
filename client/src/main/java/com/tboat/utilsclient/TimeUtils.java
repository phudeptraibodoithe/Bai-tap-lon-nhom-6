package com.tboat.utilsclient;

import com.google.gson.JsonElement;
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

public class TimeUtils {

    private static final DateTimeFormatter STANDARD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Ép kiểu thời gian từ Server (timestamp hoặc chuỗi ISO) về chuẩn hiển thị
    public static String parseServerTime(JsonElement timeElement) {
        if (timeElement == null || timeElement.isJsonNull()) return "";
        try {
            if (timeElement.isJsonPrimitive() && timeElement.getAsJsonPrimitive().isNumber()) {
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timeElement.getAsLong()),
                        ZoneId.systemDefault()
                );
                return dateTime.format(STANDARD_FORMATTER);
            } else {
                String time = timeElement.getAsString();
                return time.contains("T") ? time.replace("T", " ").substring(0, 19) : time;
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static void setupTimeSpinner(Spinner<Integer> spinner, int min, int max, int initValue, String suffix) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initValue);
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return (value == null) ? "00" + suffix : String.format("%02d%s", value, suffix);
            }
            @Override
            public Integer fromString(String string) {
                try {
                    if (string == null || string.isEmpty()) return 0;
                    return Integer.parseInt(string.replace(suffix, "").trim());
                } catch (Exception e) { return 0; }
            }
        });
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
        // Commit giá trị khi click ra ngoài
        spinner.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) spinner.increment(0);
        });
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
    // Thêm các hàm khác như format thời gian cho label, tính toán thời gian còn lại...
}