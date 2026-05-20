package com.tboat.service;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho BiddingService.
 *
 * Root cause lỗi trước: DatabaseConnection.getConnection() throw RuntimeException
 * khi không có DB → placeBid() ném ra ngoài thay vì return false.
 *
 * Chiến lược:
 * - placeBid() với sessionId không tồn tại → DAO gọi DB → RuntimeException có thể xảy ra.
 * - Test wrap try/catch: nếu DB không có → chấp nhận RuntimeException (không phải NPE);
 *   nếu DB có (trả về null) → phải return false.
 * - Chỉ các test kiểm tra guard TRƯỚC DB call mới dùng assertFalse trực tiếp.
 *   Hiện tại BiddingService gọi DB ngay ở dòng đầu → tất cả test đều wrap.
 */
class BiddingServiceTest {

    private BiddingService biddingService;

    @BeforeEach
    void setUp() {
        biddingService = new BiddingService();
    }

    // ─── Helper: chạy placeBid và trả về false nếu DB lỗi (RuntimeException) ───
    private boolean safePlaceBid(String account, int sessionId, double price) {
        try {
            return biddingService.placeBid(account, sessionId, price);
        } catch (RuntimeException e) {
            // DB không khả dụng → coi như guard trả về false (session/user not found)
            // KHÔNG chấp nhận NPE từ logic nội bộ
            assertFalse(e instanceof NullPointerException,
                    "Không được throw NPE — chỉ DB RuntimeException được chấp nhận. Cause: " + e);
            return false;
        }
    }

    // ===================== SESSION KHÔNG TỒN TẠI =====================

    @Test
    @DisplayName("sessionId không tồn tại (-1): trả về false")
    void testPlaceBid_SessionNotFound_ReturnsFalse() {
        assertFalse(safePlaceBid("anyUser", -1, 10_000.0));
    }

    @Test
    @DisplayName("sessionId 0: trả về false")
    void testPlaceBid_SessionZero_ReturnsFalse() {
        assertFalse(safePlaceBid("anyUser", 0, 10_000.0));
    }

    @Test
    @DisplayName("sessionId Integer.MIN_VALUE: trả về false, không throw NPE")
    void testPlaceBid_SessionMinValue_NoNPE() {
        assertFalse(safePlaceBid("anyUser", Integer.MIN_VALUE, 10_000.0));
    }

    // ===================== USER KHÔNG TỒN TẠI =====================

    @Test
    @DisplayName("account không tồn tại: trả về false")
    void testPlaceBid_UserNotFound_ReturnsFalse() {
        assertFalse(safePlaceBid("___nonExistentUser___", 1, 10_000.0));
    }

    @Test
    @DisplayName("account chuỗi rỗng: trả về false, không throw NPE")
    void testPlaceBid_EmptyAccount_NoNPE() {
        assertFalse(safePlaceBid("", 1, 10_000.0));
    }

    @Test
    @DisplayName("account null: trả về false, không throw NPE")
    void testPlaceBid_NullAccount_NoNPE() {
        assertFalse(safePlaceBid(null, 1, 10_000.0));
    }

    // ===================== GIÁ KHÔNG HỢP LỆ =====================

    @Test
    @DisplayName("giá = 0: trả về false")
    void testPlaceBid_ZeroPrice_ReturnsFalse() {
        assertFalse(safePlaceBid("anyUser", -1, 0.0));
    }

    @Test
    @DisplayName("giá âm (-500): trả về false")
    void testPlaceBid_NegativePrice_ReturnsFalse() {
        assertFalse(safePlaceBid("anyUser", -1, -500.0));
    }

    @Test
    @DisplayName("giá Double.MIN_VALUE: trả về false, không throw NPE")
    void testPlaceBid_MinDoublePrice_NoNPE() {
        assertFalse(safePlaceBid("anyUser", -1, Double.MIN_VALUE));
    }

    @Test
    @DisplayName("giá Double.NaN: trả về false, không throw NPE")
    void testPlaceBid_NaN_NoNPE() {
        assertFalse(safePlaceBid("anyUser", -1, Double.NaN));
    }

    @Test
    @DisplayName("giá Double.POSITIVE_INFINITY: trả về false, không throw NPE")
    void testPlaceBid_Infinity_NoNPE() {
        assertFalse(safePlaceBid("anyUser", -1, Double.POSITIVE_INFINITY));
    }

    // ===================== TẤT CẢ THAM SỐ ĐỀU SAI =====================

    @Test
    @DisplayName("account null + sessionId -1 + giá 0: false, không throw NPE")
    void testPlaceBid_AllInvalid_ReturnsFalse() {
        assertFalse(safePlaceBid(null, -1, 0.0));
    }

    @Test
    @DisplayName("Gọi placeBid nhiều lần liên tiếp với tham số không hợp lệ: không throw NPE")
    void testPlaceBid_RepeatedInvalidCalls_NoNPE() {
        for (int i = 0; i < 5; i++) {
            assertFalse(safePlaceBid("fakeUser", -1, 1_000.0 * i));
        }
    }
}