package com.tboat.models.item.factory;

import com.tboat.models.item.Item;
import com.tboat.models.item.OtherItem;

public class OtherFactory extends ItemFactory {
    @Override
    public Item createItem(String sellerAccountName, String name,
                           String description, String imageURL) {
        return new OtherItem(sellerAccountName, name, description, imageURL);
    }

    @Override
    public Item createItem() {
        return new OtherItem();
    }
}
