package com.tboat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.tboat.models.auction.StatusOfAuction;

public class StatusOfAuctionTest {
 
    @Test
    void testAllStatusValues() {
        StatusOfAuction[] values = StatusOfAuction.values();
        assertEquals(5, values.length);
        assertNotNull(StatusOfAuction.valueOf("NOT_STARTED"));
        assertNotNull(StatusOfAuction.valueOf("ONGOING"));
        assertNotNull(StatusOfAuction.valueOf("ENDED"));
        assertNotNull(StatusOfAuction.valueOf("PENDING"));
        assertNotNull(StatusOfAuction.valueOf("CANCELED"));
    }
 
    @Test
    void testInvalidStatusThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> StatusOfAuction.valueOf("INVALID_STATUS"));
    }
}