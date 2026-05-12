package com.tboat.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager auctionManager;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        auctionManager.removeRoom(101);
        auctionManager.removeRoom(102);
        auctionManager.removeRoom(103);
    }

    // ===================== SINGLETON =====================

    @Test
    @DisplayName("AuctionManager phải là Singleton - 2 lần getInstance() trả về cùng object")
    void testSingletonInstance() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();
        assertSame(instance1, instance2, "AuctionManager phải là Singleton, chỉ có 1 instance duy nhất!");
    }

    // ===================== CREATE ROOM =====================

    @Test
    @DisplayName("Tạo phòng mới và lấy lại phải thành công")
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
    @DisplayName("Tạo phòng trùng ID thì phải giữ nguyên phòng cũ (putIfAbsent)")
    void testCreateRoomDuplicate_ShouldKeepOriginal() {
        int sessionId = 103;
        double firstPrice = 100000.0;
        double secondPrice = 999999.0;

        auctionManager.createRoom(sessionId, firstPrice);
        auctionManager.createRoom(sessionId, secondPrice); // Tạo lại cùng ID

        AuctionRoom room = auctionManager.getRoom(sessionId);
        assertNotNull(room);
        // putIfAbsent: phòng đầu tiên phải được giữ nguyên, không bị ghi đè
        assertEquals(firstPrice, room.getCurrentPrice(), "Phòng cũ phải được giữ nguyên khi tạo trùng ID.");
    }

    // ===================== GET ROOM =====================

    @Test
    @DisplayName("getRoom với ID không tồn tại phải trả về null")
    void testGetRoom_NotExist_ShouldReturnNull() {
        AuctionRoom room = auctionManager.getRoom(99999);
        assertNull(room, "getRoom với ID không tồn tại phải trả về null.");
    }

    // ===================== REMOVE ROOM =====================

    @Test
    @DisplayName("Xóa phòng thành công thì getRoom phải trả về null")
    void testRemoveRoom() {
        int sessionId = 102;
        auctionManager.createRoom(sessionId, 100000.0);
        assertNotNull(auctionManager.getRoom(sessionId));

        auctionManager.removeRoom(sessionId);
        assertNull(auctionManager.getRoom(sessionId), "Phòng đấu giá phải bị xóa khỏi hệ thống.");
    }

    @Test
    @DisplayName("Xóa phòng không tồn tại không được throw Exception")
    void testRemoveRoom_NotExist_ShouldNotThrow() {
        // removeRoom ID không tồn tại không được crash
        assertDoesNotThrow(() -> auctionManager.removeRoom(99999),
                "Xóa phòng không tồn tại không được throw Exception.");
    }

    // ===================== TRẠNG THÁI PHÒNG =====================

    @Test
    @DisplayName("Phòng mới tạo: giá ban đầu đúng, lastBidder null, chưa kết thúc")
    void testNewRoom_InitialState() {
        int sessionId = 101;
        double initialPrice = 75000.0;

        auctionManager.createRoom(sessionId, initialPrice);
        AuctionRoom room = auctionManager.getRoom(sessionId);

        assertNotNull(room);
        assertEquals(initialPrice, room.getCurrentPrice());
        assertNull(room.getLastBidder(), "Phòng mới chưa có người đặt giá.");
        assertFalse(room.isFinished(), "Phòng mới chưa kết thúc.");
    }
}