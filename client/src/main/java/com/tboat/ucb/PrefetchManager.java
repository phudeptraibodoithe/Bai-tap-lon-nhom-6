package com.tboat.ucb;

import com.google.gson.JsonObject;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.SocketManager;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Dùng UCB để prefetch data của màn hình user SẮP chuyển đến,
 * gửi request ngầm và cache response trước khi user bấm.
 */
public class PrefetchManager {

    private static final Logger log = Logger.getLogger(PrefetchManager.class.getName());
    private static PrefetchManager instance;

    private static final Map<String, List<String>> SCREEN_ACTIONS = Map.of(
            "adminFxml",       List.of(ServerEvent.GET_ALL_ITEMS.name()),   // "GET_PENDING_ITEMS" không có trong enum → xem lại
            "adminWalletFxml", List.of(ServerEvent.GET_PROFILE.name()),     // "PROFILE" → GET_PROFILE
            "TrangChuFxml",    List.of(ServerEvent.LIST_AVAILABLE.name()),
            "managerFxml",     List.of(ServerEvent.GET_MY_AUCTIONS.name()),
            "historyFxml",     List.of(ServerEvent.GET_HISTORY.name()),
            "profileFxml",     List.of(ServerEvent.GET_PROFILE.name()),     // "PROFILE" → GET_PROFILE
            "NapRutFxml",      List.of(ServerEvent.GET_PROFILE.name()),     // "PROFILE" → GET_PROFILE
            "editItemFxml",    List.of(),
            "postItemFxml",    List.of(),
            "auctionFxml",     List.of()
    );

    private PrefetchManager() {}

    public static synchronized PrefetchManager getInstance() {
        if (instance == null) instance = new PrefetchManager();
        return instance;
    }

    /**
     * Gọi hàm này ngay sau khi user đến màn hình mới.
     * UCB sẽ tự tính toán và prefetch những màn hình có khả năng cao nhất.
     *
     * @param currentScreen tên màn hình hiện tại (vd: "adminFxml")
     */
    public void onScreenEntered(String currentScreen) {
        List<String> topScreens = UCBEngine.getInstance()
                .getTopScreensToPrefetch(currentScreen, 2); // Prefetch top 2

        if (topScreens.isEmpty()) {
            log.info("[Prefetch] Không đủ dữ liệu UCB để prefetch từ: " + currentScreen);
            return;
        }

        log.info("[Prefetch] Từ [" + currentScreen + "] → sẽ prefetch: " + topScreens);

        for (String targetScreen : topScreens) {
            prefetchScreen(targetScreen);
        }
    }

    private void prefetchScreen(String screenName) {
        List<String> actions = SCREEN_ACTIONS.get(screenName);
        if (actions == null) {
            log.warning("[Prefetch] Không tìm thấy actions cho screen: " + screenName);
            return;
        }

        for (String action : actions) {
            if (DataCache.getInstance().isFresh(action)) {
                log.fine("[Prefetch] Bỏ qua " + action + " (cache còn fresh)");
                continue;
            }

            // Gửi request ngầm — response sẽ được cache bởi CacheInterceptor
            JsonObject request = new JsonObject();
            request.addProperty("action", action);
            SocketManager.getInstance().send(request.toString());
            log.info("[Prefetch] → Gửi prefetch request: " + action);
        }
    }
}