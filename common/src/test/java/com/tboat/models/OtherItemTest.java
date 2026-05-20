//package com.tboat.models;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertInstanceOf;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import com.tboat.models.item.Item;
//import com.tboat.models.item.OtherItem;
//
//public class OtherItemTest {
//
//    private OtherItem item;
//
//    @BeforeEach
//    void setUp() {
//        item = new OtherItem("seller04", "Antique Vase",
//                             "Ming Dynasty Vase", "https://img/vase.jpg");
//    }
//
//    @Test
//    void testTypeIsOther() {
//        assertEquals("Other", item.getType());
//    }
//
//    @Test
//    void testIsInstanceOfItem() {
//        assertInstanceOf(Item.class, item);
//    }
//
//    @Test
//    void testGetId() {
//        assertEquals(4, item.getId());
//    }
//}
