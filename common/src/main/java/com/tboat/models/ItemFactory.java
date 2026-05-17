package com.tboat.models;

public abstract class ItemFactory {
    public ItemFactory(){}

    public abstract Item createItem(String sellerAccountName, String name, 
                                    String description, String imageURL);
    public abstract Item createItem();
}
