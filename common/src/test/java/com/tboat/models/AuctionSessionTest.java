package com.tboat.models;

import com.tboat.exception.AuctionBusinessException;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.item.Item;
import com.tboat.models.item.OtherItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionSessionTest {

    private AuctionSession session;
    private LocalDateTime start;
    private LocalDateTime end;
    private Item item;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.now().plusHours(1);
        end   = LocalDateTime.now().plusHours(3);
        item  = new OtherItem("seller01", "Test Item", "Test Description", "https://img/test.jpg");
        item.setId(101);

        session = new AuctionSession(start, end, 500.0, 10.0, item);
        session.setId(1);
    }

    // ── Kiểm tra hàm khởi tạo ────────────────────────────────────────────────

    @Test
    @DisplayName("currentPrice âm trong constructor → throw ERR_SESSION_01")
    void testConstructor_NegativeCurrentPrice_Throws() {
        AuctionBusinessException ex = assertThrows(AuctionBusinessException.class,
                () -> new AuctionSession(start, end, -1.0, 10.0, item));
        assertEquals("ERR_SESSION_01", ex.getErrorCode());
    }

    @Test
    @DisplayName("bidIncrease = 0 trong constructor → throw ERR_SESSION_02")
    void testConstructor_ZeroBidIncrease_Throws() {
        AuctionBusinessException ex = assertThrows(AuctionBusinessException.class,
                () -> new AuctionSession(start, end, 100.0, 0.0, item));
        assertEquals("ERR_SESSION_02", ex.getErrorCode());
    }

    @Test
    @DisplayName("bidIncrease âm trong constructor → throw ERR_SESSION_02")
    void testConstructor_NegativeBidIncrease_Throws() {
        AuctionBusinessException ex = assertThrows(AuctionBusinessException.class,
                () -> new AuctionSession(start, end, 100.0, -5.0, item));
        assertEquals("ERR_SESSION_02", ex.getErrorCode());
    }

    @Test
    @DisplayName("currentPrice = 0 hợp lệ → không throw")
    void testConstructor_ZeroCurrentPrice_Valid() {
        assertDoesNotThrow(() -> new AuctionSession(start, end, 0.0, 10.0, item));
    }

    // ── Hàm truy xuất cơ bản ─────────────────────────────────────────────────

    @Test
    @DisplayName("getId() trả đúng id đã set")
    void testGetId() {
        assertEquals(1, session.getId());
    }

    @Test
    @DisplayName("getItemId() tự sync từ Item.id khi khởi tạo")
    void testGetItemId_SyncedFromItem() {
        assertEquals(101, session.getItemId());
    }

    @Test
    @DisplayName("getStartTime() đúng")
    void testGetStartTime() {
        assertEquals(start, session.getStartTime());
    }

    @Test
    @DisplayName("getEndTime() đúng")
    void testGetEndTime() {
        assertEquals(end, session.getEndTime());
    }

    @Test
    @DisplayName("getCurrentPrice() đúng")
    void testGetCurrentPrice() {
        assertEquals(500.0, session.getCurrentPrice(), 0.001);
    }

    @Test
    @DisplayName("getBidIncrease() đúng")
    void testGetBidIncrease() {
        assertEquals(10.0, session.getBidIncrease(), 0.001);
    }

    @Test
    @DisplayName("getHighestBidderAccount() ban đầu là null")
    void testGetHighestBidderAccount_InitiallyNull() {
        assertNull(session.getHighestBidderAccount());
    }

    @Test
    @DisplayName("getBuyNowPrice() ban đầu là 0.0")
    void testGetBuyNowPrice_InitiallyZero() {
        assertEquals(0.0, session.getBuyNowPrice(), 0.001);
    }

    @Test
    @DisplayName("getSellerAccountName() lấy từ Item")
    void testGetSellerAccountName() {
        assertEquals("seller01", session.getSellerAccountName());
    }

    @Test
    @DisplayName("getDescription() lấy từ Item")
    void testGetDescription() {
        assertEquals("Test Description", session.getDescription());
    }

    // ── Hàm cập nhật ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("setCurrentPrice() hợp lệ → cập nhật đúng")
    void testSetCurrentPrice_Valid() {
        session.setCurrentPrice(600.0);
        assertEquals(600.0, session.getCurrentPrice(), 0.001);
    }

    @Test
    @DisplayName("setCurrentPrice() âm → throw ERR_SESSION_01")
    void testSetCurrentPrice_Negative_Throws() {
        AuctionBusinessException ex = assertThrows(AuctionBusinessException.class,
                () -> session.setCurrentPrice(-1.0));
        assertEquals("ERR_SESSION_01", ex.getErrorCode());
    }

    @Test
    @DisplayName("setStatusOfAuction() → cập nhật đúng")
    void testSetStatus() {
        session.setStatusOfAuction(StatusOfAuction.ONGOING);
        assertEquals(StatusOfAuction.ONGOING, session.getStatusOfAuction());
    }

    @Test
    @DisplayName("setHighestBidderAccount() → cập nhật đúng")
    void testSetHighestBidderAccount() {
        session.setHighestBidderAccount("user01");
        assertEquals("user01", session.getHighestBidderAccount());
    }

    @Test
    @DisplayName("setBuyNowPrice() → cập nhật đúng")
    void testSetBuyNowPrice() {
        session.setBuyNowPrice(9_999.0);
        assertEquals(9_999.0, session.getBuyNowPrice(), 0.001);
    }

    @Test
    @DisplayName("setEndTime() → cập nhật đúng")
    void testSetEndTime() {
        LocalDateTime newEnd = end.plusHours(2);
        session.setEndTime(newEnd);
        assertEquals(newEnd, session.getEndTime());
    }

    @Test
    @DisplayName("setSellerAccountName() → cập nhật trong Item")
    void testSetSellerAccountName() {
        session.setSellerAccountName("newSeller");
        assertEquals("newSeller", session.getSellerAccountName());
    }

    @Test
    @DisplayName("endTime sau startTime")
    void testEndTimeAfterStartTime() {
        assertTrue(session.getEndTime().isAfter(session.getStartTime()));
    }

    // ── updateStatusBasedOnTime() các nhánh ──────────────────────────────────

    @Test
    @DisplayName("updateStatusBasedOnTime(): start tương lai → NOT_STARTED")
    void testUpdateStatus_NotStarted() {
        AuctionSession s = new AuctionSession(
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3),
                100.0, 10.0, item);
        assertEquals(StatusOfAuction.NOT_STARTED, s.getStatusOfAuction());
    }

    @Test
    @DisplayName("updateStatusBasedOnTime(): đang diễn ra → ONGOING")
    void testUpdateStatus_Ongoing() {
        AuctionSession s = new AuctionSession(
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                100.0, 10.0, item);
        assertEquals(StatusOfAuction.ONGOING, s.getStatusOfAuction());
    }

    @Test
    @DisplayName("updateStatusBasedOnTime(): đã qua → ENDED")
    void testUpdateStatus_Ended() {
        AuctionSession s = new AuctionSession(
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusHours(1),
                100.0, 10.0, item);
        assertEquals(StatusOfAuction.ENDED, s.getStatusOfAuction());
    }

    @Test
    @DisplayName("updateStatusBasedOnTime(): startTime null → không throw, status giữ nguyên")
    void testUpdateStatus_NullTimes_NoThrow() {
        AuctionSession s = new AuctionSession(); // hàm khởi tạo rỗng
        assertDoesNotThrow(s::updateStatusBasedOnTime);
        assertNull(s.getStatusOfAuction()); // chưa set gì → null
    }
}
