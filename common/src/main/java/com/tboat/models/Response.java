package com.tboat.models;

public class Response<T> {
    private String status;
    private String message;
    private T payload;

    public Response(String status, String message, T payload) {
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    // Thêm các Getter/Setter nếu cần
}