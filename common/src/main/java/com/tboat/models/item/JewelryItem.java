package com.tboat.models.item;

public class JewelryItem extends Item {
    // Constructors
    public JewelryItem() {
        super();
    }

    public JewelryItem(String sellerAccountName, String name, String description, String imageURL) {
        super(sellerAccountName, name, description, imageURL);
    }

    @Override
    public String getType() {
        return "Trang sức";
    }
}
