package com.tboat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tboat.models.item.Item;
import com.tboat.models.item.JewelryItem;

class JewelryItemTest {
 
    private JewelryItem item;
 
    @BeforeEach
    void setUp() {
        item = new JewelryItem("seller03", "Gold Ring",
                               "24K Gold Ring 2g", "https://img/ring.jpg");
    }
 
    @Test
    void testTypeIsJewelry() {
        assertEquals("Trang sức", item.getType());
    }
 
    @Test
    void testIsInstanceOfItem() {
        assertInstanceOf(Item.class, item);
    }
 
    @Test
    void testGetDescription() {
        assertEquals("24K Gold Ring 2g", item.getDescription());
    }
}