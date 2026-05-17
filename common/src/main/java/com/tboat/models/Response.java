package com.tboat.models;

public class Response<T> {
    private String status;
    private String message;
    private T payload;
    private String type; // THÊM FIELD NÀY

    public Response(String type, String status, String message, T payload) {
        this.type    = type;
        this.status  = status;
        this.message = message;
        this.payload = payload;
    }
}