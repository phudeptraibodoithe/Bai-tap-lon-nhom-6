package com.tboat.models.network;

public enum ServerEvent {
        // ── gửi thông báo ──────
        NOTIFICATION,
        // ── Auction room broadcasts (gửi tới tất cả subscriber trong phòng) ──────
        NEW_BID,
        AUCTION_STARTED,
        AUCTION_FINISHED,
        AUCTION_CANCELED,
        TIME_EXTENDED,
        TIME_UPDATED,       // admin/seller edit lại thời gian khi đang ONGOING
        AUTO_BID_OUT,       // gửi riêng tới client bị out khỏi auto-bid

        // ── Global broadcasts (gửi tới tất cả hoặc admin) ────────────────────────
        RELOAD_AVAILABLE,       // trang chủ cần load lại danh sách phiên
        RELOAD_ALL_ITEMS,       // admin cần load lại toàn bộ phiên
        RELOAD_PENDING_ITEMS,   // admin cần load lại danh sách chờ duyệt

        // ── Response status (trường "status" trong Response) ─────────────────────
        SUCCESS,
        FAILED,
        ERROR,

        // ── Response status đặc biệt ─────────────────────────────────────────────
        JOIN_SUCCESS,
        AUTO_BID_REGISTERED,

        // ── Response type khớp với action gửi lên ────────────────────────────────
        LOGIN,
        REGISTER,
        LOGOUT,
        LIST_AVAILABLE,
        JOIN,
        BID,
        REGISTER_AUTO_BID,
        CANCEL_AUCTION,
        POST_ITEM,
        EDIT_ITEM,
        GET_ALL_ITEMS,
        APPROVE_ITEM,
        REJECT_ITEM,
        GET_PROFILE,
        UPDATE_PROFILE,
        TRANSACTION,
        GET_HISTORY,
        GET_MY_AUCTIONS,
        GET_SESSION_BIDS,

        // ── Misc ─────────────────────────────────────────────────────────────────
        SYSTEM,
        SERVER_READY,
        AUTH_ERROR,
        ERROR_DISPATCH,
        NOTIFY,
        UNKNOWN
}
