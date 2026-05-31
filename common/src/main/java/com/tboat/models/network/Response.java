package com.tboat.models.network;

public class Response<T> {
    private String status;
    private String message;
    private T payload;
    private String type; // Thêm trường này để phân biệt loại phản hồi

    public Response(String type, String status, String message, T payload) {
        this.type    = type;
        this.status  = status;
        this.message = message;
        this.payload = payload;
    }

    public Response(ServerEvent type, ServerEvent status, String message, T payload) {
        this(type.name(), status.name(), message, payload);
    }

    public Response(String status, String message, T payload) {
        this.status  = status;
        this.message = message;
        this.payload = payload;
    }

    public Response(ServerEvent status, String message, T payload) {
        this(status.name(), message, payload);
    }

    public String getType() {
        return type;
    }
}
