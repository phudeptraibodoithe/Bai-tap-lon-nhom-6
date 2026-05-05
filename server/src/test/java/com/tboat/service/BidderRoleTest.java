package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection; // Nhớ import thằng này nhé bác!
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidderRoleTest {
    private BidderRole bidderRole;
    private User testUser;
    private AuctionSession testSession;

    private UserDAO mockUserDAO;
    private AuctionSessionDAO mockSessionDAO;
    private HistoryBidDAO mockBidDAO;

    @BeforeEach
    void setUp() {
        bidderRole = new BidderRole();

        // 1. Khởi tạo User (Số dư 1,000,000)
        testUser = new User("tester", "pass", "Nick", 1000000.0, "Desc", "avatar.jpg");

        // 2. Khởi tạo Session
        testSession = new AuctionSession(
                1, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                100.0, 10.0, StatusOfAuction.ONGOING, "seller1",
                "Laptop", "Dell XPS", "Like new", "url_img", null
        );

        // 3. Khởi tạo DAO giả lập CHUẨN (Có Connection)
        mockUserDAO = new UserDAO() {
            @Override
            public boolean updateBalance(Connection conn, String account, double amount) {
                if (testUser.getAccountName().equals(account)) {
                    // Mô phỏng DB check số dư: Nếu âm tiền thì fail
                    if (testUser.getBalance() + amount < 0) return false;
                    // Đủ tiền thì trừ trên RAM để Assert kiểm tra
                    testUser.setBalance(testUser.getBalance() + amount);
                }
                return true;
            }
        };

        mockSessionDAO = new AuctionSessionDAO() {
            @Override
            public boolean updateSessionPriceAndHighest(Connection conn, int id, String bidder, double price) {
                // Mô phỏng DB: Cập nhật object RAM để Assert pass
                testSession.setCurrentPrice(price);
                testSession.setHighestBidderAccount(bidder);
                return true;
            }
        };

        mockBidDAO = new HistoryBidDAO() {
            @Override
            public boolean addBid(Connection conn, int sessionId, String bidder, double price) {
                return true;
            }
        };
    }

    @Test
    void testExecute_Success() {
        // Đặt giá 200,000. Hàm BidderRole mong đợi amount > 0, khi truyền vào userDAO sẽ bị chuyển thành số âm để trừ tiền
        boolean result = bidderRole.execute(testUser, testSession, 200000.0, mockUserDAO, mockSessionDAO, mockBidDAO);

        assertTrue(result, "Lệnh đặt giá phải thành công");
        assertEquals(200000.0, testSession.getCurrentPrice(), "Giá trên RAM phải cập nhật lên 200k");
        assertEquals("tester", testSession.getHighestBidderAccount(), "Người dẫn đầu phải là tester");
        assertEquals(800000.0, testUser.getBalance(), "Số dư User trên RAM phải còn 800k");
    }

    @Test
    void testExecute_InsufficientBalance() {
        // Bid 2,000,000 trong khi chỉ có 1,000,000 -> Phải fail ở Bước trừ tiền
        boolean result = bidderRole.execute(testUser, testSession, 2000000.0, mockUserDAO, mockSessionDAO, mockBidDAO);

        assertFalse(result, "Không được phép đặt giá vượt quá số dư tài khoản");
        assertEquals(100.0, testSession.getCurrentPrice(), "Giá không được thay đổi");
    }
}