package com.tboat.models.item;

public class FashionItem extends Item {

    // Constructors
    public FashionItem() {
        super();
    }

    public FashionItem(String sellerAccountName, String name, String description, String imageURL) {
        super(sellerAccountName,  name, description, imageURL);
    }

    @Override
    public String getType() {
        return "Thời trang";
    }
}