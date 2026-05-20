//package com.tboat.models;
//
//import java.time.LocalDateTime;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import com.tboat.models.auction.AuctionSession;
//import com.tboat.models.auction.StatusOfAuction;
//import com.tboat.models.item.Item;
//import com.tboat.models.item.OtherItem;
//
//class AuctionSessionTest {
//
//    private AuctionSession session;
//    private LocalDateTime start;
//    private LocalDateTime end;
//    private Item item;
//
//    @BeforeEach
//    void setUp() {
//        start = LocalDateTime.now().plusHours(1);
//        end = LocalDateTime.now().plusHours(3);
//        item = new OtherItem("seller01", "Test Item", "Test Description", "https://img/test.jpg");
//        session = new AuctionSession(start, end,500.0, 10.0, item);
//    }
//
//    @Test
//    void testGetId() {
//        assertEquals(1, session.getId());
//    }
//
//    @Test
//    void testGetItemId() {
//        assertEquals(101, session.getItemId());
//    }
//
//    @Test
//    void testGetStartTime() {
//        assertEquals(start, session.getStartTime());
//    }
//
//    @Test
//    void testGetEndTime() {
//        assertEquals(end, session.getEndTime());
//    }
//
//    @Test
//    void testGetCurrentPrice() {
//        assertEquals(500.0, session.getCurrentPrice(), 0.001);
//    }
//
//    @Test
//    void testGetBidIncrease() {
//        assertEquals(10.0, session.getBidIncrease(), 0.001);
//    }
//
//    @Test
//    void testGetStatus() {
//        assertEquals(StatusOfAuction.NOT_STARTED, session.getStatusOfAuction());
//    }
//
//    @Test
//    void testGetHighestBidderAccountInitiallyNull() {
//        assertNull(session.getHighestBidderAccount());
//    }
//
//    @Test
//    void testSetCurrentPrice() {
//        session.setCurrentPrice(600.0);
//        assertEquals(600.0, session.getCurrentPrice(), 0.001);
//    }
//
//    @Test
//    void testSetStatus() {
//        session.setStatusOfAuction(StatusOfAuction.ONGOING);
//        assertEquals(StatusOfAuction.ONGOING, session.getStatusOfAuction());
//    }
//
//    @Test
//    void testSetHighestBidderAccount() {
//        session.setHighestBidderAccount("user01");
//        assertEquals("user01", session.getHighestBidderAccount());
//    }
//
//    @Test
//    void testEndTimeAfterStartTime() {
//        assertTrue(session.getEndTime().isAfter(session.getStartTime()));
//    }
//
//    @Test
//    void testSetEndTime() {
//        LocalDateTime newEnd = end.plusHours(2);
//        session.setEndTime(newEnd);
//        assertEquals(newEnd, session.getEndTime());
//    }
//
//    @Test
//    void testCurrentPriceCannotBeNegative() {
//        assertThrows(IllegalArgumentException.class,
//            () -> session.setCurrentPrice(-1.0));
//    }
//
//    @Test
//    void testBidIncreaseCannotBeZero() {
//        assertThrows(IllegalArgumentException.class,
//            () -> new AuctionSession(start, end, 100.0, 0.0, item));
//    }
//}