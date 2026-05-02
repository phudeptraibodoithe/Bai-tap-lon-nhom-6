package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidderRoleTest {
    private BidderRole bidderRole;
    private User testUser;
    private AuctionSession testSession;

    // Các đối tượng DAO giả lập (Mock)
    private UserDAO mockUserDAO;
    private AuctionSessionDAO mockSessionDAO;
    private HistoryBidDAO mockBidDAO;

    @BeforeEach
    void setUp() {
        bidderRole = new BidderRole();

        // 1. Khởi tạo User giả lập (Số dư 1,000,000)
        testUser = new User("tester", "pass", "Nick", 1000000.0, "Desc", "avatar.jpg");

        // 2. Khởi tạo Phiên đấu giá giả lập (Giá hiện tại 100, bước giá 10)
        testSession = new AuctionSession(
                1, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                100.0, 10.0, StatusOfAuction.ONGOING, "seller1",
                "Laptop", "Dell XPS", "Like new", "url_img", null
        );

        // 3. Khởi tạo các DAO "giả" để không bị NullPointerException
        // Chúng ta override các hàm thực thi DB để chúng luôn trả về true mà không cần DB thật
        mockUserDAO = new UserDAO() {
            @Override public boolean updateBalance(String account, double amount) { return true; }
        };

        mockSessionDAO = new AuctionSessionDAO() {
            @Override public boolean updateSessionPriceAndHighest(int id, String bidder, double price) { return true; }
        };

        mockBidDAO = new HistoryBidDAO() {
            @Override public boolean addBid(int sessionId, String bidder, double price) { return true; }
        };
    }

    @Test
    void testExecute_Success() {
        // Đặt giá 200,000 (Hợp lệ)
        // Truyền đủ 6 tham số theo đúng thứ tự trong code của bạn
        boolean result = bidderRole.execute(testUser, testSession, 200000.0, mockUserDAO, mockSessionDAO, mockBidDAO);

        assertTrue(result, "Lệnh đặt giá phải thành công");
        assertEquals(200000.0, testSession.getCurrentPrice(), "Giá trên RAM phải cập nhật lên 200k");
        assertEquals("tester", testSession.getHighestBidderAccount(), "Người dẫn đầu phải là tester");
        assertEquals(800000.0, testUser.getBalance(), "Số dư User trên RAM phải còn 800k");
    }

    @Test
    void testExecute_InsufficientBalance() {
        // Bid 2,000,000 trong khi chỉ có 1,000,000 -> Phải fail ở Bước 1
        boolean result = bidderRole.execute(testUser, testSession, 2000000.0, mockUserDAO, mockSessionDAO, mockBidDAO);

        assertFalse(result, "Không được phép đặt giá vượt quá số dư tài khoản");
        assertEquals(100.0, testSession.getCurrentPrice(), "Giá không được thay đổi");
    }

    @Test
    void testExecute_BidTooLow() {
        // Giá hiện tại 100, bước giá 10 -> Tối thiểu phải bid 110. Thử bid 105.
        boolean result = bidderRole.execute(testUser, testSession, 105.0, mockUserDAO, mockSessionDAO, mockBidDAO);

        assertFalse(result, "Phải bid lớn hơn hoặc bằng (Giá hiện tại + Bước giá)");
    }

    @Test
    void testExecute_InvalidStatus() {
        // Giả lập phiên đấu giá đã kết thúc
        testSession.setStatusOfAuction(StatusOfAuction.ENDED);

        boolean result = bidderRole.execute(testUser, testSession, 200000.0, mockUserDAO, mockSessionDAO, mockBidDAO);

        assertFalse(result, "Không được phép đặt giá khi phiên đã kết thúc");
    }
}