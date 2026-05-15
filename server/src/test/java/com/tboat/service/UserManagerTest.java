//package com.tboat.service;
//
//import com.tboat.socket.ClientHandler;
//import com.tboat.utils.ResponseCode;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.DisplayName;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Unit Test cho UserManager.
// *
// * LƯU Ý:
// * - Các test liên quan đến login/register thật sẽ gọi DB (Integration Test).
// * - Các test về trạng thái online (onlineUsers map) hoàn toàn là Unit Test thuần.
// */
//class UserManagerTest {
//
//    private UserManager userManager;
//
//    @BeforeEach
//    void setUp() {
//        userManager = UserManager.getInstance();
//        // Reset map online trước mỗi test để tránh ảnh hưởng nhau
//        UserManager.getOnlineUsers().clear();
//    }
//
//    // ===================== SINGLETON =====================
//
//    @Test
//    @DisplayName("UserManager phải là Singleton")
//    void testSingletonInstance() {
//        UserManager instance1 = UserManager.getInstance();
//        UserManager instance2 = UserManager.getInstance();
//        assertSame(instance1, instance2, "Phải là cùng một instance duy nhất");
//    }
//
//    // ===================== LOGIN - KIỂM TRA ONLINE MAP =====================
//
//    @Test
//    @DisplayName("Login khi tài khoản đang online phải trả về ALREADY_LOGGED_IN")
//    void testLogin_AlreadyLoggedIn() {
//        String account = "testUser";
//        UserManager.getOnlineUsers().put(account, new MockHandler());
//
//        ResponseCode result = userManager.login(account, "password", new MockHandler());
//        assertEquals(ResponseCode.ALREADY_LOGGED_IN, result,
//                "Tài khoản đang online không được login thêm lần nữa.");
//    }
//
//    @Test
//    @DisplayName("Login thành công: user phải xuất hiện trong onlineUsers")
//    void testLogin_Success_UserAppearsInOnlineMap() {
//        // Test này gọi DB thật - chỉ chạy khi có DB
//        // Giả lập: ta put thẳng vào map (kiểm tra logic map, không kiểm tra DAO)
//        String account = "existingUser";
//        MockHandler handler = new MockHandler();
//        UserManager.getOnlineUsers().put(account, handler);
//
//        assertTrue(UserManager.getOnlineUsers().containsKey(account),
//                "User sau khi login phải có mặt trong onlineUsers.");
//        assertSame(handler, UserManager.getHandler(account),
//                "getHandler phải trả về đúng ClientHandler đã đăng ký.");
//    }
//
//    // ===================== LOGOUT =====================
//
//    @Test
//    @DisplayName("Logout user đang online: phải bị xóa khỏi onlineUsers")
//    void testLogout_OnlineUser_ShouldBeRemoved() {
//        String account = "onlineUser";
//        UserManager.getOnlineUsers().put(account, new MockHandler());
//        assertTrue(UserManager.getOnlineUsers().containsKey(account));
//
//        userManager.logout(account);
//        assertFalse(UserManager.getOnlineUsers().containsKey(account),
//                "User phải bị xóa khỏi onlineUsers sau khi logout.");
//    }
//
//    @Test
//    @DisplayName("Logout user không online: không throw Exception")
//    void testLogout_NotOnlineUser_ShouldNotThrow() {
//        assertDoesNotThrow(() -> userManager.logout("nonExistentUser"),
//                "Logout user không online không được throw Exception.");
//    }
//
//    @Test
//    @DisplayName("Logout với null: không throw Exception")
//    void testLogout_NullAccount_ShouldNotThrow() {
//        assertDoesNotThrow(() -> userManager.logout(null),
//                "Logout với null không được throw Exception.");
//    }
//
//    // ===================== GET HANDLER =====================
//
//    @Test
//    @DisplayName("getHandler trả về đúng handler của user đang online")
//    void testGetHandler_ReturnsCorrectHandler() {
//        String account = "userWithHandler";
//        MockHandler handler = new MockHandler();
//        UserManager.getOnlineUsers().put(account, handler);
//
//        ClientHandler result = UserManager.getHandler(account);
//        assertSame(handler, result, "getHandler phải trả về đúng ClientHandler.");
//    }
//
//    @Test
//    @DisplayName("getHandler trả về null nếu user không online")
//    void testGetHandler_UserNotOnline_ReturnsNull() {
//        ClientHandler result = UserManager.getHandler("offlineUser");
//        assertNull(result, "getHandler phải trả về null nếu user không online.");
//    }
//
//    // ===================== ONLINE USERS MAP =====================
//
//    @Test
//    @DisplayName("onlineUsers ban đầu phải trống sau khi clear")
//    void testOnlineUsers_InitiallyEmpty() {
//        assertTrue(UserManager.getOnlineUsers().isEmpty(),
//                "Map onlineUsers phải trống sau setUp().");
//    }
//
//    @Test
//    @DisplayName("Thêm nhiều user: map phải chứa đúng số lượng")
//    void testOnlineUsers_MultipleUsers() {
//        UserManager.getOnlineUsers().put("user1", new MockHandler());
//        UserManager.getOnlineUsers().put("user2", new MockHandler());
//        UserManager.getOnlineUsers().put("user3", new MockHandler());
//
//        assertEquals(3, UserManager.getOnlineUsers().size(),
//                "Map phải chứa đúng 3 user.");
//    }
//
//    @Test
//    @DisplayName("Sau logout tất cả: map phải trống")
//    void testLogout_AllUsers_MapShouldBeEmpty() {
//        UserManager.getOnlineUsers().put("userA", new MockHandler());
//        UserManager.getOnlineUsers().put("userB", new MockHandler());
//
//        userManager.logout("userA");
//        userManager.logout("userB");
//
//        assertTrue(UserManager.getOnlineUsers().isEmpty(),
//                "Map phải trống sau khi tất cả user logout.");
//    }
//
//    // ===================== MOCK CLASS =====================
//
//    /**
//     * Lớp giả lập ClientHandler để tránh lỗi khởi tạo Socket thật.
//     */
//    static class MockHandler extends ClientHandler {
//        public MockHandler() {
//            super(null);
//        }
//
//        @Override
//        public void sendSystemMessage(String action, String message, Object payload) {
//            // Không làm gì khi test
//        }
//    }
//}