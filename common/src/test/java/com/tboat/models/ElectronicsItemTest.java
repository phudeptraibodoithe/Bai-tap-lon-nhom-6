package com.tboat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tboat.models.item.ElectronicsItem;
import com.tboat.models.item.Item;

class ElectronicsItemTest {
 
    private ElectronicsItem item;
 
    @BeforeEach
    void setUp() {
        item = new ElectronicsItem("seller01", "iPhone 15",
                                   "Apple iPhone 15 128GB", "https://img/iphone.jpg");
    }
 
    @Test
    void testTypeIsElectronics() {
        assertEquals("Điện tử", item.getType());
    }
 
    @Test
    void testIsInstanceOfItem() {
        assertInstanceOf(Item.class, item);
    }
 
    @Test
    void testGetName() {
        assertEquals("iPhone 15", item.getName());
    }
 
    @Test
    void testGetSeller() {
        assertEquals("seller01", item.getSellerAccountName());
    }
}