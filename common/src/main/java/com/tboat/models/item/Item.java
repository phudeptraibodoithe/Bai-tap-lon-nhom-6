package com.tboat.models.item;

public abstract class Item {
    private int id;
    protected String sellerAccountName;
    protected String name;
    protected String description;
    protected String imageURL;
    protected String type;

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
    public int getId() {
        return id;
    }

    // Setters
    public void setSellerAccountName(String sellerAccountName) { this.sellerAccountName = sellerAccountName; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }
    public void setId(int id) {
        this.id = id;
    }
}
