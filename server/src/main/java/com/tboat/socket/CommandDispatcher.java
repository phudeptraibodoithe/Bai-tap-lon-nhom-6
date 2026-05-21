package com.tboat.socket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.network.Response;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class CommandDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private static final List<String> ADMIN_ACTIONS = Arrays.asList(
            ServerEvent.GET_ALL_ITEMS.name(), ServerEvent.APPROVE_ITEM.name(), ServerEvent.REJECT_ITEM.name()
    );

    private static final List<String> PUBLIC_ACTIONS = Arrays.asList(
            ServerEvent.LOGIN.name(), ServerEvent.REGISTER.name(), ServerEvent.LIST_AVAILABLE.name()
    );

    private final ClientContext context;

    private final AuthHandler    authHandler;
    private final AuctionHandler auctionHandler;
    private final ItemHandler    itemHandler;
    private final UserHandler    userHandler;
    private final HistoryHandler historyHandler;

    public CommandDispatcher(ClientContext context) {
        this.context        = context;
        this.authHandler    = new AuthHandler(context);
        this.auctionHandler = new AuctionHandler(context);
        this.itemHandler    = new ItemHandler(context);
        this.userHandler    = new UserHandler(context);
        this.historyHandler = new HistoryHandler(context);
    }

    public String dispatch(String rawInput) {
        String action = ServerEvent.UNKNOWN.name();
        try {
            JsonObject json = JsonParser.parseString(rawInput).getAsJsonObject();
            action = json.get("action").getAsString().toUpperCase();

            if (!PUBLIC_ACTIONS.contains(action) && context.isGuest()) {
                context.sendResponse(new Response<>(
                        ServerEvent.AUTH_ERROR.name(), ServerEvent.ERROR.name(),
                        "Vui lòng đăng nhập trước khi thực hiện thao tác này", null));
                return action;
            }

            if (ADMIN_ACTIONS.contains(action) && !context.getClientId().equals("admin")) {
                context.sendResponse(new Response<>(
                        ServerEvent.AUTH_ERROR.name(), ServerEvent.ERROR.name(),
                        "Chỉ Admin mới thực hiện được!", null));
                return action;
            }

            route(action, rawInput);

        } catch (Exception e) {
            log.error("Lỗi dispatch cho client {}: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>(
                    ServerEvent.ERROR_DISPATCH.name(), ServerEvent.ERROR.name(),
                    "Lỗi xử lý yêu cầu", null));
        }
        return action;
    }

    private void route(String action, String raw) {
        switch (action) {
            // ── Auth ──────────────────────────────────────────────────────────
            case "LOGIN"              -> authHandler.login(raw);
            case "REGISTER"           -> authHandler.register(raw);
            case "LOGOUT"             -> authHandler.logout();

            // ── Auction ───────────────────────────────────────────────────────
            case "LIST_AVAILABLE"     -> auctionHandler.listAvailable();
            case "JOIN"               -> auctionHandler.join(raw);
            case "BID"                -> auctionHandler.bid(raw);
            case "CANCEL_AUCTION"     -> auctionHandler.cancelAuction(raw);
            case "REGISTER_AUTO_BID"  -> auctionHandler.registerAutoBid(raw);

            // ── Item management ───────────────────────────────────────────────
            case "POST_ITEM"          -> itemHandler.postItem(raw);
            case "EDIT_ITEM"          -> itemHandler.editItem(raw);
            case "GET_ALL_ITEMS"      -> itemHandler.getAllItems();
            case "APPROVE_ITEM"       -> itemHandler.approveItem(raw);
            case "REJECT_ITEM"        -> itemHandler.rejectItem(raw);

            // ── User / Profile ────────────────────────────────────────────────
            case "GET_PROFILE"        -> userHandler.getProfile();
            case "UPDATE_PROFILE"     -> userHandler.updateProfile(raw);
            case "TRANSACTION"        -> userHandler.transaction(raw);

            // ── History ───────────────────────────────────────────────────────
            case "GET_HISTORY"        -> historyHandler.getHistory();
            case "GET_MY_AUCTIONS"    -> historyHandler.getMyAuctions();
            case "GET_SESSION_BIDS"   -> historyHandler.getSessionBids(raw);

            default -> context.sendResponse(new Response<>(ServerEvent.UNKNOWN.name(),
                    ServerEvent.ERROR.name(), "Lệnh không xác định: " + action, null));
        }
    }
}