package com.tboat.ucb;

import com.tboat.models.network.ServerEvent;
import com.tboat.socket.SocketHelper;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Dùng UCB để tải trước dữ liệu của màn hình người dùng có khả năng sắp chuyển đến,
 * gửi request ngầm và cache phản hồi trước khi người dùng bấm.
 */
public class PrefetchManager {

    private static final Logger log = Logger.getLogger(PrefetchManager.class.getName());
    private static PrefetchManager instance;
    private static final int PREFETCH_SCREEN_LIMIT = 2;

    private static final Map<String, List<ServerEvent>> SCREEN_ACTIONS = Map.of(
            "adminFxml",       List.of(ServerEvent.GET_ALL_ITEMS),
            "adminWalletFxml", List.of(ServerEvent.GET_PROFILE),
            "TrangChuFxml",    List.of(ServerEvent.LIST_AVAILABLE),
            "managerFxml",     List.of(ServerEvent.GET_MY_AUCTIONS),
            "historyFxml",     List.of(ServerEvent.GET_HISTORY),
            "profileFxml",     List.of(ServerEvent.GET_PROFILE),
            "NapRutFxml",      List.of(ServerEvent.GET_PROFILE),
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
     * UCB sẽ tự tính toán và tải trước những màn hình có khả năng cao nhất.
     *
     * @param currentScreen tên màn hình hiện tại (vd: "adminFxml")
     */
    public void onScreenEntered(String currentScreen) {
        List<String> topScreens = UCBEngine.getInstance()
                .getTopScreensToPrefetch(currentScreen, PREFETCH_SCREEN_LIMIT);

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
        List<ServerEvent> actions = SCREEN_ACTIONS.get(screenName);
        if (actions == null) {
            log.warning("[Prefetch] Không tìm thấy actions cho screen: " + screenName);
            return;
        }

        for (ServerEvent action : actions) {
            if (DataCache.getInstance().isFresh(action)) {
                log.fine("[Prefetch] Bỏ qua " + action.name() + " (cache còn fresh)");
                continue;
            }

            // Gửi request ngầm, phản hồi sẽ được bộ chặn cache lưu lại
            SocketHelper.sendRequest(action);
            log.info("[Prefetch] → Gửi prefetch request: " + action.name());
        }
    }
}
