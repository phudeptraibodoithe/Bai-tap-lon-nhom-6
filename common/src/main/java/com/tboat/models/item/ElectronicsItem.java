package com.tboat.models.item;

public class ElectronicsItem extends Item {

    // Constructor dùng cho Factory
    public ElectronicsItem( String sellerAccountName, String name,
                            String description, String imageURL) {
        super(sellerAccountName, name, description, imageURL);
    }

    public ElectronicsItem(){super();}

    @Override
    public String getType() {
        return "Điện tử";
    }
}