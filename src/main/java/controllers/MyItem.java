package controllers;

public class MyItem {
    private String id;
    private String name;
    private String role; // Ví dụ: "Người đăng" hoặc "Người đấu giá"
    private double currentPrice;
    private String status; // Ví dụ: "Đang diễn ra", "Chờ duyệt", "Đang dẫn đầu", "Đã kết thúc"

    public MyItem(String id, String name, String role, double currentPrice, String status) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.currentPrice = currentPrice;
        this.status = status;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public double getCurrentPrice() { return currentPrice; }
    public String getStatus() { return status; }
}