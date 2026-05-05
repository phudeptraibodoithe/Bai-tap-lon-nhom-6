package com.tboat.utilsclient;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class CurrencyFormatter {
    private static final DecimalFormat formatter;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        formatter = new DecimalFormat("#,###", symbols);
    }

    public static String format(Double value) {
        if (value == null || value == 0.0) {
            return "0 VNĐ";
        }
        return formatter.format(value) + " VNĐ";
    }

    public static Double parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        try {
            String cleanString = text.replaceAll("[^\\d.]", "");
            if (cleanString.isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(cleanString);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}