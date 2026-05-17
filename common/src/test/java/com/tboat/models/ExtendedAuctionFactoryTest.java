//package com.tboat.models;
//
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
//import java.time.LocalDateTime;
//
//public class ExtendedAuctionFactoryTest {
//
//    @Test
//    void testAllFactoriesViaProducer() {
//        // Test Trang sức
//        AuctionFactory jewelryFactory = AuctionFactoryProducer.getFactory("Trang sức");
//        assertTrue(jewelryFactory instanceof JewelryFactory);
//
//        // Test Khác/Default
//        AuctionFactory otherFactory = AuctionFactoryProducer.getFactory("Bất kỳ");
//        assertTrue(otherFactory instanceof OtherFactory);
//    }
//
//    @Test
//    void testJewelryAuctionCreation() {
//        JewelryFactory factory = new JewelryFactory();
//        AuctionSession session = factory.createAuctionSession(
//                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
//                2000.0, 100.0, "seller", "Nhan cuoi", "Vang 24k", "url"
//        );
//
//        assertEquals("Trang sức", session.getType());
//        assertTrue(session instanceof JewelryAuction);
//    }
//}