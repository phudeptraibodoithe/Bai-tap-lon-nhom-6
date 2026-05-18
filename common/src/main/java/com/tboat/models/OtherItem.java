package com.tboat.models;

import java.time.LocalDateTime;

public class OtherItem extends Item {
    // Constructors
    public OtherItem() {
        super.setType("Khác");
    }

    public OtherItem(String sellerAccountName, String name,
                    String description, String imageURL) {
        super(sellerAccountName, "Khác", name, description, imageURL);
    }
}
