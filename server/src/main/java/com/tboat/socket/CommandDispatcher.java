package com.tboat.socket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.Response;
import com.tboat.socket.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * CommandDispatcher — Nhận JSON thô, xác định action, gọi đúng Handler.
 * Khi thêm action mới: chỉ cần thêm 1 case ở đây + viết Handler tương ứng.
 * Không cần động vào ClientHandler hay các Handler khác.
 */
public class CommandDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private static final List<String> PUBLIC_ACTIONS =
        Arrays.asList("LOGIN", "REGISTER", "LIST_AVAILABLE", "GET_PENDING_ITEMS",
                       "APPROVE_ITEM", "REJECT_ITEM");

    private final ClientContext context;

    // Các handler — khởi tạo một lần, tái dùng cho mọi request
    private final AuthHandler authHandler;
    private final AuctionHandler auctionHandler;
    private final ItemHandler itemHandler;
    private final UserHandler userHandler;
    private final HistoryHandler historyHandler;

    public CommandDispatcher(ClientContext context) {
        this.context = context;
        this.authHandler    = new AuthHandler(context);
        this.auctionHandler = new AuctionHandler(context);
        this.itemHandler    = new ItemHandler(context);
        this.userHandler    = new UserHandler(context);
        this.historyHandler = new HistoryHandler(context);
    }

    /**
     * Nhận JSON thô từ socket, routing tới handler phù hợp.
     * @return action đã xử lý (dùng để log)
     */
    public String dispatch(String rawInput) {
        String action = "UNKNOWN";
        try {
            JsonObject json = JsonParser.parseString(rawInput).getAsJsonObject();
            action = json.get("action").getAsString().toUpperCase();

            // Kiểm tra auth trước khi xử lý
            if (!PUBLIC_ACTIONS.contains(action) && context.isGuest()) {
                context.sendResponse(new Response<>("AUTH_ERROR", "ERROR", "Vui lòng đăng nhập", null));
                return action;
            }

            route(action, rawInput);

        } catch (Exception e) {
            log.error("Lỗi dispatch cho client {}: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("ERROR_DISPATCH", "ERROR", "Lỗi xử lý yêu cầu", null));
        }
        return action;
    }

    private void route(String action, String raw) {
        switch (action) {
            // Auth
            case "LOGIN"    -> authHandler.login(raw);
            case "REGISTER" -> authHandler.register(raw);
            case "LOGOUT"   -> authHandler.logout();

            // Auction
            case "LIST_AVAILABLE"  -> auctionHandler.listAvailable();
            case "JOIN"            -> auctionHandler.join(raw);
            case "BID"             -> auctionHandler.bid(raw);
            case "CANCEL_AUCTION"  -> auctionHandler.cancelAuction(raw);

            // Item management
            case "POST_ITEM"       -> itemHandler.postItem(raw);
            case "EDIT_ITEM"       -> itemHandler.editItem(raw);
            case "GET_PENDING_ITEMS" -> itemHandler.getPendingItems();
            case "APPROVE_ITEM"    -> itemHandler.approveItem(raw);
            case "REJECT_ITEM"     -> itemHandler.rejectItem(raw);

            // User / Profile
            case "PROFILE"         -> userHandler.getProfile();
            case "UPDATE_PROFILE"  -> userHandler.updateProfile(raw);
            case "TRANSACTION"     -> userHandler.transaction(raw);

            // History
            case "GET_HISTORY"     -> historyHandler.getHistory();
            case "GET_MY_AUCTIONS" -> historyHandler.getMyAuctions();
            case "GET_SESSION_BIDS"-> historyHandler.getSessionBids(raw);

            default -> context.sendResponse(new Response<>("UNKNOWN", "ERROR", "Lệnh không xác định: " + action, null));

        }
    }
}
