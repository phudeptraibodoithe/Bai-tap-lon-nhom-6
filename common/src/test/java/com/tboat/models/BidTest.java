//package com.tboat.models;
//
//import java.time.LocalDateTime;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import com.tboat.models.auction.Bid;
//
//class BidTest {
//
//    private Bid bid;
//    private LocalDateTime bidTime;
//
//    @BeforeEach
//    void setUp() {
//        bidTime = LocalDateTime.now();
//        bid = new Bid(1, 10, "user01", 750.0, bidTime);
//    }
//
//    @Test
//    void testGetBidderAccount() {
//        assertEquals("user01", bid.getBidderAccount());
//    }
//
//    @Test
//    void testGetBidAmount() {
//        assertEquals(750.0, bid.getBidAmount(), 0.001);
//    }
//
//    @Test
//    void testGetBidTime() {
//        assertEquals(bidTime, bid.getBidTime());
//    }
//
//    @Test
//    void testBidAmountMustBePositive() {
//        assertThrows(IllegalArgumentException.class,
//            () -> new Bid(2, 10, "user02", 0.0, bidTime));
//    }
//
//    @Test
//    void testBidAmountNegativeThrows() {
//        assertThrows(IllegalArgumentException.class,
//            () -> new Bid(3, 10, "user03", -50.0, bidTime));
//    }
//
//    @Test
//    void testBidTimeNotNull() {
//        assertNotNull(bid.getBidTime());
//    }
//
//    @Test
//    void testBidderAccountNotNull() {
//        assertNotNull(bid.getBidderAccount());
//    }
//
//    @Test
//    void testBidderAccountNotEmpty() {
//        assertThrows(IllegalArgumentException.class,
//            () -> new Bid(4, 10, "", 100.0, bidTime));
//    }
//}