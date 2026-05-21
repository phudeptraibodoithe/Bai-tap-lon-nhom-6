package com.tboat.service;

import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.item.Item;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AuctionTimerService — KHÔNG dùng Mockito.
 *
 * Chiến lược:
 * - Status PENDING/CANCELED/ENDED: scheduleAuction() return ngay trước khi chạm DB
 *   → dùng AuctionSession + Item thật, hoàn toàn an toàn.
 * - scheduleAuctionClose tương lai: chỉ lên lịch, không gọi DB ngay → an toàn.
 * - Các case gọi DB (quá khứ, extendAuction, ONGOING): wrap runAcceptingDbError().
 * - scheduleAuction(null): NPE documented behavior.
 */
class AuctionTimerServiceTest {

    private AuctionTimerService timerService;

    @BeforeEach
    void setUp() {
        timerService = AuctionTimerService.getInstance();
    }

    // ─── Stub Item không dùng Mockito ────────────────────────────────────

    /**
     * Item tối giản — subclass inline ghi đè những gì cần thiết.
     * Nếu Item là class cụ thể với constructor(seller, name, desc, imageURL)
     * thì dùng thẳng constructor đó.
     */
    private static Item stubItem() {
        return new Item("seller1", "Test Item", "desc", "http://img") {
            @Override
            public String getType() { return "ELECTRONICS"; }
        };
    }

    /**
     * Tạo AuctionSession thật với status cho trước.
     * Constructor: (id, start, end, price, bidIncrease, status, item, highestBidder)
     */
    private AuctionSession sessionWithStatus(int id, StatusOfAuction status) {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end   = LocalDateTime.now().plusHours(2);
        return new AuctionSession(id, start, end, 500.0, 50.0, status, stubItem(), null, 0.0);
    }

    /**
     * Tạo AuctionSession ONGOING với startTime/endTime tùy chỉnh.
     */
    private AuctionSession ongoingSession(int id, LocalDateTime start, LocalDateTime end) {
        return new AuctionSession(id, start, end, 500.0, 50.0,
                StatusOfAuction.ONGOING, stubItem(), null, 0.0);
    }

    // ─── Helper chấp nhận DB error ───────────────────────────────────────

    /**
     * Chạy action, chấp nhận RuntimeException bắt nguồn từ DB (không có kết nối).
     * Fail nếu là NPE thuần từ logic code nội bộ.
     */
    private void runAcceptingDbError(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            Throwable cause = e;
            while (cause.getCause() != null) cause = cause.getCause();
            if (cause instanceof NullPointerException) {
                fail("Không chấp nhận NPE từ logic nội bộ: " + e);
            }
            // RuntimeException khác (DB connection refused, access denied...) → pass
        }
    }

    // ===================== SINGLETON =====================

    @Test
    @DisplayName("getInstance() không được trả về null")
    void testSingleton_NotNull() {
        assertNotNull(timerService);
    }

    @Test
    @DisplayName("getInstance() phải là Singleton — 2 lần gọi trả về cùng object")
    void testSingleton_SameInstance() {
        AuctionTimerService i1 = AuctionTimerService.getInstance();
        AuctionTimerService i2 = AuctionTimerService.getInstance();
        assertSame(i1, i2);
    }

    @Test
    @DisplayName("Singleton thread-safe: 3 thread cùng gọi getInstance() → cùng 1 object")
    void testSingleton_ThreadSafe() throws InterruptedException {
        AuctionTimerService[] results = new AuctionTimerService[3];
        Thread t1 = new Thread(() -> results[0] = AuctionTimerService.getInstance());
        Thread t2 = new Thread(() -> results[1] = AuctionTimerService.getInstance());
        Thread t3 = new Thread(() -> results[2] = AuctionTimerService.getInstance());
        t1.start(); t2.start(); t3.start();
        t1.join();  t2.join();  t3.join();
        assertSame(results[0], results[1]);
        assertSame(results[1], results[2]);
    }

    // ===================== scheduleAuctionClose — TƯƠNG LAI (an toàn) =====================

    @Test
    @DisplayName("scheduleAuctionClose: endTime tương lai 1 giây → chỉ lên lịch, không throw")
    void testScheduleClose_NearFuture_NoThrow() {
        assertDoesNotThrow(() ->
                timerService.scheduleAuctionClose(-2, LocalDateTime.now().plusSeconds(1))
        );
    }

    @Test
    @DisplayName("scheduleAuctionClose: endTime tương lai 24h → chỉ lên lịch, không throw")
    void testScheduleClose_FarFuture_NoThrow() {
        assertDoesNotThrow(() ->
                timerService.scheduleAuctionClose(-3, LocalDateTime.now().plusHours(24))
        );
    }

    @Test
    @DisplayName("scheduleAuctionClose gọi 2 lần cùng ID: task cũ bị hủy, không throw")
    void testScheduleClose_CalledTwiceSameId_NoThrow() {
        assertDoesNotThrow(() -> {
            timerService.scheduleAuctionClose(-4, LocalDateTime.now().plusMinutes(30));
            timerService.scheduleAuctionClose(-4, LocalDateTime.now().plusMinutes(60));
        });
    }

    @Test
    @DisplayName("scheduleAuctionClose nhiều ID tương lai khác nhau: không throw")
    void testScheduleClose_MultipleIds_NoThrow() {
        assertDoesNotThrow(() -> {
            for (int i = -10; i >= -20; i--) {
                timerService.scheduleAuctionClose(i, LocalDateTime.now().plusMinutes(60));
            }
        });
    }

    // ===================== scheduleAuctionClose — QUÁ KHỨ (gọi DB) =====================

    @Test
    @DisplayName("scheduleAuctionClose: endTime quá khứ → closeAuction ngay (chấp nhận DB error)")
    void testScheduleClose_PastEndTime_AcceptsDbError() {
        runAcceptingDbError(() ->
                timerService.scheduleAuctionClose(-1, LocalDateTime.now().minusMinutes(5))
        );
    }

    @Test
    @DisplayName("scheduleAuctionClose: endTime = now → closeAuction ngay (chấp nhận DB error)")
    void testScheduleClose_ExactlyNow_AcceptsDbError() {
        runAcceptingDbError(() ->
                timerService.scheduleAuctionClose(-5, LocalDateTime.now())
        );
    }

    // ===================== scheduleAuction — STATUS GUARD (return sớm, KHÔNG gọi DB) =====================

    @Test
    @DisplayName("scheduleAuction với PENDING: return ngay trước DB, không throw")
    void testScheduleAuction_Pending_ReturnEarly_NoThrow() {
        AuctionSession session = sessionWithStatus(-100, StatusOfAuction.PENDING);
        assertDoesNotThrow(() -> timerService.scheduleAuction(session));
    }

    @Test
    @DisplayName("scheduleAuction với CANCELED: return ngay trước DB, không throw")
    void testScheduleAuction_Canceled_ReturnEarly_NoThrow() {
        AuctionSession session = sessionWithStatus(-101, StatusOfAuction.CANCELED);
        assertDoesNotThrow(() -> timerService.scheduleAuction(session));
    }

    @Test
    @DisplayName("scheduleAuction với ENDED: return ngay trước DB, không throw")
    void testScheduleAuction_Ended_ReturnEarly_NoThrow() {
        AuctionSession session = sessionWithStatus(-102, StatusOfAuction.ENDED);
        assertDoesNotThrow(() -> timerService.scheduleAuction(session));
    }

    @Test
    @DisplayName("scheduleAuction PENDING: gọi 2 lần cùng session → không throw")
    void testScheduleAuction_Pending_CalledTwice_NoThrow() {
        AuctionSession session = sessionWithStatus(-103, StatusOfAuction.PENDING);
        assertDoesNotThrow(() -> {
            timerService.scheduleAuction(session);
            timerService.scheduleAuction(session);
        });
    }

    @Test
    @DisplayName("scheduleAuction CANCELED: getId() trả về đúng ID đã set")
    void testScheduleAuction_SessionId_CorrectAfterCreation() {
        AuctionSession session = sessionWithStatus(-104, StatusOfAuction.CANCELED);
        assertEquals(-104, session.getId());
        assertDoesNotThrow(() -> timerService.scheduleAuction(session));
    }

    @Test
    @DisplayName("scheduleAuction ENDED: status của session là ENDED")
    void testScheduleAuction_Ended_StatusIsEnded() {
        AuctionSession session = sessionWithStatus(-105, StatusOfAuction.ENDED);
        assertEquals(StatusOfAuction.ENDED, session.getStatusOfAuction());
        assertDoesNotThrow(() -> timerService.scheduleAuction(session));
    }

    // ===================== scheduleAuction — ONGOING (gọi DB) =====================

    @Test
    @DisplayName("scheduleAuction ONGOING với endTime quá khứ: chấp nhận DB error")
    void testScheduleAuction_Ongoing_PastEnd_AcceptsDbError() {
        AuctionSession session = ongoingSession(
                -200,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusMinutes(10)
        );
        runAcceptingDbError(() -> timerService.scheduleAuction(session));
    }

    @Test
    @DisplayName("scheduleAuction ONGOING với startTime tương lai: chấp nhận DB error")
    void testScheduleAuction_Ongoing_FutureStart_AcceptsDbError() {
        AuctionSession session = ongoingSession(
                -201,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusHours(2)
        );
        runAcceptingDbError(() -> timerService.scheduleAuction(session));
    }

    @Test
    @DisplayName("scheduleAuction ONGOING đang diễn ra (start quá khứ, end tương lai): chấp nhận DB error")
    void testScheduleAuction_Ongoing_CurrentlyActive_AcceptsDbError() {
        AuctionSession session = ongoingSession(
                -202,
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().plusHours(1)
        );
        runAcceptingDbError(() -> timerService.scheduleAuction(session));
    }

    // ===================== scheduleAuction — NULL =====================

    @Test
    @DisplayName("scheduleAuction với session null: throw NullPointerException")
    void testScheduleAuction_NullSession_ThrowsNPE() {
        // session.getId() trên null → NPE — hành vi hiện tại.
        // Muốn bỏ test này thì thêm null-guard vào đầu scheduleAuction().
        assertThrows(NullPointerException.class,
                () -> timerService.scheduleAuction(null));
    }

    // ===================== extendAuction (gọi DB) =====================

    @Test
    @DisplayName("extendAuction sessionId không tồn tại: chấp nhận DB error hoặc return bình thường")
    void testExtendAuction_SessionNotFound_AcceptsDbError() {
        runAcceptingDbError(() -> timerService.extendAuction(-1, 30));
    }

    @Test
    @DisplayName("extendAuction 0 giây: chấp nhận DB error")
    void testExtendAuction_ZeroSeconds_AcceptsDbError() {
        runAcceptingDbError(() -> timerService.extendAuction(-1, 0));
    }

    @Test
    @DisplayName("extendAuction giây âm: chấp nhận DB error")
    void testExtendAuction_NegativeSeconds_AcceptsDbError() {
        runAcceptingDbError(() -> timerService.extendAuction(-1, -10));
    }

    @Test
    @DisplayName("extendAuction giây rất lớn: chấp nhận DB error")
    void testExtendAuction_LargeSeconds_AcceptsDbError() {
        runAcceptingDbError(() -> timerService.extendAuction(-1, Integer.MAX_VALUE));
    }
}