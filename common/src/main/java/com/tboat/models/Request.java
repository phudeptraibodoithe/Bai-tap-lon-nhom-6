package com.tboat.models; // Đảm bảo đúng package của bạn

public class Request<T> {
    private String action;
    private T payload;

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

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}