package com.tboat.models;

public class Request<T> {
    private String action;
    private T payload;

    public Request(String action, T payload) {
        this.action = action;
        this.payload = payload;
    }
}