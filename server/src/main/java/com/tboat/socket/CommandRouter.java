package com.tboat.socket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.network.Response;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

public class CommandRouter {

    private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);

    private static final Set<ServerEvent> ADMIN_ACTIONS = EnumSet.of(
            ServerEvent.GET_ALL_ITEMS, ServerEvent.APPROVE_ITEM, ServerEvent.REJECT_ITEM
    );

    private static final Set<ServerEvent> PUBLIC_ACTIONS = EnumSet.of(
            ServerEvent.LOGIN, ServerEvent.REGISTER, ServerEvent.LIST_AVAILABLE
    );

    private final ClientSession context;

    private final AuthCommandHandler    authHandler;
    private final AuctionCommandHandler auctionHandler;
    private final ItemCommandHandler    itemHandler;
    private final UserCommandHandler    userHandler;
    private final HistoryCommandHandler historyHandler;

    public CommandRouter(ClientSession context) {
        this.context        = context;
        this.authHandler    = new AuthCommandHandler(context);
        this.auctionHandler = new AuctionCommandHandler(context);
        this.itemHandler    = new ItemCommandHandler(context);
        this.userHandler    = new UserCommandHandler(context);
        this.historyHandler = new HistoryCommandHandler(context);
    }

    public String dispatch(String rawInput) {
        ServerEvent action = ServerEvent.UNKNOWN;
        String rawAction = ServerEvent.UNKNOWN.name();
        try {
            /*
             * Request đi vào vẫn là text từ socket. Chuyển action sang ServerEvent
             * một lần tại đây để các handler phía sau dùng action có kiểu rõ ràng.
             */
            JsonObject json = JsonParser.parseString(rawInput).getAsJsonObject();
            rawAction = json.get("action").getAsString().toUpperCase();
            try {
                action = ServerEvent.valueOf(rawAction);
            } catch (IllegalArgumentException e) {
                action = ServerEvent.UNKNOWN;
            }

            if (!PUBLIC_ACTIONS.contains(action) && context.isGuest()) {
                context.sendResponse(new Response<>(
                        ServerEvent.AUTH_ERROR, ServerEvent.ERROR,
                        "Vui lòng đăng nhập trước khi thực hiện thao tác này", null));
                return rawAction;
            }

            if (ADMIN_ACTIONS.contains(action) && !context.getClientId().equals("admin")) {
                context.sendResponse(new Response<>(
                        ServerEvent.AUTH_ERROR, ServerEvent.ERROR,
                        "Chỉ Admin mới thực hiện được!", null));
                return rawAction;
            }

            route(action, rawAction, rawInput);

        } catch (Exception e) {
            log.error("Lỗi dispatch cho client {}: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>(
                    ServerEvent.ERROR_DISPATCH, ServerEvent.ERROR,
                    "Lỗi xử lý yêu cầu", null));
        }
        return rawAction;
    }

    /*
     * Gom routing trong một switch để ClientConnection không cần biết từng
     * handler nghiệp vụ. Nhờ vậy kiểm tra quyền luôn chạy trước mọi action.
     */
    private void route(ServerEvent action, String rawAction, String raw) {
        switch (action) {
            case LOGIN              -> authHandler.login(raw);
            case REGISTER           -> authHandler.register(raw);
            case LOGOUT             -> authHandler.logout();

            case LIST_AVAILABLE     -> auctionHandler.listAvailable();
            case JOIN               -> auctionHandler.join(raw);
            case BID                -> auctionHandler.bid(raw);
            case CANCEL_AUCTION     -> auctionHandler.cancelAuction(raw);
            case REGISTER_AUTO_BID  -> auctionHandler.registerAutoBid(raw);

            case POST_ITEM          -> itemHandler.postItem(raw);
            case EDIT_ITEM          -> itemHandler.editItem(raw);
            case GET_ALL_ITEMS      -> itemHandler.getAllItems();
            case APPROVE_ITEM       -> itemHandler.approveItem(raw);
            case REJECT_ITEM        -> itemHandler.rejectItem(raw);

            case GET_PROFILE        -> userHandler.getProfile();
            case UPDATE_PROFILE     -> userHandler.updateProfile(raw);
            case TRANSACTION        -> userHandler.transaction(raw);

            case GET_HISTORY        -> historyHandler.getHistory();
            case GET_MY_AUCTIONS    -> historyHandler.getMyAuctions();
            case GET_SESSION_BIDS   -> historyHandler.getSessionBids(raw);

            default -> context.sendResponse(new Response<>(ServerEvent.UNKNOWN,
                    ServerEvent.ERROR, "Lệnh không xác định: " + rawAction, null));
        }
    }
}
