package com.tboat.models.network; // Đảm bảo đúng gói của dự án

public class Request<T> {
    private String action;
    private T payload;

    public Request() {}

    public Request(String action, T payload) {
        this.action = action;
        this.payload = payload;
    }

    public Request(ServerEvent action, T payload) {
        this(action.name(), payload);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setAction(ServerEvent action) {
        this.action = action.name();
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
