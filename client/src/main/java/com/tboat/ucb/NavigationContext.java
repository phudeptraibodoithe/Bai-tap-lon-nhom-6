package com.tboat.ucb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NavigationContext {

    private static NavigationContext instance;
    private final Map<String, Boolean> cacheHitMap = new ConcurrentHashMap<>();

    private NavigationContext() {}

    public static synchronized NavigationContext getInstance() {
        if (instance == null) instance = new NavigationContext();
        return instance;
    }

    /** Controller gọi hàm này khi biết mình có cache hit hay không */
    public void reportCacheHit(String screenKey, boolean hit) {
        cacheHitMap.put(screenKey, hit);
    }

    /** BaseController gọi sau khi navigate để lấy kết quả (1 lần dùng 1 lần xóa) */
    public boolean popCacheHit(String screenKey) {
        Boolean result = cacheHitMap.remove(screenKey); // Lấy & xóa 1 lần
        return result != null && result;                // null-safe
    }
}