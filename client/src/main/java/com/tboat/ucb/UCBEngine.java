package com.tboat.ucb;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Thuật toán UCB1 — học mẫu điều hướng của người dùng.
 * Công thức: UCB(a) = Q(a) + C * sqrt(ln(N) / n(a))
 *   Q(a) = tỉ lệ cache hit (phần thưởng trung bình)
 *   N    = tổng số lần điều hướng
 *   n(a) = số lần đi từ A → màn hình đó
 *   C    = hệ số cân bằng giữa khám phá và khai thác
 */
public class UCBEngine {

    private static final Logger log = Logger.getLogger(UCBEngine.class.getName());
    private static UCBEngine instance;
    private static final int INITIAL_NAVIGATION_COUNT = 0;
    private static final int VISIT_INDEX = 0;
    private static final int REWARD_INDEX = 1;
    private static final int FIRST_VISIT_COUNT = 0;
    private static final int CACHE_HIT_REWARD = 1;
    private static final int CACHE_MISS_REWARD = 0;
    private static final int SEEDED_TOTAL_NAVIGATIONS = 80;
    private static final double EXPLORATION_BALANCE = 1.5;

    // stats.get("adminFxml").get("homeFxml") = [số lần đi qua, tổng phần thưởng]
    private Map<String, Map<String, int[]>> stats = new HashMap<>();
    private int totalNavigations = INITIAL_NAVIGATION_COUNT;

    // Dùng API Preferences của Java để lưu vào registry/thư mục người dùng, không cần quản lý file
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
        if (totalNavigations == FIRST_VISIT_COUNT || visits == FIRST_VISIT_COUNT) {
            return Double.MAX_VALUE;
        }
        double exploitation = (double) totalReward / visits;
        double exploration  = EXPLORATION_BALANCE * Math.sqrt(Math.log(totalNavigations) / visits);
        return exploitation + exploration;
    }

    // ─── Lấy top-K màn hình nên tải trước khi đang ở `currentScreen` ────────
    public List<String> getTopScreensToPrefetch(String currentScreen, int topK) {
        Map<String, int[]> fromStats = stats.get(currentScreen);
        if (fromStats == null || fromStats.isEmpty()) {
            log.info("[UCB] Chưa có dữ liệu cho màn: " + currentScreen);
            return Collections.emptyList();
        }
        return fromStats.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(
                        e.getKey(),
                        calculateUCB(e.getValue()[VISIT_INDEX], e.getValue()[REWARD_INDEX])))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ─── Ghi nhận 1 lần điều hướng và kết quả (có dùng được cache hay không) ─
    public void recordNavigation(String fromScreen, String toScreen, boolean cacheHit) {
        if (fromScreen == null || fromScreen.equals(toScreen)) return;

        stats.computeIfAbsent(fromScreen, k -> new HashMap<>())
                .computeIfAbsent(toScreen, k -> new int[]{FIRST_VISIT_COUNT, CACHE_MISS_REWARD});

        int[] data = stats.get(fromScreen).get(toScreen);
        data[VISIT_INDEX]++;
        data[REWARD_INDEX] += cacheHit ? CACHE_HIT_REWARD : CACHE_MISS_REWARD;

        totalNavigations++;
        saveToPrefs();

        log.info(String.format("[UCB] %s → %s | visits=%d | reward=%d | UCB=%.3f",
                fromScreen, toScreen, data[VISIT_INDEX], data[REWARD_INDEX],
                calculateUCB(data[VISIT_INDEX], data[REWARD_INDEX])));
    }

    // ─── Gieo dữ liệu mặc định (dùng khi khởi động nguội, tuần đầu) ─────────
    public void seedDefaultPatterns() {
        seedEdge("startFxml",    "TrangChuFxml",    20, 18);
        seedEdge("startFxml",    "loginFxml",       20, 16);
        seedEdge("loginFxml",    "TrangChuFxml",    25, 22);
        seedEdge("loginFxml",    "adminFxml",        5,  4);
        seedEdge("TrangChuFxml", "auctionFxml",     30, 25);
        seedEdge("TrangChuFxml", "historyFxml",     15, 10);
        seedEdge("TrangChuFxml", "managerFxml",     12,  8);
        seedEdge("TrangChuFxml", "profileFxml",     10,  7);
        seedEdge("adminFxml",    "adminWalletFxml", 10,  8);
        totalNavigations = Math.max(totalNavigations, SEEDED_TOTAL_NAVIGATIONS);
        saveToPrefs();
    }

    private void seedEdge(String from, String to, int visits, int reward) {
        stats.computeIfAbsent(from, k -> new HashMap<>())
                .put(to, new int[]{visits, reward});
    }

    // ─── Lưu bền vững ───────────────────────────────────────────────────────
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
                totalNavigations = prefs.getInt(PREFS_KEY_TOTAL, INITIAL_NAVIGATION_COUNT);
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
