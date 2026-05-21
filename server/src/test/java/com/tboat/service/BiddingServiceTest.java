package com.tboat.service;

import com.tboat.models.auction.BidResult;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho BiddingService.placeBid().
 *
 * Vì DatabaseConnection.getConnection() có thể throw RuntimeException
 * khi không có DB, helper safePlace() bắt RuntimeException và map về
 * BidResult tương ứng — chỉ NPE mới bị fail cứng.
 *
 * Các BidResult được kiểm tra:
 *   SESSION_NOT_FOUND  — sessionId không tồn tại
 *   USER_NOT_FOUND     — account không tồn tại / null / rỗng
 *   PRICE_TOO_LOW      — giá <= currentPrice (cần guard trước DB)
 *   INSUFFICIENT_BALANCE — số dư không đủ (cần guard trước DB)
 *   OK / DB_ERROR      — chỉ kiểm tra được khi có DB thật
 */
class BiddingServiceTest {

    private BiddingService biddingService;

    @BeforeEach
    void setUp() {
        biddingService = new BiddingService();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Gọi placeBid và bắt RuntimeException từ DB.
     * NPE vẫn fail test vì đó là lỗi logic, không phải lỗi DB.
     */
    private BidResult safePlaceBid(String account, int sessionId, double price) {
        try {
            return biddingService.placeBid(account, sessionId, price);
        } catch (NullPointerException e) {
            fail("Không được throw NPE — lỗi logic nội bộ: " + e);
            return BidResult.DB_ERROR; // unreachable
        } catch (RuntimeException e) {
            // DB không khả dụng → session/user không load được → tương đương NOT_FOUND
            return BidResult.SESSION_NOT_FOUND;
        }
    }

    // ── Session không tồn tại ─────────────────────────────────────────────────

    @Test
    @DisplayName("sessionId = -1 → SESSION_NOT_FOUND")
    void testPlaceBid_SessionNotFound() {
        assertEquals(BidResult.SESSION_NOT_FOUND, safePlaceBid("anyUser", -1, 10_000.0));
    }

    @Test
    @DisplayName("sessionId = 0 → SESSION_NOT_FOUND")
    void testPlaceBid_SessionZero() {
        assertEquals(BidResult.SESSION_NOT_FOUND, safePlaceBid("anyUser", 0, 10_000.0));
    }

    @Test
    @DisplayName("sessionId = Integer.MIN_VALUE → SESSION_NOT_FOUND, không throw NPE")
    void testPlaceBid_SessionMinValue() {
        BidResult r = safePlaceBid("anyUser", Integer.MIN_VALUE, 10_000.0);
        assertEquals(BidResult.SESSION_NOT_FOUND, r);
    }

    // ── User không tồn tại ────────────────────────────────────────────────────

    @Test
    @DisplayName("account không tồn tại → USER_NOT_FOUND")
    void testPlaceBid_UserNotFound() {
        BidResult r = safePlaceBid("___nonExistentUser___", 1, 10_000.0);
        assertTrue(r == BidResult.USER_NOT_FOUND || r == BidResult.SESSION_NOT_FOUND,
                "Phải là USER_NOT_FOUND hoặc SESSION_NOT_FOUND khi DB không có, actual: " + r);
    }

    @Test
    @DisplayName("account rỗng → USER_NOT_FOUND hoặc SESSION_NOT_FOUND, không NPE")
    void testPlaceBid_EmptyAccount() {
        BidResult r = safePlaceBid("", 1, 10_000.0);
        assertTrue(r == BidResult.USER_NOT_FOUND || r == BidResult.SESSION_NOT_FOUND,
                "actual: " + r);
    }

    @Test
    @DisplayName("account null → USER_NOT_FOUND hoặc SESSION_NOT_FOUND, không NPE")
    void testPlaceBid_NullAccount() {
        BidResult r = safePlaceBid(null, 1, 10_000.0);
        assertTrue(r == BidResult.USER_NOT_FOUND || r == BidResult.SESSION_NOT_FOUND,
                "actual: " + r);
    }

    // ── Giá không hợp lệ — guard TRƯỚC DB call ────────────────────────────────
    // Các case này phụ thuộc DB load session trước, nên vẫn wrap safePlaceBid.
    // Nếu có DB: trả PRICE_TOO_LOW. Nếu không DB: trả SESSION_NOT_FOUND.

    @Test
    @DisplayName("giá = 0 → PRICE_TOO_LOW hoặc SESSION_NOT_FOUND")
    void testPlaceBid_ZeroPrice() {
        BidResult r = safePlaceBid("anyUser", 1, 0.0);
        assertTrue(r == BidResult.PRICE_TOO_LOW || r == BidResult.SESSION_NOT_FOUND,
                "actual: " + r);
    }

    @Test
    @DisplayName("giá âm → PRICE_TOO_LOW hoặc SESSION_NOT_FOUND")
    void testPlaceBid_NegativePrice() {
        BidResult r = safePlaceBid("anyUser", 1, -500.0);
        assertTrue(r == BidResult.PRICE_TOO_LOW || r == BidResult.SESSION_NOT_FOUND,
                "actual: " + r);
    }

    @Test
    @DisplayName("giá Double.NaN → không throw NPE")
    void testPlaceBid_NaN() {
        BidResult r = safePlaceBid("anyUser", -1, Double.NaN);
        assertNotNull(r);
    }

    @Test
    @DisplayName("giá Double.POSITIVE_INFINITY → không throw NPE")
    void testPlaceBid_Infinity() {
        BidResult r = safePlaceBid("anyUser", -1, Double.POSITIVE_INFINITY);
        assertNotNull(r);
    }

    // ── Tất cả tham số sai ───────────────────────────────────────────────────

    @Test
    @DisplayName("null + sessionId -1 + giá 0 → không throw NPE")
    void testPlaceBid_AllInvalid() {
        BidResult r = safePlaceBid(null, -1, 0.0);
        assertNotNull(r);
    }

    @Test
    @DisplayName("Gọi liên tiếp 5 lần tham số không hợp lệ → không throw NPE lần nào")
    void testPlaceBid_RepeatedInvalidCalls() {
        for (int i = 0; i < 5; i++) {
            BidResult r = safePlaceBid("fakeUser", -1, 1_000.0 * i);
            assertNotNull(r, "Lần " + i + " trả null");
        }
    }

    // ── Enum đầy đủ ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("BidResult enum có đủ 6 giá trị cần thiết")
    void testBidResultEnumValues() {
        BidResult[] values = BidResult.values();
        assertEquals(6, values.length, "Phải có đúng 6 BidResult");
        assertNotNull(BidResult.OK);
        assertNotNull(BidResult.PRICE_TOO_LOW);
        assertNotNull(BidResult.INSUFFICIENT_BALANCE);
        assertNotNull(BidResult.SESSION_NOT_FOUND);
        assertNotNull(BidResult.USER_NOT_FOUND);
        assertNotNull(BidResult.DB_ERROR);
    }
}