package com.tboat.models.core;

public abstract class Person {
    // Thuộc tính
    private String accountName;
    private String password;
    private String nickname;
    private double balance;

    // Hàm khởi tạo
    public Person(String accountName, String password, String nickname) {
        this.accountName = accountName;
        this.password = password;
        this.nickname = nickname;
        this.balance = 0.0;
    }

    public Person(String accountName, String password, String nickname,double balance) {
        this.accountName = accountName;
        this.password = password;
        this.nickname = nickname;
        this.balance = balance;
    }

    public Person(){}

    // Hàm cập nhật
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public void setBalance(double balance) {
        this.balance = balance;
    }

    // Hàm truy xuất
    public String getAccountName() {
        return accountName;
    }
    public String getNickname() {
        return nickname;
    }
    public double getBalance() {
        return balance;
    }
    public String getPassword() {
        return password;
    }
}
