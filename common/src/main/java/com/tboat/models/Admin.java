package com.tboat.models;

public class Admin extends Person {

    public Admin(String accountName, String password, String nickname) {
        super(accountName,password,nickname);
    }

    public Admin(String accountName, String password, String nickname,double balance) {
        super(accountName, password, nickname, balance);
    }
}