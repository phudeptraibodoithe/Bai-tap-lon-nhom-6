package com.tboat.models;

public abstract class Item {
    private String sellerAccountName;
    private String type;
    private String name;
    private String description;
    private String imageURL;

    public Item(String sellerAccountName, String type, String name, String description, String imageURL) {
        this.sellerAccountName = sellerAccountName;
        this.type = type;
        this.name = name;
        this.description = description;
        this.imageURL = imageURL;
    }
    public Item() {}

    // Getters
    public String getSellerAccountName() { return sellerAccountName; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageURL() { return imageURL; }

    // Setters
    public void setSellerAccountName(String sellerAccountName) { this.sellerAccountName = sellerAccountName; }
    public void setType(String type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }
}
