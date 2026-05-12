package com.tboat.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

public class AuctionFactoryTest {

    @Test
    void testAuctionFactoryProducer() {
        // Kiểm tra xem nhập "Điện tử" có ra ElectronicsFactory không
        AuctionFactory factory = AuctionFactoryProducer.getFactory("Điện tử");
        assertTrue(factory instanceof ElectronicsFactory, "Phai tra ve ElectronicsFactory cho loai Dien tu");

        // Kiểm tra giá trị null hoặc không khớp
        AuctionFactory defaultFactory = AuctionFactoryProducer.getFactory(null);
        assertNotNull(defaultFactory, "Factory mac dinh khong duoc null");
    }

    @Test
    void testCreateElectronicsAuction() {
        AuctionFactory factory = new ElectronicsFactory();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        // Kiểm tra việc tạo đối tượng cụ thể qua Factory
        AuctionSession session = factory.createAuctionSession(
                start, end, 1000.0, 50.0, "seller1", "iPhone 15", "Mo ta", "url"
        );

        assertNotNull(session);
        assertEquals("iPhone 15", session.getName());
        assertEquals(1000.0, session.getCurrentPrice());
    }
}