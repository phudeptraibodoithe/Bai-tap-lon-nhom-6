package com.tboat.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BidTest {

    @Test
    void testBidCreation() {
        Bid bid = new Bid(1, "user123", 500.0);

        // Luu y: Trong code Bid.java ban dang gan bidTime = LocalDateTime.now() trong constructor
        assertNotNull(bid, "Doi tuong Bid khong duoc null");
        // Neu ban co Getter cho BidAmount thi hay test them:
         assertEquals(500.0, bid.getBidAmount());
    }
}