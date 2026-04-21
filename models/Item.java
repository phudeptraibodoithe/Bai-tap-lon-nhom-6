package models;

public class Item {
    // Attributes
    private int id;
    private String sellerAccountName; 
    private String type;
    private String name;
    private String description;
    private String imageURL;

    // Có thể tự thêm Constructor và Getters/Setters để sử dụng
    public Item(int id, String sellerAccountName, String name, String type) {
        this.id = id;
        this.sellerAccountName = sellerAccountName;
        this.name = name;
        this.type = type;
    }
}