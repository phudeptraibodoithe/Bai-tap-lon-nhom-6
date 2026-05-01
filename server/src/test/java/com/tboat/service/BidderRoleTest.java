package com.tboat.service;

import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BidderRoleTest {
    private BidderRole bidderRole;
    private User testUser;
    private AuctionSession testSession;

    @BeforeEach
    void setUp() {
        bidderRole = new BidderRole();
        // Giả lập User có 1,000,000 VND
        testUser = new User("tester", "pass", "Nick", 1000000.0, "Desc", "avatar.jpg");
        // Giả lập Phiên đấu giá đang diễn ra, giá hiện tại 100, bước giá 10
        testSession = new AuctionSession(1, "Sản phẩm test", 100.0, 10.0, "seller1");
        testSession.setStatusOfAuction(StatusOfAuction.ONGOING);
    }

    @Test
    void testExecute_Success() {
        // Đặt giá 200,000 (Hợp lệ)
        boolean result = bidderRole.execute(testUser, testSession, 200000.0);

        assertTrue(result);
        assertEquals(200000.0, testSession.getCurrentPrice());
        assertEquals("tester", testSession.getHighestBidderAccount());
    }

    @Test
    void testExecute_InvalidStatus() {
        testSession.setStatusOfAuction(StatusOfAuction.ENDED);
        boolean result = bidderRole.execute(testUser, testSession, 200000.0);

        assertFalse(result, "Không được phép bid khi phiên đã kết thúc");
    }

    @Test
    void testExecute_InsufficientBalance() {
        // Bid 2,000,000 trong khi chỉ có 1,000,000
        boolean result = bidderRole.execute(testUser, testSession, 2000000.0);

        assertFalse(result, "Không được phép bid vượt quá số dư tài khoản");
    }

    @Test
    void testExecute_BidTooLow() {
        // Giá hiện tại 100, bước giá 10 -> Tối thiểu phải bid 110. Thử bid 105.
        boolean result = bidderRole.execute(testUser, testSession, 105.0);

        assertFalse(result, "Phải bid lớn hơn hoặc bằng (Giá hiện tại + Bước giá)");
    }
}