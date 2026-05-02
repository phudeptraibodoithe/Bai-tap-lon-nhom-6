package com.tboat.models;

public abstract class Person {
    // Attributes
    protected String accountName;
    protected String password;
    protected String nickname;
    protected double balance;

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
}