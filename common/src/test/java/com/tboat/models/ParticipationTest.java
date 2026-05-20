//package com.tboat.models;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import com.tboat.models.auction.Participation;
//
//public class ParticipationTest {
//
//    private Participation participation;
//
//    @BeforeEach
//    void setUp() {
//        participation = new Participation("user01", 10, "BIDDER");
//    }
//
//    @Test
//    void testGetAccountName() {
//        assertEquals("user01", participation.getAccountName());
//    }
//
//    @Test
//    void testGetAuctionSessionId() {
//        assertEquals(10, participation.getAuctionSessionId());
//    }
//
//    @Test
//    void testGetRoleType() {
//        assertEquals("BIDDER", participation.getRoleType());
//    }
//
//    @Test
//    void testAccountNameNotNull() {
//        assertNotNull(participation.getAccountName());
//    }
//
//    @Test
//    void testAccountNameNotEmpty() {
//        assertThrows(IllegalArgumentException.class,
//            () -> new Participation("", 10, "BIDDER"));
//    }
//
//    @Test
//    void testRoleTypeNotNull() {
//        assertNotNull(participation.getRoleType());
//    }
//
//    @Test
//    void testNullRoleTypeThrows() {
//        assertThrows(IllegalArgumentException.class,
//            () -> new Participation("user01", 10, null));
//    }
//
//    @Test
//    void testAuctionSessionIdPositive() {
//        assertThrows(IllegalArgumentException.class,
//            () -> new Participation("user01", -1, "BIDDER"));
//    }
//
//    @Test
//    void testEqualityByAccountAndSession() {
//        Participation p2 = new Participation("user01", 10, "OBSERVER");
//        assertEquals(participation.getAccountName(), p2.getAccountName());
//        assertEquals(participation.getAuctionSessionId(), p2.getAuctionSessionId());
//    }
//}