package com.tboat.service;

import com.tboat.socket.ClientContext;
import org.junit.jupiter.api.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AuctionRoom.
 *
 * Chiến lược:
 * - Test trạng thái (state) và subscribe/unsubscribe: KHÔNG cần DB.
 * - finishAuction gọi DB (getAuctionById) — ta wrap try/catch để vẫn
 *   kiểm tra được flag isFinished, vì isFinished = true được set TRƯỚC
 *   mọi thao tác DB theo đúng implementation.
 * - placeBid với phòng đã kết thúc: kiểm tra guard đầu method, KHÔNG cần DB.
 */
class AuctionRoomTest {

    private AuctionRoom room;

    @BeforeEach
    void setUp() {
        room = new AuctionRoom(101, 500.0);
        // Đảm bảo room 101 có trong AuctionManager để removeRoom bên trong
        // finishAuction không gây lỗi
        AuctionManager.getInstance().createRoom(101, 500.0);
    }

    @AfterEach
    void tearDown() {
        AuctionManager.getInstance().removeRoom(101);
    }

    // ===================== TRẠNG THÁI KHỞI TẠO =====================

    @Test
    @DisplayName("Khởi tạo: sessionId phải đúng")
    void testInit_SessionId() {
        assertEquals(101, room.getSessionId());
    }

    @Test
    @DisplayName("Khởi tạo: giá khởi điểm phải đúng")
    void testInit_CurrentPrice() {
        assertEquals(500.0, room.getCurrentPrice());
    }

    @Test
    @DisplayName("Khởi tạo: lastBidder phải là null")
    void testInit_LastBidderIsNull() {
        assertNull(room.getLastBidder());
    }

    @Test
    @DisplayName("Khởi tạo: isFinished phải là false")
    void testInit_IsFinishedFalse() {
        assertFalse(room.isFinished());
    }

    // ===================== FINISH AUCTION =====================

    @Test
    @DisplayName("finishAuction: isFinished phải được set true ngay (trước DB call)")
    void testFinishAuction_SetsIsFinishedTrue() {
        assertFalse(room.isFinished());
        try {
            room.finishAuction();
        } catch (RuntimeException ignored) {
            // DB không khả dụng trong môi trường test thuần — bỏ qua.
            // isFinished = true được set ở DÒNG ĐẦU TIÊN của method,
            // trước bất kỳ thao tác DB nào.
        }
        assertTrue(room.isFinished(),
                "isFinished phải là true ngay sau khi gọi finishAuction().");
    }

    @Test
    @DisplayName("finishAuction gọi lần 2: phải return ngay, không throw Exception")
    void testFinishAuction_CalledTwice_Idempotent() {
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
        assertTrue(room.isFinished());

        // Lần 2: guard `if (isFinished) return;` ở dòng đầu — không chạm DB
        assertDoesNotThrow(() -> room.finishAuction(),
                "Gọi finishAuction lần 2 không được throw Exception.");
        assertTrue(room.isFinished(), "isFinished vẫn phải là true.");
    }

    @Test
    @DisplayName("finishAuction gọi 10 lần liên tiếp: không throw Exception")
    void testFinishAuction_CalledManyTimes_NoThrow() {
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
        for (int i = 0; i < 9; i++) {
            assertDoesNotThrow(() -> room.finishAuction());
        }
        assertTrue(room.isFinished());
    }

    // ===================== PLACE BID — KHÔNG CẦN DB =====================

    @Test
    @DisplayName("placeBid vào phòng đã finished: phải trả về false ngay (guard đầu method)")
    void testPlaceBid_RoomFinished_ReturnsFalse() {
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
        assertTrue(room.isFinished());

        // Guard `if (isFinished) return false;` không gọi BiddingService → không cần DB
        boolean result = room.placeBid(9_999.0, "alice");
        assertFalse(result, "Phòng đã kết thúc phải từ chối mọi bid.");
    }

    @Test
    @DisplayName("placeBid với giá 0 vào phòng finished: false")
    void testPlaceBid_ZeroPrice_RoomFinished_ReturnsFalse() {
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
        assertFalse(room.placeBid(0.0, "alice"));
    }

    @Test
    @DisplayName("placeBid với giá âm vào phòng finished: false")
    void testPlaceBid_NegativePrice_RoomFinished_ReturnsFalse() {
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
        assertFalse(room.placeBid(-100.0, "alice"));
    }

    @Test
    @DisplayName("placeBid với account null vào phòng finished: false, không throw")
    void testPlaceBid_NullAccount_RoomFinished_NoThrow() {
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
        assertDoesNotThrow(() -> {
            boolean result = room.placeBid(1_000.0, null);
            assertFalse(result);
        });
    }

    @Test
    @DisplayName("placeBid vào phòng finished nhiều lần: tất cả đều false")
    void testPlaceBid_MultipleAttempts_RoomFinished_AllFalse() {
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
        assertFalse(room.placeBid(1_000.0, "user1"));
        assertFalse(room.placeBid(2_000.0, "user2"));
        assertFalse(room.placeBid(3_000.0, "user3"));
    }

    @Test
    @DisplayName("currentPrice không thay đổi sau các bid bị từ chối (phòng finished)")
    void testPlaceBid_Rejected_PriceUnchanged() {
        double originalPrice = room.getCurrentPrice();
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
        room.placeBid(99_999.0, "alice");
        assertEquals(originalPrice, room.getCurrentPrice(),
                "Giá không được thay đổi khi bid bị từ chối.");
    }

    @Test
    @DisplayName("lastBidder không thay đổi sau bid bị từ chối (phòng finished)")
    void testPlaceBid_Rejected_LastBidderUnchanged() {
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
        room.placeBid(99_999.0, "alice");
        // lastBidder ban đầu null, sau bid bị từ chối vẫn phải null
        assertNull(room.getLastBidder());
    }

    // ===================== SUBSCRIBER =====================

    @Test
    @DisplayName("addSubscriber với ClientContext hợp lệ: không throw Exception")
    void testAddSubscriber_ValidContext_NoThrow() {
        ClientContext ctx = new ClientContext();
        ctx.setOut(new PrintWriter(new StringWriter(), true));
        assertDoesNotThrow(() -> room.addSubscriber(ctx));
    }

    @Test
    @DisplayName("removeSubscriber với context chưa thêm vào: không throw Exception")
    void testRemoveSubscriber_NotAdded_NoThrow() {
        ClientContext ctx = new ClientContext();
        assertDoesNotThrow(() -> room.removeSubscriber(ctx));
    }

    @Test
    @DisplayName("addSubscriber rồi removeSubscriber: không throw Exception")
    void testAddThenRemoveSubscriber_NoThrow() {
        ClientContext ctx = new ClientContext();
        ctx.setOut(new PrintWriter(new StringWriter(), true));
        assertDoesNotThrow(() -> {
            room.addSubscriber(ctx);
            room.removeSubscriber(ctx);
        });
    }

    @Test
    @DisplayName("addSubscriber null: không throw Exception (CopyOnWriteArrayList cho phép null)")
    void testAddSubscriber_Null_NoThrow() {
        assertDoesNotThrow(() -> room.addSubscriber(null));
    }

    @Test
    @DisplayName("removeSubscriber null: không throw Exception")
    void testRemoveSubscriber_Null_NoThrow() {
        assertDoesNotThrow(() -> room.removeSubscriber(null));
    }

    // ===================== BROADCAST — PHÒNG TRỐNG =====================

    @Test
    @DisplayName("broadcast khi không có subscriber: không throw Exception")
    void testBroadcast_NoSubscribers_NoThrow() {
        assertDoesNotThrow(
                () -> room.broadcast("TEST_ACTION", "test message", null),
                "broadcast vào phòng trống không được crash."
        );
    }

    @Test
    @DisplayName("broadcast với payload null: không throw Exception")
    void testBroadcast_NullPayload_NoThrow() {
        assertDoesNotThrow(() -> room.broadcast("ACTION", "msg", null));
    }

    @Test
    @DisplayName("broadcast với action null: không throw Exception")
    void testBroadcast_NullAction_NoThrow() {
        assertDoesNotThrow(() -> room.broadcast(null, "msg", null));
    }

    // ===================== GETTER CHUẨN =====================

    @Test
    @DisplayName("getSessionId trả về đúng giá trị được truyền vào constructor")
    void testGetSessionId_Correct() {
        AuctionRoom r = new AuctionRoom(42, 1_000.0);
        assertEquals(42, r.getSessionId());
    }

    @Test
    @DisplayName("getCurrentPrice trả về đúng giá khởi điểm")
    void testGetCurrentPrice_Correct() {
        AuctionRoom r = new AuctionRoom(1, 123.45);
        assertEquals(123.45, r.getCurrentPrice());
    }

    @Test
    @DisplayName("2 AuctionRoom khác nhau có state độc lập")
    void testTwoRooms_IndependentState() {
        AuctionRoom r1 = new AuctionRoom(201, 1_000.0);
        AuctionRoom r2 = new AuctionRoom(202, 2_000.0);

        try { r1.finishAuction(); } catch (RuntimeException ignored) {}

        assertTrue(r1.isFinished(), "r1 phải finished.");
        assertFalse(r2.isFinished(), "r2 không bị ảnh hưởng bởi r1.");
    }
}