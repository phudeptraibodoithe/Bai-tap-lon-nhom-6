package com.tboat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tboat.models.item.FashionItem;
import com.tboat.models.item.Item;

class FashionItemTest {
 
    private FashionItem item;
 
    @BeforeEach
    void setUp() {
        item = new FashionItem("seller02", "Nike Air Max",
                               "Limited edition sneaker", "https://img/nike.jpg");
    }
 
    @Test
    void testTypeIsFashion() {
        assertEquals("Thời trang", item.getType());
    }
 
    @Test
    void testIsInstanceOfItem() {
        assertInstanceOf(Item.class, item);
    }
 
    @Test
    void testGetName() {
        assertEquals("Nike Air Max", item.getName());
    }
}