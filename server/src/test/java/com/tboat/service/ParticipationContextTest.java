package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.core.User;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho ParticipationContext (mẫu Strategy).
 *
 * Dùng lambda thủ công làm TransactionRole giả lập, không cần DB và không cần Mockito.
 * Chữ ký hàm đúng: (User, AuctionSession, double, UserDAO, AuctionSessionDAO, HistoryDAO, BidDAO)
 */
class ParticipationContextTest {

    private User          dummyUser;
    private AuctionSession dummySession;

    @BeforeEach
    void setUp() {
        dummyUser    = new User("testAccount", "pass", "TestNick", 999_999.0, "", "");
        dummySession = new AuctionSession(
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                1_000.0,
                100.0,
                null
        );
        dummySession.setId(1);
    }

    // ===================== CHIẾN LƯỢC — VAI TRÒ LUÔN TRUE =====================

    @Test
    @DisplayName("Role trả về true → Context trả về true")
    void testExecuteAction_AlwaysTrueRole_ReturnsTrue() {
        TransactionRole alwaysTrue =
                (user, session, amount, userDAO, sessionDAO, historyDAO, bidDAO) -> true;

        ParticipationContext ctx = new ParticipationContext(alwaysTrue);
        assertTrue(ctx.executeAction(dummyUser, dummySession, 1_000.0,
                null, null, null, null));
    }

    // ===================== CHIẾN LƯỢC — VAI TRÒ LUÔN FALSE =====================

    @Test
    @DisplayName("Role trả về false → Context trả về false")
    void testExecuteAction_AlwaysFalseRole_ReturnsFalse() {
        TransactionRole alwaysFalse =
                (user, session, amount, userDAO, sessionDAO, historyDAO, bidDAO) -> false;

        ParticipationContext ctx = new ParticipationContext(alwaysFalse);
        assertFalse(ctx.executeAction(dummyUser, dummySession, 1_000.0,
                null, null, null, null));
    }

    // ===================== VAI TRÒ NULL =====================

    @Test
    @DisplayName("roleBehavior = null → trả về false ngay, không throw")
    void testExecuteAction_NullRole_ReturnsFalse() {
        ParticipationContext ctx = new ParticipationContext(null);
        assertFalse(ctx.executeAction(dummyUser, dummySession, 1_000.0,
                null, null, null, null));
    }

    // ===================== THAM SỐ TRUYỀN VÀO VAI TRÒ =====================

    @Test
    @DisplayName("Role nhận đúng User được truyền vào")
    void testExecuteAction_RoleReceivesCorrectUser() {
        User[] captured = {null};
        TransactionRole capturingRole =
                (user, session, amount, userDAO, sessionDAO, historyDAO, bidDAO) -> {
                    captured[0] = user;
                    return true;
                };

        new ParticipationContext(capturingRole)
                .executeAction(dummyUser, dummySession, 500.0, null, null, null, null);

        assertSame(dummyUser, captured[0], "Role phải nhận đúng User.");
    }

    @Test
    @DisplayName("Role nhận đúng AuctionSession được truyền vào")
    void testExecuteAction_RoleReceivesCorrectSession() {
        AuctionSession[] captured = {null};
        TransactionRole capturingRole =
                (user, session, amount, userDAO, sessionDAO, historyDAO, bidDAO) -> {
                    captured[0] = session;
                    return true;
                };

        new ParticipationContext(capturingRole)
                .executeAction(dummyUser, dummySession, 500.0, null, null, null, null);

        assertSame(dummySession, captured[0], "Role phải nhận đúng AuctionSession.");
    }

    @Test
    @DisplayName("Role nhận đúng amount = 0.0")
    void testExecuteAction_RoleReceivesZeroAmount() {
        double[] captured = {-1};
        TransactionRole capturingRole =
                (user, session, amount, userDAO, sessionDAO, historyDAO, bidDAO) -> {
                    captured[0] = amount;
                    return true;
                };

        new ParticipationContext(capturingRole)
                .executeAction(dummyUser, dummySession, 0.0, null, null, null, null);

        assertEquals(0.0, captured[0], "Role phải nhận đúng amount = 0.");
    }

    @Test
    @DisplayName("Role nhận đúng amount lớn (1 tỷ)")
    void testExecuteAction_RoleReceivesLargeAmount() {
        double[] captured = {-1};
        TransactionRole capturingRole =
                (user, session, amount, userDAO, sessionDAO, historyDAO, bidDAO) -> {
                    captured[0] = amount;
                    return true;
                };

        new ParticipationContext(capturingRole)
                .executeAction(dummyUser, dummySession, 1_000_000_000.0, null, null, null, null);

        assertEquals(1_000_000_000.0, captured[0]);
    }

    @Test
    @DisplayName("Role nhận đúng UserDAO được truyền vào")
    void testExecuteAction_RoleReceivesCorrectUserDAO() {
        UserDAO fakeDAO = new UserDAO();
        UserDAO[] captured = {null};
        TransactionRole capturingRole =
                (user, session, amount, userDAO, sessionDAO, historyDAO, bidDAO) -> {
                    captured[0] = userDAO;
                    return true;
                };

        new ParticipationContext(capturingRole)
                .executeAction(dummyUser, dummySession, 1_000.0, fakeDAO, null, null, null);

        assertSame(fakeDAO, captured[0]);
    }

    // ===================== TẤT CẢ THAM SỐ NULL =====================

    @Test
    @DisplayName("executeAction với tất cả tham số null (trừ role): không throw")
    void testExecuteAction_AllNullParams_NoThrow() {
        TransactionRole safeRole =
                (user, session, amount, userDAO, sessionDAO, historyDAO, bidDAO) -> true;

        assertDoesNotThrow(() ->
                new ParticipationContext(safeRole)
                        .executeAction(null, null, 0, null, null, null, null)
        );
    }

    // ===================== CHIẾN LƯỢC — TÍNH HOÁN ĐỔI =====================

    @Test
    @DisplayName("2 context với 2 role khác nhau: kết quả độc lập")
    void testStrategy_TwoContexts_IndependentResults() {
        TransactionRole roleTrue  =
                (u, s, a, ud, sd, hd, bd) -> true;
        TransactionRole roleFalse =
                (u, s, a, ud, sd, hd, bd) -> false;

        assertTrue(
                new ParticipationContext(roleTrue)
                        .executeAction(dummyUser, dummySession, 100, null, null, null, null)
        );
        assertFalse(
                new ParticipationContext(roleFalse)
                        .executeAction(dummyUser, dummySession, 100, null, null, null, null)
        );
    }

    @Test
    @DisplayName("Role có thể ném RuntimeException: Context phải lan truyền exception")
    void testExecuteAction_RoleThrowsException_Propagates() {
        TransactionRole throwingRole =
                (u, s, a, ud, sd, hd, bd) -> { throw new RuntimeException("test error"); };

        ParticipationContext ctx = new ParticipationContext(throwingRole);
        assertThrows(RuntimeException.class,
                () -> ctx.executeAction(dummyUser, dummySession, 1_000.0,
                        null, null, null, null)
        );
    }

    @Test
    @DisplayName("Cùng 1 context gọi executeAction nhiều lần: kết quả nhất quán")
    void testExecuteAction_CalledMultipleTimes_ConsistentResult() {
        TransactionRole alwaysTrue =
                (u, s, a, ud, sd, hd, bd) -> true;

        ParticipationContext ctx = new ParticipationContext(alwaysTrue);
        for (int i = 0; i < 5; i++) {
            assertTrue(ctx.executeAction(dummyUser, dummySession, 1_000.0,
                            null, null, null, null),
                    "Lần gọi thứ " + (i + 1) + " phải trả về true.");
        }
    }
}
