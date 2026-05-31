package com.tboat.service;

import com.tboat.models.auction.BidResult;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.ClientSession;
import org.junit.jupiter.api.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho AuctionRoom.
 *
 * Chiến lược:
 * - Test trạng thái và đăng ký/hủy đăng ký theo dõi: KHÔNG cần DB.
 * - finishAuction gọi DB, bọc try/catch và kiểm tra isFinished vì cờ
 *   được gán ở DÒNG ĐẦU TIÊN trước mọi thao tác DB.
 * - placeBid vào phòng đã kết thúc: chốt kiểm tra đầu method trả DB_ERROR, KHÔNG cần DB.
 *   Các trường hợp khác (PRICE_TOO_LOW, INSUFFICIENT_BALANCE...) cần DB thật.
 */
class AuctionRoomTest {

    private AuctionRoom room;

    @BeforeEach
    void setUp() {
        room = new AuctionRoom(101, 500.0);
        AuctionManager.getInstance().createRoom(101, 500.0);
    }

    @AfterEach
    void tearDown() {
        AuctionManager.getInstance().removeRoom(101);
    }

    // ── Hàm hỗ trợ ───────────────────────────────────────────────────────────

    /** Kết thúc phòng, bỏ qua lỗi DB trong môi trường test */
    private void finishRoomSafely() {
        try { room.finishAuction(); } catch (RuntimeException ignored) {}
    }

    // ── Khởi tạo ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Khởi tạo: sessionId đúng")
    void testInit_SessionId() {
        assertEquals(101, room.getSessionId());
    }

    @Test
    @DisplayName("Khởi tạo: currentPrice đúng")
    void testInit_CurrentPrice() {
        assertEquals(500.0, room.getCurrentPrice());
    }

    @Test
    @DisplayName("Khởi tạo: lastBidder là null")
    void testInit_LastBidderIsNull() {
        assertNull(room.getLastBidder());
    }

    @Test
    @DisplayName("Khởi tạo: isFinished = false")
    void testInit_IsFinishedFalse() {
        assertFalse(room.isFinished());
    }

    // ── Kết thúc phiên ───────────────────────────────────────────────────────

    @Test
    @DisplayName("finishAuction: isFinished = true ngay (trước DB call)")
    void testFinishAuction_SetsIsFinishedTrue() {
        assertFalse(room.isFinished());
        finishRoomSafely();
        assertTrue(room.isFinished());
    }

    @Test
    @DisplayName("finishAuction lần 2: idempotent, không throw")
    void testFinishAuction_CalledTwice_Idempotent() {
        finishRoomSafely();
        assertDoesNotThrow(() -> room.finishAuction());
        assertTrue(room.isFinished());
    }

    @Test
    @DisplayName("finishAuction 10 lần liên tiếp: không throw")
    void testFinishAuction_CalledManyTimes_NoThrow() {
        finishRoomSafely();
        for (int i = 0; i < 9; i++) {
            assertDoesNotThrow(() -> room.finishAuction());
        }
        assertTrue(room.isFinished());
    }

    // ── placeBid — phòng đã kết thúc (không cần DB) ─────────────────────────

    @Test
    @DisplayName("placeBid vào phòng finished → DB_ERROR (guard đầu method)")
    void testPlaceBid_RoomFinished_ReturnsDbError() {
        finishRoomSafely();
        assertEquals(BidResult.DB_ERROR, room.placeBid(9_999.0, "alice"));
    }

    @Test
    @DisplayName("placeBid giá 0, phòng finished → DB_ERROR")
    void testPlaceBid_ZeroPrice_RoomFinished() {
        finishRoomSafely();
        assertEquals(BidResult.DB_ERROR, room.placeBid(0.0, "alice"));
    }

    @Test
    @DisplayName("placeBid giá âm, phòng finished → DB_ERROR")
    void testPlaceBid_NegativePrice_RoomFinished() {
        finishRoomSafely();
        assertEquals(BidResult.DB_ERROR, room.placeBid(-100.0, "alice"));
    }

    @Test
    @DisplayName("placeBid account null, phòng finished → DB_ERROR, không throw NPE")
    void testPlaceBid_NullAccount_RoomFinished_NoThrow() {
        finishRoomSafely();
        assertDoesNotThrow(() ->
                assertEquals(BidResult.DB_ERROR, room.placeBid(1_000.0, null)));
    }

    @Test
    @DisplayName("placeBid nhiều lần, phòng finished → tất cả DB_ERROR")
    void testPlaceBid_MultipleAttempts_RoomFinished_AllDbError() {
        finishRoomSafely();
        assertEquals(BidResult.DB_ERROR, room.placeBid(1_000.0, "user1"));
        assertEquals(BidResult.DB_ERROR, room.placeBid(2_000.0, "user2"));
        assertEquals(BidResult.DB_ERROR, room.placeBid(3_000.0, "user3"));
    }

    @Test
    @DisplayName("currentPrice không đổi sau bid bị từ chối (phòng finished)")
    void testPlaceBid_Rejected_PriceUnchanged() {
        double original = room.getCurrentPrice();
        finishRoomSafely();
        room.placeBid(99_999.0, "alice");
        assertEquals(original, room.getCurrentPrice());
    }

    @Test
    @DisplayName("lastBidder không đổi sau bid bị từ chối (phòng finished)")
    void testPlaceBid_Rejected_LastBidderUnchanged() {
        finishRoomSafely();
        room.placeBid(99_999.0, "alice");
        assertNull(room.getLastBidder());
    }

    // ── placeBid — phòng chưa kết thúc, không có DB ─────────────────────────
    // Các trường hợp này BiddingService sẽ gọi DB → RuntimeException nếu không có DB.
    // Ta chỉ đảm bảo không ném NPE và kết quả không null.

    @Test
    @DisplayName("placeBid phòng chưa finished, không có DB → không throw NPE")
    void testPlaceBid_ActiveRoom_NoDB_NoNPE() {
        BidResult result;
        try {
            result = room.placeBid(600.0, "alice");
        } catch (NullPointerException e) {
            fail("Không được throw NPE: " + e);
            return;
        } catch (RuntimeException ignored) {
            return; // DB lỗi — chấp nhận
        }
        assertNotNull(result);
    }

    // ── registerAutoBid — chốt kiểm tra isFinished ──────────────────────────

    @Test
    @DisplayName("registerAutoBid vào phòng finished → return ngay, không throw")
    void testRegisterAutoBid_RoomFinished_NoThrow() {
        finishRoomSafely();
        assertDoesNotThrow(() -> room.registerAutoBid("alice", 1_000.0));
    }

    @Test
    @DisplayName("registerAutoBid account null, phòng finished → không throw NPE")
    void testRegisterAutoBid_NullAccount_RoomFinished_NoThrow() {
        finishRoomSafely();
        assertDoesNotThrow(() -> room.registerAutoBid(null, 1_000.0));
    }

    // ── Người theo dõi phòng ────────────────────────────────────────────────

    @Test
    @DisplayName("addSubscriber hợp lệ → không throw")
    void testAddSubscriber_Valid_NoThrow() {
        ClientSession ctx = new ClientSession();
        ctx.setOut(new PrintWriter(new StringWriter(), true));
        assertDoesNotThrow(() -> room.addSubscriber(ctx));
    }

    @Test
    @DisplayName("removeSubscriber chưa thêm → không throw")
    void testRemoveSubscriber_NotAdded_NoThrow() {
        assertDoesNotThrow(() -> room.removeSubscriber(new ClientSession()));
    }

    @Test
    @DisplayName("addSubscriber rồi removeSubscriber → không throw")
    void testAddThenRemoveSubscriber_NoThrow() {
        ClientSession ctx = new ClientSession();
        ctx.setOut(new PrintWriter(new StringWriter(), true));
        assertDoesNotThrow(() -> {
            room.addSubscriber(ctx);
            room.removeSubscriber(ctx);
        });
    }

    @Test
    @DisplayName("addSubscriber null → không throw (CopyOnWriteArrayList cho phép)")
    void testAddSubscriber_Null_NoThrow() {
        assertDoesNotThrow(() -> room.addSubscriber(null));
    }

    @Test
    @DisplayName("removeSubscriber null → không throw")
    void testRemoveSubscriber_Null_NoThrow() {
        assertDoesNotThrow(() -> room.removeSubscriber(null));
    }

    // ── Phát thông báo ──────────────────────────────────────────────────────

    @Test
    @DisplayName("broadcast phòng trống → không throw")
    void testBroadcast_NoSubscribers_NoThrow() {
        assertDoesNotThrow(() -> room.broadcast(ServerEvent.SYSTEM, "msg", null));
    }

    @Test
    @DisplayName("broadcast payload null → không throw")
    void testBroadcast_NullPayload_NoThrow() {
        assertDoesNotThrow(() -> room.broadcast(ServerEvent.SYSTEM, "msg", null));
    }

    @Test
    @DisplayName("broadcast action null → không throw")
    void testBroadcast_NullAction_NoThrow() {
        assertDoesNotThrow(() -> room.broadcast(null, "msg", null));
    }

    // ── Hàm truy xuất / trạng thái độc lập ──────────────────────────────────

    @Test
    @DisplayName("getSessionId đúng với constructor")
    void testGetSessionId_Correct() {
        assertEquals(42, new AuctionRoom(42, 1_000.0).getSessionId());
    }

    @Test
    @DisplayName("getCurrentPrice đúng với constructor")
    void testGetCurrentPrice_Correct() {
        assertEquals(123.45, new AuctionRoom(1, 123.45).getCurrentPrice());
    }

    @Test
    @DisplayName("2 AuctionRoom có state độc lập nhau")
    void testTwoRooms_IndependentState() {
        AuctionRoom r1 = new AuctionRoom(201, 1_000.0);
        AuctionRoom r2 = new AuctionRoom(202, 2_000.0);

        try { r1.finishAuction(); } catch (RuntimeException ignored) {}

        assertTrue(r1.isFinished(), "r1 phải finished");
        assertFalse(r2.isFinished(), "r2 không bị ảnh hưởng bởi r1");
    }

    // ── Bao phủ enum BidResult ──────────────────────────────────────────────

    @Test
    @DisplayName("BidResult.DB_ERROR được trả khi phòng finished — đúng semantic")
    void testBidResult_DbError_Semantic() {
        finishRoomSafely();
        BidResult r = room.placeBid(600.0, "alice");
        assertEquals(BidResult.DB_ERROR, r,
                "Phòng finished phải trả DB_ERROR, không phải boolean false");
    }
}
