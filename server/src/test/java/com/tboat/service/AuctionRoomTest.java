package com.tboat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class AuctionRoomTest {
    private AuctionRoom room;

    @BeforeEach
    void setUp() {
        // SỬA: ID giờ là kiểu int (101 thay vì "101")
        room = new AuctionRoom(101, 500.0);
    }

    @Test
    void testInitialState() {
        assertEquals(101, room.getSessionId());
        assertEquals(500.0, room.getCurrentPrice());
        assertNull(room.getLastBidder());
        assertFalse(room.isFinished());
    }

    /**
     * LƯU Ý: testPlaceBid hiện tại sẽ gọi vào BiddingService và DAO thực tế.
     * Nếu bạn chưa cấu hình Database cho Test, test này có thể bị fail.
     * Đây là "Integration Test" (Kiểm thử tích hợp).
     */
    @Test
    void testAuctionFinishState() {
        assertFalse(room.isFinished(), "Phòng mới tạo không được ở trạng thái kết thúc");
        room.finishAuction();
        assertTrue(room.isFinished(), "Phòng phải chuyển sang trạng thái finish sau khi gọi hàm");
    }
}