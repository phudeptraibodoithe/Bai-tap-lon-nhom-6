package controllers;

public class AuctionSession {
    private String id;
    private String name;
    private double startPrice;
    private double jump;
    private String seller;

    public AuctionSession(String id, String name, double startPrice, double jump, String seller) {
        this.id = id;
        this.name = name;
        this.startPrice = startPrice;
        this.jump = jump;
        this.seller=seller;
    }

    // Các Getter bắt buộc phải có để TableView đọc được dữ liệu
    public String getId() { return id; }
    public String getName() { return name; }
    public double getStartPrice() { return startPrice; }
    public double getJump() { return jump; }
    public String getSeller(){ return seller; };
}