package com.tboat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AuctionRoom.
 *
 * LƯU Ý QUAN TRỌNG:
 * - testPlaceBid* sẽ gọi BiddingService -> DAO -> Database thực tế (Integration Test).
 *   Nếu chưa cấu hình DB cho môi trường Test, các test đó sẽ bị FAIL.
 * - Các test về trạng thái (state) không cần DB và luôn chạy được.
 */
class AuctionRoomTest {

    private AuctionRoom room;

    @BeforeEach
    void setUp() {
        room = new AuctionRoom(101, 500.0);
    }

    // ===================== TRẠNG THÁI KHỞI TẠO =====================

    @Test
    @DisplayName("Phòng mới tạo: ID, giá, lastBidder, isFinished đều đúng")
    void testInitialState() {
        assertEquals(101, room.getSessionId(), "SessionId phải đúng");
        assertEquals(500.0, room.getCurrentPrice(), "Giá khởi điểm phải đúng");
        assertNull(room.getLastBidder(), "Chưa có ai đặt giá -> lastBidder phải null");
        assertFalse(room.isFinished(), "Phòng mới tạo chưa được kết thúc");
    }

    // ===================== FINISH AUCTION =====================

    @Test
    @DisplayName("finishAuction: phòng phải chuyển sang trạng thái finished")
    void testAuctionFinishState() {
        assertFalse(room.isFinished(), "Phòng mới tạo không được ở trạng thái kết thúc");
        room.finishAuction();
        assertTrue(room.isFinished(), "Phòng phải chuyển sang trạng thái finish sau khi gọi hàm");
    }

    @Test
    @DisplayName("finishAuction gọi 2 lần: lần 2 không thực thi lại (idempotent)")
    void testFinishAuction_CalledTwice_ShouldBeIdempotent() {
        room.finishAuction();
        assertTrue(room.isFinished());

        // Gọi lần 2 không được throw Exception hay thay đổi trạng thái
        assertDoesNotThrow(() -> room.finishAuction(),
                "Gọi finishAuction lần 2 không được throw Exception.");
        assertTrue(room.isFinished(), "Phòng vẫn phải ở trạng thái finished.");
    }

    // ===================== PLACE BID - LOGIC KHÔNG CẦN DB =====================

    @Test
    @DisplayName("placeBid khi phòng đã finished phải trả về false ngay lập tức")
    void testPlaceBid_WhenRoomIsFinished_ShouldReturnFalse() {
        room.finishAuction(); // Chốt phòng
        assertTrue(room.isFinished());

        // Đặt giá vào phòng đã kết thúc phải trả về false (không gọi DB)
        boolean result = room.placeBid(9999.0, "userA");
        assertFalse(result, "Không thể đặt giá vào phòng đã kết thúc.");
    }

    @Test
    @DisplayName("placeBid với giá 0 vào phòng đã finished: trả về false")
    void testPlaceBid_ZeroPrice_WhenFinished_ShouldReturnFalse() {
        room.finishAuction();
        boolean result = room.placeBid(0.0, "userA");
        assertFalse(result, "Phòng đã kết thúc, mọi bid đều phải trả về false.");
    }

    // ===================== GETTER / SETTER TRẠNG THÁI =====================

    @Test
    @DisplayName("getSessionId trả về đúng giá trị")
    void testGetSessionId() {
        assertEquals(101, room.getSessionId());
    }

    @Test
    @DisplayName("getCurrentPrice trả về đúng giá khởi điểm")
    void testGetCurrentPrice() {
        assertEquals(500.0, room.getCurrentPrice());
    }

    @Test
    @DisplayName("getLastBidder trả về null khi chưa có ai đặt giá")
    void testGetLastBidder_InitiallyNull() {
        assertNull(room.getLastBidder());
    }

    // ===================== SUBSCRIBER =====================

    @Test
    @DisplayName("addSubscriber và removeSubscriber không throw Exception")
    void testAddAndRemoveSubscriber_ShouldNotThrow() {
        assertDoesNotThrow(() -> {
            room.addSubscriber(null); // null handler (môi trường test không có socket thật)
            room.removeSubscriber(null);
        }, "addSubscriber/removeSubscriber với null không được throw Exception.");
    }
}