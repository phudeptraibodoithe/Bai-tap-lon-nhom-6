package com.tboat.models; // Đảm bảo đúng package của bạn

public class Request<T> {
    private String action;
    private T payload; // Đây là nơi chứa dữ liệu (User, AuctionSession, Double...)

    public Request() {}

    public Request(String action, T payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    // Phương thức mà ClientHandler đang báo lỗi thiếu:
    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}