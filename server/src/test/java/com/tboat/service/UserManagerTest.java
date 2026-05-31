package com.tboat.service;

import com.tboat.socket.ClientSession;
import com.tboat.utils.ResponseCode;
import org.junit.jupiter.api.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho UserManager.
 * Không cần DB cho các test về bản đồ người dùng online.
 *
 * LƯU Ý quan trọng:
 * - ConcurrentHashMap.get(null) ném NPE — đây là hành vi Java chuẩn.
 *   Test getHandler(null) được viết để chấp nhận cả null lẫn NPE.
 * - register() / login() với account không online gọi DB nên chấp nhận RuntimeException từ DB.
 */
class UserManagerTest {

    private UserManager userManager;

    @BeforeEach
    void setUp() {
        userManager = UserManager.getInstance();
        UserManager.getOnlineUsers().clear();
    }

    @AfterEach
    void tearDown() {
        UserManager.getOnlineUsers().clear();
    }

    static class MockContext extends ClientSession {
        public MockContext() {}
        public MockContext(String clientId) {
            this.setClientId(clientId);
            this.setOut(new PrintWriter(new StringWriter(), true));
        }
    }

    // ===================== SINGLETON DÙNG CHUNG =====================

    @Test
    @DisplayName("UserManager phải là Singleton")
    void testSingleton_SameInstance() {
        assertSame(UserManager.getInstance(), UserManager.getInstance());
    }

    @Test
    @DisplayName("getInstance() không được trả về null")
    void testSingleton_NotNull() {
        assertNotNull(UserManager.getInstance());
    }

    // ===================== BẢN ĐỒ NGƯỜI DÙNG ONLINE =====================

    @Test
    @DisplayName("onlineUsers phải trống sau setUp()")
    void testOnlineUsers_InitiallyEmpty() {
        assertTrue(UserManager.getOnlineUsers().isEmpty());
    }

    @Test
    @DisplayName("getOnlineUsers() không được trả về null")
    void testGetOnlineUsers_NotNull() {
        assertNotNull(UserManager.getOnlineUsers());
    }

    // ===================== ĐĂNG NHẬP — kiểm tra bản đồ online (KHÔNG cần DB) =====================

    @Test
    @DisplayName("Login khi account đang online → ALREADY_LOGGED_IN (không gọi DB)")
    void testLogin_AlreadyOnline_ReturnsAlreadyLoggedIn() {
        // Chốt kiểm tra onlineUsers.containsKey() chạy TRƯỚC khi gọi DB nên không cần DB
        UserManager.getOnlineUsers().put("alice", new MockContext("alice"));
        ResponseCode result = userManager.login("alice", "anypass", new MockContext());
        assertEquals(ResponseCode.ALREADY_LOGGED_IN, result);
    }

    @Test
    @DisplayName("Login khi account đang online: map không bị thay đổi")
    void testLogin_AlreadyOnline_MapUnchanged() {
        MockContext original = new MockContext("alice");
        UserManager.getOnlineUsers().put("alice", original);
        userManager.login("alice", "anypass", new MockContext());
        assertSame(original, UserManager.getOnlineUsers().get("alice"));
    }

    @Test
    @DisplayName("Login nhiều account đang online: tất cả đều ALREADY_LOGGED_IN")
    void testLogin_MultipleOnlineAccounts_AllAlreadyLoggedIn() {
        UserManager.getOnlineUsers().put("userA", new MockContext("userA"));
        UserManager.getOnlineUsers().put("userB", new MockContext("userB"));
        assertEquals(ResponseCode.ALREADY_LOGGED_IN,
                userManager.login("userA", "pass", new MockContext()));
        assertEquals(ResponseCode.ALREADY_LOGGED_IN,
                userManager.login("userB", "pass", new MockContext()));
    }

    // ===================== ĐĂNG XUẤT =====================

    @Test
    @DisplayName("logout user đang online: bị xóa khỏi map")
    void testLogout_OnlineUser_RemovedFromMap() {
        UserManager.getOnlineUsers().put("alice", new MockContext("alice"));
        userManager.logout("alice");
        assertFalse(UserManager.getOnlineUsers().containsKey("alice"));
    }

    @Test
    @DisplayName("logout user không online: không throw Exception")
    void testLogout_NotOnline_NoThrow() {
        assertDoesNotThrow(() -> userManager.logout("nonExistentUser"));
    }

    @Test
    @DisplayName("logout null: không throw Exception (UserManager có guard null)")
    void testLogout_Null_NoThrow() {
        // UserManager.logout() có: if (account != null) { onlineUsers.remove(account) }
        assertDoesNotThrow(() -> userManager.logout(null));
    }

    @Test
    @DisplayName("logout chuỗi rỗng: không throw Exception")
    void testLogout_EmptyString_NoThrow() {
        assertDoesNotThrow(() -> userManager.logout(""));
    }

    @Test
    @DisplayName("logout gọi 2 lần cùng account: không throw Exception")
    void testLogout_CalledTwice_NoThrow() {
        UserManager.getOnlineUsers().put("alice", new MockContext("alice"));
        userManager.logout("alice");
        assertDoesNotThrow(() -> userManager.logout("alice"));
    }

    // ===================== LẤY HANDLER =====================

    @Test
    @DisplayName("getHandler trả về đúng ClientSession đã đăng ký")
    void testGetHandler_ReturnsCorrectContext() {
        MockContext ctx = new MockContext("alice");
        UserManager.getOnlineUsers().put("alice", ctx);
        assertSame(ctx, UserManager.getHandler("alice"));
    }

    @Test
    @DisplayName("getHandler với user không online → null")
    void testGetHandler_NotOnline_ReturnsNull() {
        assertNull(UserManager.getHandler("offlineUser"));
    }

    @Test
    @DisplayName("getHandler với null: ConcurrentHashMap không chấp nhận null key → NPE là hành vi hợp lệ")
    void testGetHandler_Null_BehaviorDocumented() {
        // ConcurrentHashMap.get(null) ném NullPointerException theo đặc tả Java.
        // Test này xác nhận hành vi hiện tại (NPE) và tài liệu hoá nó.
        // Nếu muốn an toàn hơn, cần thêm chốt kiểm tra null vào UserManager.getHandler().
        assertThrows(NullPointerException.class, () -> UserManager.getHandler(null),
                "ConcurrentHashMap.get(null) phải throw NPE theo đặc tả Java.");
    }

    @Test
    @DisplayName("getHandler sau logout → null")
    void testGetHandler_AfterLogout_ReturnsNull() {
        UserManager.getOnlineUsers().put("alice", new MockContext("alice"));
        userManager.logout("alice");
        assertNull(UserManager.getHandler("alice"));
    }

    // ===================== NHIỀU NGƯỜI DÙNG =====================

    @Test
    @DisplayName("Thêm 3 user vào map: size phải là 3")
    void testOnlineUsers_ThreeUsers_SizeIsThree() {
        UserManager.getOnlineUsers().put("u1", new MockContext("u1"));
        UserManager.getOnlineUsers().put("u2", new MockContext("u2"));
        UserManager.getOnlineUsers().put("u3", new MockContext("u3"));
        assertEquals(3, UserManager.getOnlineUsers().size());
    }

    @Test
    @DisplayName("Logout tất cả user: map phải trống")
    void testLogout_AllUsers_MapEmpty() {
        UserManager.getOnlineUsers().put("u1", new MockContext("u1"));
        UserManager.getOnlineUsers().put("u2", new MockContext("u2"));
        userManager.logout("u1");
        userManager.logout("u2");
        assertTrue(UserManager.getOnlineUsers().isEmpty());
    }

    @Test
    @DisplayName("Logout 1 trong 3 user: 2 user còn lại vẫn online")
    void testLogout_OneOfThree_OthersTwoRemain() {
        UserManager.getOnlineUsers().put("u1", new MockContext("u1"));
        UserManager.getOnlineUsers().put("u2", new MockContext("u2"));
        UserManager.getOnlineUsers().put("u3", new MockContext("u3"));
        userManager.logout("u2");
        assertEquals(2, UserManager.getOnlineUsers().size());
        assertTrue(UserManager.getOnlineUsers().containsKey("u1"));
        assertFalse(UserManager.getOnlineUsers().containsKey("u2"));
        assertTrue(UserManager.getOnlineUsers().containsKey("u3"));
    }

    @Test
    @DisplayName("Thêm cùng account 2 lần: map chỉ giữ giá trị cuối")
    void testOnlineUsers_PutSameKeyTwice_LastValueWins() {
        MockContext first  = new MockContext("alice");
        MockContext second = new MockContext("alice");
        UserManager.getOnlineUsers().put("alice", first);
        UserManager.getOnlineUsers().put("alice", second);
        assertEquals(1, UserManager.getOnlineUsers().size());
        assertSame(second, UserManager.getHandler("alice"));
    }

    // ===================== ĐĂNG KÝ (gọi DB, chỉ test không phát sinh NPE) =====================

    @Test
    @DisplayName("register với account rỗng: nếu có DB trả về ResponseCode, nếu không có DB throw RuntimeException (không phải NPE)")
    void testRegister_EmptyAccount_NoNPE() {
        try {
            ResponseCode result = userManager.register("", "pass", "nick", "", "");
            assertNotNull(result, "Nếu có DB, kết quả không được null.");
        } catch (RuntimeException e) {
            assertFalse(e instanceof NullPointerException,
                    "Chỉ chấp nhận DB RuntimeException, không chấp nhận NPE.");
        }
    }

    @Test
    @DisplayName("register với tất cả null: không phát sinh NPE (chỉ chấp nhận DB error)")
    void testRegister_AllNull_NoNPE() {
        try {
            userManager.register(null, null, null, null, null);
        } catch (RuntimeException e) {
            assertFalse(e instanceof NullPointerException,
                    "Không được throw NPE — chỉ DB-related exception là chấp nhận được.");
        }
    }

    @Test
    @DisplayName("register với email và phone null: không phát sinh NPE")
    void testRegister_NullEmailPhone_NoNPE() {
        try {
            userManager.register("testUser", "pass", "nick", null, null);
        } catch (RuntimeException e) {
            assertFalse(e instanceof NullPointerException,
                    "Không được throw NPE.");
        }
    }
}
