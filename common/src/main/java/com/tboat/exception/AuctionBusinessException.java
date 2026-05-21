package com.tboat.exception;

public class AuctionBusinessException extends RuntimeException {
    private final String errorCode;

    public AuctionBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}