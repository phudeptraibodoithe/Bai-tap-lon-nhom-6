//package com.tboat.models;
//
//import java.time.LocalDateTime;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import com.tboat.models.auction.History;
//
//public class HistoryTest {
//
//    private History history;
//    private LocalDateTime completedAt;
//
//    @BeforeEach
//    void setUp() {
//        completedAt = LocalDateTime.now();
//        history = new History(10, "user01", 1500.0, completedAt);
//    }
//
//    @Test
//    void testGetAuctionSessionId() {
//        assertEquals(10, history.getAuctionSessionId());
//    }
//
//    @Test
//    void testGetWinnerAccount() {
//        assertEquals("user01", history.getWinnerAccountName());
//    }
//
//    @Test
//    void testGetFinalPrice() {
//        assertEquals(1500.0, history.getFinalPrice(), 0.001);
//    }
//
//    @Test
//    void testGetCompletedAt() {
//        assertEquals(completedAt, history.getCompletedAt());
//    }
//
//    @Test
//    void testFinalPriceMustBePositive() {
//        assertThrows(IllegalArgumentException.class,
//            () -> new History(10, "user01", -100.0, completedAt));
//    }
//
//    @Test
//    void testWinnerAccountNotNull() {
//        assertNotNull(history.getWinnerAccountName());
//    }
//
//    @Test
//    void testCompletedAtNotNull() {
//        assertNotNull(history.getCompletedAt());
//    }
//
//    @Test
//    void testNullWinnerAccountThrows() {
//        assertThrows(IllegalArgumentException.class,
//            () -> new History(10, null, 500.0, completedAt));
//    }
//
//    @Test
//    void testCompletedAtNotInFuture() {
//        assertFalse(history.getCompletedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
//    }
//}