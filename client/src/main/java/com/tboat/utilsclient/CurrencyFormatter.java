package com.tboat.utilsclient;

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
}