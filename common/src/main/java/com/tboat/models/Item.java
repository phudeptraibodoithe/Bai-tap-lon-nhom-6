package com.tboat.models;

public abstract class Item {
    private int id;
    private String sellerAccountName;
    private String name;
    private String description;
    private String imageURL;
    private String type;

    public Item(String sellerAccountName, String name, String description, String imageURL) {
        this.sellerAccountName = sellerAccountName;
        this.name = name;
        this.description = description;
        this.imageURL = imageURL;
        this.type = getType();
    }
    public Item() {}

    // Getters
    public String getSellerAccountName() { return sellerAccountName; }
    public abstract String getType();
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageURL() { return imageURL; }

    // Setters
    public void setSellerAccountName(String sellerAccountName) { this.sellerAccountName = sellerAccountName; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }
}
