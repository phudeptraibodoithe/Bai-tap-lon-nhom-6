package com.tboat.service;

import java.util.*;
import java.util.concurrent.*;

public class AuctionManager {
    private static AuctionManager instance;
    private Map<String, AuctionRoom> activeRooms = new ConcurrentHashMap<>();

    private AuctionManager() {}

    //Mau Singleton
    public static synchronized AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    public AuctionRoom getRoom(String roomName) {
        // Chỉ trả về phòng nếu nó đã tồn tại trong Map
        return activeRooms.get(roomName);
    }

    // Thêm hàm để Server chủ động tạo phòng từ Database khi khởi động
    public void createRoom(String roomName, double initialPrice) {
        activeRooms.put(roomName, new AuctionRoom(roomName, initialPrice));
    }
}