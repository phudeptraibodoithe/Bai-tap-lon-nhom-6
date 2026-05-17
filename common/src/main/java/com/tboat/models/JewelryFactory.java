package com.tboat.models;

public class JewelryFactory extends ItemFactory {
    @Override
    public Item createItem(String sellerAccountName, String name,
                           String description, String imageURL) {
        return new JewelryItem(sellerAccountName, name, description, imageURL);
    }

    @Override
    public Item createItem() {
        return new JewelryItem();
    }
}
