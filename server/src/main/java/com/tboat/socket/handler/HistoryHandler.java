package com.tboat.socket.handler;

import com.google.gson.JsonParser;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.models.*;
import com.tboat.socket.ClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class HistoryHandler {

    private static final Logger log = LoggerFactory.getLogger(HistoryHandler.class);

    private final ClientContext    context;
    private final HistoryDAO       historyDAO = new HistoryDAO();
    private final AuctionSessionDAO auctionDAO = new AuctionSessionDAO();
    private final BidDAO           bidDAO     = new BidDAO();

    public HistoryHandler(ClientContext context) { this.context = context; }

    public void getHistory() {
        try {
            var list = historyDAO.getHistoryByAccount(context.getClientId());
            context.sendResponse(new Response<>("SUCCESS", "Lấy lịch sử thành công",
                    list != null ? list : new ArrayList<>()));
        } catch (Exception e) {
            log.error("Lỗi GET_HISTORY [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("ERROR", "Lỗi lấy lịch sử: " + e.getMessage(), null));
        }
    }

    public void getMyAuctions() {
        try {
            var list = auctionDAO.getAuctionsBySeller(context.getClientId());
            context.sendResponse(new Response<>("SUCCESS", "Lấy danh sách sản phẩm thành công",
                    list != null ? list : new ArrayList<>()));
        } catch (Exception e) {
            log.error("Lỗi GET_MY_AUCTIONS [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("ERROR", "Lỗi lấy danh sách: " + e.getMessage(), null));
        }
    }

    public void getSessionBids(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();
            var list = bidDAO.getBidsBySession(sessionId);
            context.sendResponse(new Response<>("SUCCESS", "Lấy danh sách Bid thành công",
                    list != null ? list : new ArrayList<>()));
        } catch (Exception e) {
            log.error("Lỗi GET_SESSION_BIDS [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("ERROR", "Lỗi lấy danh sách Bid: " + e.getMessage(), null));
        }
    }
}
