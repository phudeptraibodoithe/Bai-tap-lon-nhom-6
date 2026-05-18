package com.tboat.models;

public class ElectronicsItem extends Item {

    public ElectronicsItem() {
        super.setType("Điện tử");
    }
    // Constructor dùng cho Factory
    public ElectronicsItem( String sellerAccountName, String name,
                            String description, String imageURL) {
        super(sellerAccountName, "Điện tử", name, description, imageURL);
    }
}