package com.tboat.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager auctionManager;

    @BeforeEach
    void setUp() {
        // Lấy instance trước mỗi test
        auctionManager = AuctionManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        // Xóa phòng để không ảnh hưởng đến test khác
        auctionManager.removeRoom(101);
        auctionManager.removeRoom(102);
    }

    @Test
    void testSingletonInstance() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();

        // Cả 2 biến phải trỏ về cùng một vùng nhớ
        assertSame(instance1, instance2, "AuctionManager phải là Singleton, chỉ có 1 instance duy nhất!");
    }

    @Test
    void testCreateAndGetRoom() {
        int sessionId = 101;
        double initialPrice = 50000.0;

        auctionManager.createRoom(sessionId, initialPrice);
        AuctionRoom room = auctionManager.getRoom(sessionId);

        assertNotNull(room, "Phòng đấu giá phải tồn tại sau khi tạo.");
        assertEquals(sessionId, room.getSessionId(), "Session ID phải khớp.");
        assertEquals(initialPrice, room.getCurrentPrice(), "Giá khởi điểm phải khớp.");
    }

    @Test
    void testRemoveRoom() {
        int sessionId = 102;
        auctionManager.createRoom(sessionId, 100000.0);

        // Đảm bảo phòng đã được tạo
        assertNotNull(auctionManager.getRoom(sessionId));

        // Xóa phòng và kiểm tra lại
        auctionManager.removeRoom(sessionId);
        assertNull(auctionManager.getRoom(sessionId), "Phòng đấu giá phải bị xóa khỏi hệ thống.");
    }
}