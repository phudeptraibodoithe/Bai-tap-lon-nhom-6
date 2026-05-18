package com.tboat.models.item.factory;

import com.tboat.models.item.Item;

public abstract class ItemFactory {
    public ItemFactory(){}

    public abstract Item createItem(String sellerAccountName, String name,
                                    String description, String imageURL);
    public abstract Item createItem();
}
