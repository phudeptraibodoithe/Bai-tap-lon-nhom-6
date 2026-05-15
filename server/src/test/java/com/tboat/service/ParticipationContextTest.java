//package com.tboat.service;
//
//import com.tboat.models.AuctionSession;
//import com.tboat.models.User;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.DisplayName;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Unit Test cho ParticipationContext (Strategy Pattern).
// *
// * Sử dụng stub/mock thủ công (không dùng Mockito) để kiểm tra
// * logic điều phối của Context mà không cần DB.
// */
//class ParticipationContextTest {
//
//    private User dummyUser;
//    private AuctionSession dummySession;
//
//    @BeforeEach
//    void setUp() {
//        dummyUser = new User("testAccount", "pass", "TestNick", 999999.0, "", "");
//
//        // AuctionSession là abstract class -> dùng anonymous subclass
//        dummySession = new AuctionSession() {};
//    }
//
//    // ===================== STRATEGY PATTERN =====================
//
//    @Test
//    @DisplayName("executeAction với role trả về true -> Context phải trả về true")
//    void testExecuteAction_WithAlwaysTrueRole_ShouldReturnTrue() {
//        TransactionRole alwaysTrueRole = (user, session, amount, userDAO, sessionDAO, bidDAO) -> true;
//
//        ParticipationContext context = new ParticipationContext(alwaysTrueRole);
//        boolean result = context.executeAction(dummyUser, dummySession, 1000.0, null, null, null);
//
//        assertTrue(result, "Context phải trả về true khi role trả về true.");
//    }
//
//    @Test
//    @DisplayName("executeAction với role trả về false -> Context phải trả về false")
//    void testExecuteAction_WithAlwaysFalseRole_ShouldReturnFalse() {
//        TransactionRole alwaysFalseRole = (user, session, amount, userDAO, sessionDAO, bidDAO) -> false;
//
//        ParticipationContext context = new ParticipationContext(alwaysFalseRole);
//        boolean result = context.executeAction(dummyUser, dummySession, 1000.0, null, null, null);
//
//        assertFalse(result, "Context phải trả về false khi role trả về false.");
//    }
//
//    @Test
//    @DisplayName("executeAction với roleBehavior = null -> phải trả về false ngay")
//    void testExecuteAction_NullRole_ShouldReturnFalse() {
//        ParticipationContext context = new ParticipationContext(null);
//        boolean result = context.executeAction(dummyUser, dummySession, 1000.0, null, null, null);
//
//        assertFalse(result, "Context với role null phải trả về false mà không throw Exception.");
//    }
//
//    @Test
//    @DisplayName("executeAction không throw Exception khi tham số null (trừ role)")
//    void testExecuteAction_NullParams_ShouldNotThrow() {
//        TransactionRole safeRole = (user, session, amount, userDAO, sessionDAO, bidDAO) -> true;
//        ParticipationContext context = new ParticipationContext(safeRole);
//
//        assertDoesNotThrow(
//                () -> context.executeAction(null, null, 0, null, null, null),
//                "Context không được throw Exception khi nhận tham số null."
//        );
//    }
//
//    // ===================== KIỂM TRA ROLE THAY THẾ ĐƯỢC (Strategy) =====================
//
//    @Test
//    @DisplayName("Có thể đổi role khác nhau trên cùng 1 context - Strategy pattern hoạt động")
//    void testStrategyPattern_RolesAreInterchangeable() {
//        TransactionRole roleA = (u, s, a, ud, sd, bd) -> true;
//        TransactionRole roleB = (u, s, a, ud, sd, bd) -> false;
//
//        ParticipationContext contextA = new ParticipationContext(roleA);
//        ParticipationContext contextB = new ParticipationContext(roleB);
//
//        assertTrue(contextA.executeAction(dummyUser, dummySession, 100, null, null, null));
//        assertFalse(contextB.executeAction(dummyUser, dummySession, 100, null, null, null));
//    }
//
//    @Test
//    @DisplayName("executeAction với amount = 0: role nhận đúng giá trị 0")
//    void testExecuteAction_ZeroAmount_RoleReceivesCorrectValue() {
//        double[] capturedAmount = {-1};
//        TransactionRole capturingRole = (user, session, amount, userDAO, sessionDAO, bidDAO) -> {
//            capturedAmount[0] = amount;
//            return true;
//        };
//
//        ParticipationContext context = new ParticipationContext(capturingRole);
//        context.executeAction(dummyUser, dummySession, 0.0, null, null, null);
//
//        assertEquals(0.0, capturedAmount[0], "Role phải nhận đúng giá trị amount = 0.");
//    }
//}