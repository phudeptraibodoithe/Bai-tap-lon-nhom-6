package com.tboat.session;

import com.tboat.models.core.User;

public class UserSession {
    private static UserSession instance;
    private User currentUser;
    private static final double GUEST_BALANCE = 0.0;

    // Private constructor để ngăn chặn khởi tạo từ bên ngoài
    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Khởi tạo phiên làm việc khi đăng nhập thành công.
     */
    public void createUserSession(User user) {
        this.currentUser = user;
    }

    public void cleanUserSession() {
        this.currentUser = null;
    }

    public User getUser() {
        return currentUser;
    }

    public String getUsername() {
        return (currentUser != null) ? currentUser.getAccountName() : null;
    }

    public double getBalance() {
        return (currentUser != null) ? currentUser.getBalance() : GUEST_BALANCE;
    }
}
