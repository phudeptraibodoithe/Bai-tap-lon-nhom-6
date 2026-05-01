package com.tboat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class AuctionRoomTest {
    private AuctionRoom room;

    @BeforeEach
    void setUp() {
        // Khởi tạo phòng với giá khởi điểm 500
        room = new AuctionRoom("101", 500.0);
    }

    @Test
    void testPlaceBid_ValidIncrementsPrice() {
        boolean result = room.placeBid(600.0, "user1");

        assertTrue(result);
        assertEquals(600.0, room.getCurrentPrice());
        assertEquals("user1", room.getLastBidder());
    }

    @Test
    void testPlaceBid_LowerPriceFails() {
        room.placeBid(600.0, "user1");
        boolean result = room.placeBid(550.0, "user2");

        assertFalse(result, "Giá bid mới phải cao hơn giá hiện tại");
        assertEquals(600.0, room.getCurrentPrice());
    }

    @Test
    void testAuctionFinishState() {
        assertFalse(room.isFinished(), "Phòng mới tạo không được ở trạng thái kết thúc");
    }
}