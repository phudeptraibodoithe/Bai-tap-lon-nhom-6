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
                // Trường hợp 2: Server trả về chuỗi String
                String timeStr = timeElement.getAsString().replace(" ", "T");

                // Nếu chuỗi bị thiếu giây (VD: 2026-05-16T17:22 - dài 16 ký tự), tự động bù thêm ":00"
                if (timeStr.length() == 16) {
                    timeStr += ":00";
                }

                // Cắt đi phần thừa nếu chuỗi dài hơn chuẩn (chống lỗi OutOfBounds)
                if (timeStr.length() > 19) {
                    timeStr = timeStr.substring(0, 19);
                }

                return LocalDateTime.parse(timeStr);
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse thời gian từ Server: " + e.getMessage() + " - Dữ liệu gốc: " + timeElement.toString());
            return null; // Trả về null để Controller tự dùng fallback (như LocalDateTime.now())
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

    public static String formatTimeDisplay(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(STANDARD_FORMATTER);
    }
}