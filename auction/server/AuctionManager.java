package com.auction.server;

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
        // Tự động tạo phòng mới nếu chưa tồn tại
        return activeRooms.computeIfAbsent(roomName, name -> new AuctionRoom(name, 0));
    }
}