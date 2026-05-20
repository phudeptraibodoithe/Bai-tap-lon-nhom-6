package com.tboat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tboat.exception.AuctionBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tboat.models.auction.Participation;

public class ParticipationTest {

    private Participation participation;

    @BeforeEach
    void setUp() {
        participation = new Participation("user01", 10, "BIDDER");
    }

    @Test
    void testGetAccountName() {
        assertEquals("user01", participation.getAccountName());
    }

    @Test
    void testGetAuctionSessionId() {
        assertEquals(10, participation.getAuctionSessionId());
    }

    @Test
    void testGetRoleType() {
        assertEquals("BIDDER", participation.getRoleType());
    }

    @Test
    void testAccountNameNotNull() {
        assertNotNull(participation.getAccountName());
    }

    @Test
    void testRoleTypeNotNull() {
        assertNotNull(participation.getRoleType());
    }

    @Test
    void testEqualityByAccountAndSession() {
        Participation p2 = new Participation("user01", 10, "OBSERVER");
        assertEquals(participation.getAccountName(), p2.getAccountName());
        assertEquals(participation.getAuctionSessionId(), p2.getAuctionSessionId());
    }

    @Test
    void testAccountNameNotEmpty() {
        assertThrows(AuctionBusinessException.class,
                () -> new Participation("", 10, "BIDDER"));
    }

    @Test
    void testNullRoleTypeThrows() {
        assertThrows(AuctionBusinessException.class,
                () -> new Participation("user01", 10, null));
    }

    @Test
    void testAuctionSessionIdPositive() {
        assertThrows(AuctionBusinessException.class,
                () -> new Participation("user01", -1, "BIDDER"));
    }
}