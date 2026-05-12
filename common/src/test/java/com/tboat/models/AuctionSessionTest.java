package com.tboat.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

public class AuctionSessionTest {

    @Test
    void testUpdateStatusOngoing() {
        // Thiet lap thoi gian bat dau la qua khu, ket thuc la tuong lai -> Phai la ONGOING
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        // Dung mot lop con cu the de test logic cua lop cha AuctionSession
        AuctionSession session = new ElectronicsAuction(start, end, 100.0, 10.0, "seller", "Test", "Desc", "url");

        assertEquals(StatusOfAuction.ONGOING, session.getStatusOfAuction(),
                "Phien dau gia phai o trang thai ONGOING khi thoi gian hien tai nam trong khoang Start-End");
    }

    @Test
    void testUpdateStatusEnded() {
        // Thoi gian ket thuc da qua -> Phai la ENDED
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        AuctionSession session = new ElectronicsAuction(start, end, 100.0, 10.0, "seller", "Test", "Desc", "url");

        assertEquals(StatusOfAuction.ENDED, session.getStatusOfAuction(),
                "Phien dau gia phai o trang thai ENDED khi thoi gian ket thuc da qua");
    }
}