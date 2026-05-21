package com.tboat.ucb;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Thuật toán UCB1 — Học pattern điều hướng của user.
 * Công thức: UCB(a) = Q(a) + C * sqrt(ln(N) / n(a))
 *   Q(a) = tỉ lệ cache hit (reward trung bình)
 *   N    = tổng số lần navigate
 *   n(a) = số lần đi từ A → màn hình đó
 *   C    = hệ số cân bằng exploration/exploitation
 */
public class UCBEngine {

    private static final Logger log = Logger.getLogger(UCBEngine.class.getName());
    private static UCBEngine instance;

    // stats.get("adminFxml").get("homeFxml") = [visits, totalReward]
    private Map<String, Map<String, int[]>> stats = new HashMap<>();
    private int totalNavigations = 0;
    private static final double C = 1.5;

    // Dùng Java Preferences API để lưu vào registry/user home — không cần quản lý file
    private static final Preferences prefs = Preferences.userNodeForPackage(UCBEngine.class);
    private static final String PREFS_KEY_STATS = "ucb_stats";
    private static final String PREFS_KEY_TOTAL = "ucb_total";
    private final Gson gson = new Gson();

    private UCBEngine() {
        loadFromPrefs();
    }

    public static synchronized UCBEngine getInstance() {
        if (instance == null) instance = new UCBEngine();
        return instance;
    }

    // ─── Tính điểm UCB cho một cạnh ─────────────────────────────────────────
    private double calculateUCB(int visits, int totalReward) {
        if (totalNavigations == 0 || visits == 0) return Double.MAX_VALUE; // Chưa thử → ưu tiên
        double exploitation = (double) totalReward / visits;
        double exploration  = C * Math.sqrt(Math.log(totalNavigations) / visits);
        return exploitation + exploration;
    }

    // ─── Lấy top-K màn hình nên prefetch khi đang ở `currentScreen` ─────────
    public List<String> getTopScreensToPrefetch(String currentScreen, int topK) {
        Map<String, int[]> fromStats = stats.get(currentScreen);
        if (fromStats == null || fromStats.isEmpty()) {
            log.info("[UCB] Chưa có dữ liệu cho màn: " + currentScreen);
            return Collections.emptyList();
        }
        return fromStats.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(
                        e.getKey(),
                        calculateUCB(e.getValue()[0], e.getValue()[1])))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ─── Ghi nhận 1 lần navigate + kết quả (cache hit hay không) ────────────
    public void recordNavigation(String fromScreen, String toScreen, boolean cacheHit) {
        if (fromScreen == null || fromScreen.equals(toScreen)) return;

        stats.computeIfAbsent(fromScreen, k -> new HashMap<>())
                .computeIfAbsent(toScreen, k -> new int[]{0, 0});

        int[] data = stats.get(fromScreen).get(toScreen);
        data[0]++;                    // visits++
        data[1] += cacheHit ? 1 : 0; // reward++ nếu cache hit

        totalNavigations++;
        saveToPrefs();

        log.info(String.format("[UCB] %s → %s | visits=%d | reward=%d | UCB=%.3f",
                fromScreen, toScreen, data[0], data[1],
                calculateUCB(data[0], data[1])));
    }

    // ─── Seed dữ liệu mặc định (dùng khi cold start, tuần đầu) ─────────────
    public void seedDefaultPatterns() {
        seedEdge("startFxml",    "TrangChuFxml",    20, 18); // ← SỬA
        seedEdge("startFxml",    "loginFxml",       20, 16);
        seedEdge("loginFxml",    "TrangChuFxml",    25, 22); // ← THÊM
        seedEdge("loginFxml",    "adminFxml",        5,  4); // ← THÊM
        seedEdge("TrangChuFxml", "auctionFxml",     30, 25);
        seedEdge("TrangChuFxml", "historyFxml",     15, 10);
        seedEdge("TrangChuFxml", "managerFxml",     12,  8);
        seedEdge("TrangChuFxml", "profileFxml",     10,  7);
        seedEdge("adminFxml",    "adminWalletFxml", 10,  8);
        totalNavigations = Math.max(totalNavigations, 80);
        saveToPrefs();
    }

    private void seedEdge(String from, String to, int visits, int reward) {
        stats.computeIfAbsent(from, k -> new HashMap<>())
                .put(to, new int[]{visits, reward});
    }

    // ─── Persist ─────────────────────────────────────────────────────────────
    private void saveToPrefs() {
        try {
            prefs.put(PREFS_KEY_STATS, gson.toJson(stats));
            prefs.putInt(PREFS_KEY_TOTAL, totalNavigations);
        } catch (Exception e) {
            log.warning("[UCB] Không lưu được prefs: " + e.getMessage());
        }
    }

    private void loadFromPrefs() {
        try {
            String savedStats = prefs.get(PREFS_KEY_STATS, null);
            if (savedStats != null) {
                Type type = new TypeToken<Map<String, Map<String, int[]>>>(){}.getType();
                stats = gson.fromJson(savedStats, type);
                totalNavigations = prefs.getInt(PREFS_KEY_TOTAL, 0);
                log.info("[UCB] Load thành công. Total navigations: " + totalNavigations);
            } else {
                log.info("[UCB] Chưa có dữ liệu lịch sử → seed mặc định.");
                seedDefaultPatterns();
            }
        } catch (Exception e) {
            log.warning("[UCB] Load prefs thất bại, seed lại: " + e.getMessage());
            seedDefaultPatterns();
        }
    }
}