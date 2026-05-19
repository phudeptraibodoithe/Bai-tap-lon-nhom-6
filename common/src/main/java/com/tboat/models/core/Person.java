package com.tboat.models.core;

public abstract class Person {
    // Attributes
    protected String accountName;
    protected String password;
    protected String nickname;
    protected double balance;
    private String email;
    private String phone;

    // Constructor
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

    // Setters
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public void setBalance(double balance) {
        this.balance = balance;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    // Getters
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
    public String getEmail() {
        return email;
    }
    public String getPhone() {
        return phone;
    }
}