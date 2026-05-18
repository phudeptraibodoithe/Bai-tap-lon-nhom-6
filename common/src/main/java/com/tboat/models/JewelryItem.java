package com.tboat.models;

import java.time.LocalDateTime;

public class JewelryItem extends Item {
    // Constructors
    public JewelryItem() {
        super.setType("Trang sức");
    }

    public JewelryItem(String sellerAccountName, String name, String description, String imageURL) {
        super(sellerAccountName, "Trang sức", name, description, imageURL);
    }
}
