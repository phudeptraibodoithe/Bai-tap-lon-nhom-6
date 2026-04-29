package com.tboat.utils;

public enum ResponseCode {
    // --- Nhóm hệ thống & Tài khoản ---
    SUCCESS("Thao tác thành công"),
    ERROR("Lỗi hệ thống không xác định"),
    EXISTED("Tài khoản đã tồn tại"),
    NOT_FOUND("Không tìm thấy tài khoản"),
    WRONG_PASSWORD("Mật khẩu không chính xác"),
    ALREADY_LOGGED_IN("Tài khoản đang đăng nhập ở nơi khác"),
    INVALID_INPUT("Dữ liệu nhập vào không hợp lệ"),

    // --- Nhóm Đấu giá (Cần thêm để đồng bộ logic mới) ---
    INVALID_PRICE("Giá đặt phải cao hơn giá hiện tại cộng với bước giá"),
    INSUFFICIENT_BALANCE("Số dư tài khoản không đủ để thực hiện đặt giá"), // Dùng cho lỗi image_88c9a1.jpg
    AUCTION_ENDED("Phiên đấu giá đã kết thúc"),
    AUCTION_NOT_STARTED("Phiên đấu giá chưa bắt đầu"),
    BID_FAILED("Đặt giá thất bại, vui lòng thử lại");

    private final String message;

    ResponseCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}