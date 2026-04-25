package com.tboat.utilsclient;

public class UserSession {
    private static UserSession instance;

    private String username;
    private String nickname;
    private double balance;

    // Private constructor để không ai 'new' lung tung được
    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // Hàm này gọi ngay khi nhận được LOGIN_SUCCESS từ Server
    public void createUserSession(String username, String nickname, double balance) {
        this.username = username;
        this.nickname = nickname;
        this.balance = balance;
    }

    // Hàm xóa session khi Logout
    public void cleanUserSession() {
        username = null;
        nickname = null;
        balance = 0.0;
    }

    // Các hàm Getter để lấy dữ liệu ở màn hình khác
    public String getNickname() { return nickname; }
    public double getBalance() { return balance; }
}