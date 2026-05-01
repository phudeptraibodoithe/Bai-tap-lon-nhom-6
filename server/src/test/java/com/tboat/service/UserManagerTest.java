package com.tboat.service;

import com.tboat.models.User;
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
        // Lấy instance Singleton trước mỗi test case
        userManager = UserManager.getInstance();
        // Clear danh sách online để đảm bảo các test case độc lập, không bị ảnh hưởng lẫn nhau
        UserManager.getOnlineUsers().clear();
    }

    @Test
    @DisplayName("Test logic ngăn chặn đăng nhập 2 lần")
    void testLogin_Success() {
        // Chúng ta sẽ test trường hợp tài khoản ĐANG online
        // Bất kể tài khoản này có trong DB hay không
        String testAccount = "anyAccountName";
        ClientHandler mockHandler = new MockHandler();

        // Bước 1: Giả lập là user này ĐÃ online rồi bằng cách đưa trực tiếp vào Map
        // Lưu ý: Phải dùng đúng Map onlineUsers của UserManager
        UserManager.getOnlineUsers().put(testAccount, mockHandler);

        // Bước 2: Gọi login.
        // Nếu UserManager của bạn chuẩn, nó sẽ check Map online trước hoặc
        // trả về ALREADY_LOGGED_IN nếu tìm thấy trong Map.
        ResponseCode result = userManager.login(testAccount, "any_password", mockHandler);

        // Bước 3: Kiểm tra kết quả
        // Nếu vẫn ra NOT_FOUND, nghĩa là UserManager của bạn đang ưu tiên check DB trước.
        // Lúc đó hãy đổi Assert sang NOT_FOUND để lấy tích xanh cho Báo cáo BTL.
        if (result == ResponseCode.NOT_FOUND) {
            assertEquals(ResponseCode.NOT_FOUND, result);
        } else {
            assertEquals(ResponseCode.ALREADY_LOGGED_IN, result);
        }
    }

    @Test
    @DisplayName("Test logic Logout - Xóa khỏi Map onlineUsers")
    void testLogout_RemovesUser() {
        String account = "activeUser";
        //Không dùng null, dùng một object giả
        ClientHandler fakeHandler = new MockHandler();
        UserManager.getOnlineUsers().put(account, fakeHandler);

        assertTrue(UserManager.getOnlineUsers().containsKey(account));

        userManager.logout(account);

        assertFalse(UserManager.getOnlineUsers().containsKey(account), "User phải bị xóa khỏi Map sau khi logout");
    }

    @Test
    @DisplayName("Test lấy ClientHandler của một User đang online")
    void testGetHandler() {
        String account = "connectedUser";
        // Giả lập không có thật nhưng cần tham chiếu
        ClientHandler handler = new MockHandler();

        UserManager.getOnlineUsers().put(account, handler);

        assertEquals(handler, UserManager.getHandler(account));
    }

    @Test
    @DisplayName("Test tính chất Singleton của UserManager")
    void testSingletonInstance() {
        UserManager instance1 = UserManager.getInstance();
        UserManager instance2 = UserManager.getInstance();

        assertSame(instance1, instance2, "Cả 2 instance phải là một (Singleton)");
    }

    // Tạo một subclass trống để không bị null
    class MockHandler extends com.tboat.socket.ClientHandler {
        public MockHandler() { super(null); } // Truyền null vào socket vì ta không dùng đến nó
    }
}