package com.tboat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tboat.models.item.Item;

public class ItemTest {

    static class ConcreteItem extends Item {
        public ConcreteItem(String sellerAccountName, String name, String description, String imageURL) {
            super(sellerAccountName, name, description, imageURL);
        }
        @Override
        public String getType() {
            return "Electronics";
        }
    }

    private ConcreteItem item;

    @BeforeEach
    void setUp() {
        item = new ConcreteItem("user01", "Laptop Dell", "Dell XPS 15", "https://img.com/dell.jpg");
    }

    @Test
    void testGetSellerAccountName() {
        assertEquals("user01", item.getSellerAccountName());
    }

    @Test
    void testGetType() {
        assertEquals("Electronics", item.getType());
    }

    @Test
    void testGetName() {
        assertEquals("Laptop Dell", item.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Dell XPS 15", item.getDescription());
    }

    @Test
    void testGetImageURL() {
        assertEquals("https://img.com/dell.jpg", item.getImageURL());
    }

    @Test
    void testSetName() {
        item.setName("Laptop HP");
        assertEquals("Laptop HP", item.getName());
    }

    @Test
    void testSetDescription() {
        item.setDescription("HP Spectre 360");
        assertEquals("HP Spectre 360", item.getDescription());
    }

    @Test
    void testSetImageURL() {
        item.setImageURL("https://img.com/hp.jpg");
        assertEquals("https://img.com/hp.jpg", item.getImageURL());
    }
}