package com.tboat.models.item;

public class OtherItem extends Item {
    // Constructors
    public OtherItem() {
        super();
    }

    public OtherItem(String sellerAccountName, String name,
                    String description, String imageURL) {
        super(sellerAccountName,  name, description, imageURL);
    }

    @Override
    public String getType() {
        return "Khác";
    }
}
