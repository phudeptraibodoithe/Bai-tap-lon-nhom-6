package com.tboat.utils;

public enum ResponseCode {
    SUCCESS("Thao tác thành công"),
    EXISTED("Tài khoản đã tồn tại"),
    NOT_FOUND("Không tìm thấy tài khoản"),
    WRONG_PASSWORD("Mật khẩu không chính xác"),
    ERROR("Lỗi hệ thống không xác định"),
    INVALID_INPUT("Dữ liệu nhập vào không hợp lệ");

    private final String message;

    // Constructor để gán thông báo mặc định cho mỗi mã
    ResponseCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}