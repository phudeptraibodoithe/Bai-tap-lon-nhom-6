package com.tboat.service;

import com.tboat.socket.ClientHandler;
import com.tboat.utils.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {
    private UserManager userManager;

    @BeforeEach
    void setUp() {
        userManager = UserManager.getInstance();
        // Đảm bảo map online trống trước mỗi test
        UserManager.getOnlineUsers().clear();
    }

    @Test
    @DisplayName("Test tính chất Singleton của UserManager")
    void testSingletonInstance() {
        UserManager instance1 = UserManager.getInstance();
        UserManager instance2 = UserManager.getInstance();
        assertSame(instance1, instance2, "Phải là cùng một instance duy nhất");
    }

    @Test
    @DisplayName("Test đăng nhập khi tài khoản đã online")
    void testLogin_AlreadyLoggedIn() {
        String account = "testUser";
        ClientHandler mockHandler = new MockHandler();

        // Giả lập user đã online
        UserManager.getOnlineUsers().put(account, mockHandler);

        ResponseCode result = userManager.login(account, "password", new MockHandler());
        assertEquals(ResponseCode.ALREADY_LOGGED_IN, result);
    }

    // Lớp giả lập để tránh lỗi khởi tạo Socket
    class MockHandler extends ClientHandler {
        public MockHandler() { super(null); }
        @Override public void sendSystemMessage(String action, String message, Object payload) {
            // Không làm gì cả khi chạy test
        }
    }
}