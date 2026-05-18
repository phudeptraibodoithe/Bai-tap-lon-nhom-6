package com.tboat.controllers;

public class BidEntry {
    private final String time;
    private final String user;
    private final double price;

    public BidEntry(String time, String user, double price) {
        this.time  = time;
        this.user  = user;
        this.price = price;
    }

    public String getTime()  { return time; }
    public String getUser()  { return user; }
    public double getPrice() { return price; }
}