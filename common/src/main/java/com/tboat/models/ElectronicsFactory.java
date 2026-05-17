package com.tboat.models;

public class ElectronicsFactory extends ItemFactory {
    @Override 
    public Item createItem(String sellerAccountName, String name,
                           String description, String imageURL) {
        return new ElectronicsItem(sellerAccountName, name, description, imageURL);
    }

    @Override
    public Item createItem() {
        return new ElectronicsItem();
    }
}
