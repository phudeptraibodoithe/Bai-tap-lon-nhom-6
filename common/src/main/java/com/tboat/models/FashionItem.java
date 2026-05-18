package com.tboat.models;

public class FashionItem extends Item {

    // Constructors
    public FashionItem() {
        super.setType("Thời trang");
    }

    public FashionItem(String sellerAccountName, String name, String description, String imageURL) {
        super(sellerAccountName, "Thời trang", name, description, imageURL);
    }
}