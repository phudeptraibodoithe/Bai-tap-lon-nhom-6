package com.tboat.models;

public class FashionFactory extends ItemFactory {
    @Override
    public Item createItem(String sellerAccountName, String name,
                           String description, String imageURL) {
        return new FashionItem(sellerAccountName, name, description, imageURL);
    }

    @Override
    public Item createItem() {
        return new FashionItem();
    }
}
