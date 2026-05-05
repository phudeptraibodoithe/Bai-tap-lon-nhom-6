package com.tboat.utils;

public enum ResponseCode {
    // --- Nhóm hệ thống & Tài khoản ---
    SUCCESS("Thao tác thành công"),
    ERROR("Lỗi hệ thống không xác định"),
    EXISTED("Tài khoản đã tồn tại"),
    NOT_FOUND("Không tìm thấy tài khoản"),
    WRONG_PASSWORD("Mật khẩu không chính xác"),
    ALREADY_LOGGED_IN("Tài khoản đang đăng nhập ở nơi khác");


    private final String message;

    ResponseCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}