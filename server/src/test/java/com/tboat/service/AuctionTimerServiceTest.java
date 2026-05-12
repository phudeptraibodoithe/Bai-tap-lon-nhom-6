package com.tboat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AuctionTimerService.
 *
 * Phần lớn logic trong AuctionTimerService phụ thuộc DB và hệ thống Timer.
 * File này kiểm tra những gì có thể test được không cần DB.
 */
class AuctionTimerServiceTest {

    // ===================== SINGLETON =====================

    @Test
    @DisplayName("AuctionTimerService phải là Singleton")
    void testSingletonInstance() {
        AuctionTimerService instance1 = AuctionTimerService.getInstance();
        AuctionTimerService instance2 = AuctionTimerService.getInstance();

        assertNotNull(instance1, "getInstance() không được trả về null.");
        assertSame(instance1, instance2, "AuctionTimerService phải là Singleton.");
    }

    // ===================== SCHEDULE CLOSE - BIÊN =====================

    @Test
    @DisplayName("scheduleAuctionClose với endTime đã qua: không throw Exception")
    void testScheduleAuctionClose_PastEndTime_ShouldNotThrow() {
        // EndTime trong quá khứ -> closeAuction được gọi ngay
        // Room -1 không tồn tại trong AuctionManager -> không làm gì hết
        assertDoesNotThrow(
                () -> AuctionTimerService.getInstance()
                        .scheduleAuctionClose(-1, java.time.LocalDateTime.now().minusMinutes(1)),
                "scheduleAuctionClose với endTime trong quá khứ không được throw Exception."
        );
    }

    @Test
    @DisplayName("scheduleAuctionClose với endTime trong tương lai: không throw Exception")
    void testScheduleAuctionClose_FutureEndTime_ShouldNotThrow() {
        // Lên lịch cho phòng không tồn tại ở tương lai xa
        assertDoesNotThrow(
                () -> AuctionTimerService.getInstance()
                        .scheduleAuctionClose(-999, java.time.LocalDateTime.now().plusHours(24)),
                "scheduleAuctionClose với endTime tương lai không được throw Exception."
        );
    }

    // ===================== EXTEND AUCTION =====================

    @Test
    @DisplayName("extendAuction với sessionId không tồn tại: không throw Exception")
    void testExtendAuction_SessionNotFound_ShouldNotThrow() {
        assertDoesNotThrow(
                () -> AuctionTimerService.getInstance().extendAuction(-1, 30),
                "extendAuction với sessionId không tồn tại không được throw Exception."
        );
    }

    @Test
    @DisplayName("extendAuction với giây gia hạn = 0: không throw Exception")
    void testExtendAuction_ZeroSeconds_ShouldNotThrow() {
        assertDoesNotThrow(
                () -> AuctionTimerService.getInstance().extendAuction(-1, 0),
                "extendAuction với 0 giây không được throw Exception."
        );
    }
}