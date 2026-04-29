package com.tboat.socket;

import com.tboat.socket.SocketManager;

public interface SocketListener {
    // Phương thức bắt buộc phải thực thi để xử lý phản hồi từ server
    void handleServerResponse(String response);

    // Đăng ký nhận thông báo
    default void registerSocket() {
        SocketManager.getInstance().subscribe(this);
    }

    // Hủy đăng ký (Cực kỳ quan trọng khi chuyển Scene để tránh rò rỉ bộ nhớ)
    default void unregisterSocket() {
        SocketManager.getInstance().unsubscribe(this);
    }
}