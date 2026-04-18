package models;

public class Item {
    // Attributes
    private int id;
    private int sellerId; 
    private String type;
    private String name;
    private String description;
    private String imageURL;

    // Có thể tự thêm Constructor và Getters/Setters để sử dụng
    public Item(int id, int sellerId, String name, String type) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.type = type;
    }
}