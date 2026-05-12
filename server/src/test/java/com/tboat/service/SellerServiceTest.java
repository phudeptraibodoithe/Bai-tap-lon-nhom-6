package com.tboat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test cho SellerService.
 *
 * LƯU Ý: Tất cả các test ở đây đều gọi DB thực tế thông qua SellerService -> DAO.
 * Các test dùng ID/account không tồn tại để đảm bảo an toàn (không ghi DB).
 */
class SellerServiceTest {

    private SellerService sellerService;

    @BeforeEach
    void setUp() {
        sellerService = new SellerService();
    }

    // ===================== CANCEL AUCTION =====================

    @Test
    @DisplayName("cancelAuction với sessionId không tồn tại: trả về false")
    void testCancelAuction_SessionNotFound_ShouldReturnFalse() {
        boolean result = sellerService.cancelAuction("anyAccount", -1);
        assertFalse(result, "Session không tồn tại phải trả về false.");
    }

    @Test
    @DisplayName("cancelAuction với account không tồn tại: trả về false")
    void testCancelAuction_UserNotFound_ShouldReturnFalse() {
        boolean result = sellerService.cancelAuction("___nonExistentUser___", 1);
        assertFalse(result, "User không tồn tại phải trả về false.");
    }

    @Test
    @DisplayName("cancelAuction với account null: trả về false, không throw Exception")
    void testCancelAuction_NullAccount_ShouldReturnFalse() {
        assertDoesNotThrow(() -> {
            boolean result = sellerService.cancelAuction(null, 1);
            assertFalse(result, "Account null phải trả về false.");
        }, "cancelAuction với null không được throw Exception.");
    }

    @Test
    @DisplayName("cancelAuction với cả 2 tham số null/không hợp lệ: trả về false")
    void testCancelAuction_BothInvalid_ShouldReturnFalse() {
        boolean result = sellerService.cancelAuction(null, -1);
        assertFalse(result, "Cả 2 tham số không hợp lệ phải trả về false.");
    }

    // ===================== EDIT AUCTION =====================

    @Test
    @DisplayName("editAuction với updatedSession null: trả về false")
    void testEditAuction_NullSession_ShouldReturnFalse() {
        boolean result = sellerService.editAuction("anyAccount", null);
        assertFalse(result, "Session null phải trả về false.");
    }

    @Test
    @DisplayName("editAuction với account null: trả về false, không throw Exception")
    void testEditAuction_NullAccount_ShouldReturnFalse() {
        assertDoesNotThrow(() -> {
            boolean result = sellerService.editAuction(null, null);
            assertFalse(result, "Account null phải trả về false.");
        }, "editAuction với null không được throw Exception.");
    }

    @Test
    @DisplayName("editAuction với account không tồn tại: trả về false")
    void testEditAuction_UserNotFound_ShouldReturnFalse() {
        // user null thì return false ngay trước khi gọi DAO
        boolean result = sellerService.editAuction("___nonExistentUser___", null);
        assertFalse(result, "User không tồn tại phải trả về false.");
    }
}