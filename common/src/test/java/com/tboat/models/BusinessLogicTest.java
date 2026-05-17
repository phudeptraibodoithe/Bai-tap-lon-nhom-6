//package com.tboat.models;
//
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
//
//public class BusinessLogicTest {
//
//    @Test
//    void testParticipationRole() {
//        Participation participation = new Participation("user1", 101, "BIDDER");
//
//        assertEquals("user1", participation.getAccountName());
//        assertEquals(101, participation.getAuctionSessionId());
//        assertEquals("BIDDER", participation.getRoleType());
//    }
//
//    @Test
//    void testRequestResponseGeneric() {
//        // Test xem class Generic Request/Response có hoạt động không
//        User user = new User("test", "1", "nick", 0, "desc", "url");
//        Request<User> request = new Request<>("LOGIN", user);
//
//        assertEquals("LOGIN", request.getAction());
//        assertEquals("test", request.getPayload().getAccountName());
//
//        Response<String> response = new Response<>("SUCCESS", "Login successful", "Token123");
//        assertNotNull(response);
//    }
//}