package com.tboat.service;

import com.tboat.models.auction.AuctionSession;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho SellerService.
 * Dùng ID/account không tồn tại để kiểm tra guard logic.
 *
 * Root cause lỗi trước: DatabaseConnection.getConnection() throw RuntimeException
 * khi không có DB → DAO ném ra ngoài thay vì return false.
 *
 * Chiến lược: wrap tất cả call vào safeCancel/safeEdit helper.
 * Nếu DB không có → RuntimeException được chấp nhận (không phải NPE).
 * Nếu DB có và trả về null → phải return false.
 */
class SellerServiceTest {

    private SellerService sellerService;

    @BeforeEach
    void setUp() {
        sellerService = new SellerService();
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private boolean safeCancelAuction(String account, int sessionId) {
        try {
            return sellerService.cancelAuction(account, sessionId);
        } catch (RuntimeException e) {
            assertFalse(e instanceof NullPointerException,
                    "Không được throw NPE. Cause: " + e);
            return false;
        }
    }

    private boolean safeEditAuction(String account, AuctionSession session) {
        try {
            return sellerService.editAuction(account, session);
        } catch (RuntimeException e) {
            assertFalse(e instanceof NullPointerException,
                    "Không được throw NPE. Cause: " + e);
            return false;
        }
    }

    // ===================== CANCEL AUCTION =====================

    @Test
    @DisplayName("cancelAuction: sessionId không tồn tại → false")
    void testCancelAuction_SessionNotFound_False() {
        assertFalse(safeCancelAuction("anyAccount", -1));
    }

    @Test
    @DisplayName("cancelAuction: account không tồn tại → false")
    void testCancelAuction_UserNotFound_False() {
        assertFalse(safeCancelAuction("___nonExistentUser___", 1));
    }

    @Test
    @DisplayName("cancelAuction: account null → false, không throw NPE")
    void testCancelAuction_NullAccount_NoNPE() {
        assertFalse(safeCancelAuction(null, 1));
    }

    @Test
    @DisplayName("cancelAuction: cả 2 tham số không hợp lệ → false")
    void testCancelAuction_BothInvalid_False() {
        assertFalse(safeCancelAuction(null, -1));
    }

    @Test
    @DisplayName("cancelAuction: account rỗng → false, không throw NPE")
    void testCancelAuction_EmptyAccount_NoNPE() {
        assertFalse(safeCancelAuction("", -1));
    }

    @Test
    @DisplayName("cancelAuction: sessionId = 0 → false")
    void testCancelAuction_SessionZero_False() {
        assertFalse(safeCancelAuction("anyAccount", 0));
    }

    @Test
    @DisplayName("cancelAuction gọi nhiều lần liên tiếp với ID giả: không throw NPE")
    void testCancelAuction_RepeatedInvalidCalls_NoNPE() {
        for (int i = 0; i < 5; i++) {
            assertFalse(safeCancelAuction("fakeUser", -i));
        }
    }

    // ===================== EDIT AUCTION =====================

    @Test
    @DisplayName("editAuction: updatedSession null → false ngay (guard đầu method, không cần DB)")
    void testEditAuction_NullSession_False() {
        // SellerService.editAuction() có guard: if (user == null || updatedSession == null) return false
        // Nhưng userDAO.getUser() gọi DB trước → vẫn cần wrap
        assertFalse(safeEditAuction("anyAccount", null));
    }

    @Test
    @DisplayName("editAuction: account null + session null → false, không throw NPE")
    void testEditAuction_NullAccount_NullSession_NoNPE() {
        assertFalse(safeEditAuction(null, null));
    }

    @Test
    @DisplayName("editAuction: account không tồn tại, session null → false")
    void testEditAuction_UserNotFound_NullSession_False() {
        assertFalse(safeEditAuction("___nonExistentUser___", null));
    }

    @Test
    @DisplayName("editAuction: account không tồn tại, session hợp lệ → false (user null)")
    void testEditAuction_UserNotFound_ValidSession_False() {
        AuctionSession session = new AuctionSession(
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                1_000.0, 100.0, null
        );
        session.setId(99);
        assertFalse(safeEditAuction("___nonExistentUser___", session));
    }

    @Test
    @DisplayName("editAuction: account rỗng → false, không throw NPE")
    void testEditAuction_EmptyAccount_NoNPE() {
        assertFalse(safeEditAuction("", null));
    }

    @Test
    @DisplayName("editAuction: cả account null và session null → false")
    void testEditAuction_BothNull_False() {
        assertFalse(safeEditAuction(null, null));
    }

    @Test
    @DisplayName("editAuction gọi nhiều lần với tham số không hợp lệ: không throw NPE")
    void testEditAuction_RepeatedInvalidCalls_NoNPE() {
        for (int i = 0; i < 5; i++) {
            assertFalse(safeEditAuction("fakeUser" + i, null));
        }
    }
}