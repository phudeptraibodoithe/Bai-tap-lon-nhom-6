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
        // Tự động tạo phòng mới nếu chưa tồn tại (Hỗ trợ 3.1.2)
        return activeRooms.computeIfAbsent(roomName, name -> new AuctionRoom(name, 0));
    }
}