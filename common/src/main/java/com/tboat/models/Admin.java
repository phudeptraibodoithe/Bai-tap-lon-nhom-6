package com.tboat.models;

public class Admin extends Person {

    public Admin(String accountName, String password, String nickname) {
        super(accountName,password,nickname);
    }

    public Admin(String accountName, String password, String nickname,double balance) {
        super(accountName, password, nickname, balance);
    }

    public void censorSession(AuctionSession session) {
        // Code duyệt sản phẩm
        System.out.println("The product has been approved!");
    }

    public void ban(User user) {
        // Code ban tài khoản
        System.out.println("The account has been banned: " + user.getAccountName());
    }
}