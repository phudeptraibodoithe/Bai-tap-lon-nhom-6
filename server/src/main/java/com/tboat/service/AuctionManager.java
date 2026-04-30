package com.tboat.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map<String, AuctionRoom> activeRooms = new ConcurrentHashMap<>();
    private AuctionManager() {}
    public static AuctionManager getInstance() {
        if (instance == null) { // Kiểm tra lần 1 không cần khóa (nhanh)
            synchronized (AuctionManager.class) {
                if (instance == null) { // Kiểm tra lần 2 có khóa (an toàn)
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public AuctionRoom getRoom(String roomName) {
        return activeRooms.get(roomName);
    }
    public void createRoom(String roomName, double initialPrice) {
        activeRooms.putIfAbsent(roomName, new AuctionRoom(roomName, initialPrice));
    }
    public void removeRoom(String roomName) {
        activeRooms.remove(roomName);
        System.out.println("[AuctionManager]: Đã giải phóng phòng " + roomName + " khỏi bộ nhớ.");
    }
}