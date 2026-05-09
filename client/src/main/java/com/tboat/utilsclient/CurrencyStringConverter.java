package com.tboat.utilsclient;

import javafx.util.StringConverter;

public class CurrencyStringConverter extends StringConverter<Double> {

    @Override
    public String toString(Double value) {
        return CurrencyFormatter.formatDisplay(value);
    }

    @Override
    public Double fromString(String string) {
        return CurrencyFormatter.parse(string);
    }
}