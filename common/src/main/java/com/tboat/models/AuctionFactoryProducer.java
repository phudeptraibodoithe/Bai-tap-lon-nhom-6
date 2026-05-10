package com.tboat.models;

public class AuctionFactoryProducer {
    public static AuctionFactory getFactory(String type) {
        if (type == null) {
            return new OtherFactory();
        }
        switch (type.trim()) {
            case "Điện tử":
                return new ElectronicsFactory();
            case "Thời trang":
                return new FashionFactory();
            case "Trang sức":
                return new JewelryFactory();
            case "Khác":
            default:
                return new OtherFactory();
        }
    }
}