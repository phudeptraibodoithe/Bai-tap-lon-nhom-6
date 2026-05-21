package com.tboat.models;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tboat.exception.AuctionBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.tboat.models.auction.Bid;

class BidTest {

    private Bid bid;
    private LocalDateTime bidTime;

    @BeforeEach
    void setUp() {
        bidTime = LocalDateTime.now();
        bid = new Bid(1, 10, "user01", 750.0, bidTime);
    }

    @Test
    void testGetBidderAccount() {
        assertEquals("user01", bid.getBidderAccount());
    }

    @Test
    void testGetBidAmount() {
        assertEquals(750.0, bid.getBidAmount(), 0.001);
    }

    @Test
    void testGetBidTime() {
        assertEquals(bidTime, bid.getBidTime());
    }

    @Test
    void testBidAmountMustBePositive() {
        AuctionBusinessException exception = assertThrows(AuctionBusinessException.class,
                () -> new Bid(2, 10, "user02", 0.0, bidTime));
        assertEquals("ERR_BID_01", exception.getErrorCode());
    }

    @Test
    void testBidAmountNegativeThrows() {
        AuctionBusinessException exception = assertThrows(AuctionBusinessException.class,
                () -> new Bid(3, 10, "user03", -50.0, bidTime));
        assertEquals("ERR_BID_01", exception.getErrorCode());
    }

    @Test
    void testBidTimeNotNull() {
        assertNotNull(bid.getBidTime());
    }

    @Test
    void testBidderAccountNotNull() {
        assertNotNull(bid.getBidderAccount());
    }

    @Test
    void testBidderAccountNotEmpty() {
        AuctionBusinessException exception = assertThrows(AuctionBusinessException.class,
                () -> new Bid(4, 10, "", 100.0, bidTime));
        assertEquals("ERR_USER_01", exception.getErrorCode());
    }
}