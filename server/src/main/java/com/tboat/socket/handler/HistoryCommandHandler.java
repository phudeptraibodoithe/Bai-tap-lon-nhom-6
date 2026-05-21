package com.tboat.socket.handler;

import com.google.gson.JsonParser;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.models.network.Response;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class HistoryCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(HistoryCommandHandler.class);

    private final ClientSession     context;
    private final HistoryDAO        historyDAO = new HistoryDAO();
    private final AuctionSessionDAO auctionDAO = new AuctionSessionDAO();
    private final BidDAO            bidDAO     = new BidDAO();

    public HistoryCommandHandler(ClientSession context) { this.context = context; }

    /*
     * Các lệnh lịch sử chỉ đọc dữ liệu. Khi DAO không có dòng nào, trả về list rỗng
     * để client vẫn render bảng trống bình thường.
     */
    public void getHistory() {
        try {
            var historyRows = historyDAO.getHistoryByAccount(context.getClientId());
            context.sendResponse(new Response<>(ServerEvent.GET_HISTORY.name(), ServerEvent.SUCCESS.name(),
                    "Lấy lịch sử thành công", historyRows != null ? historyRows : new ArrayList<>()));
        } catch (Exception error) {
            log.error("Lỗi GET_HISTORY [{}]: {}", context.getClientId(), error.getMessage(), error);
            context.sendResponse(new Response<>(ServerEvent.GET_HISTORY.name(), ServerEvent.ERROR.name(),
                    "Lỗi lấy lịch sử: " + error.getMessage(), null));
        }
    }

    public void getMyAuctions() {
        try {
            var auctions = auctionDAO.getAuctionsBySeller(context.getClientId());
            context.sendResponse(new Response<>(ServerEvent.GET_MY_AUCTIONS.name(), ServerEvent.SUCCESS.name(),
                    "Lấy danh sách sản phẩm thành công", auctions != null ? auctions : new ArrayList<>()));
        } catch (Exception error) {
            log.error("Lỗi GET_MY_AUCTIONS [{}]: {}", context.getClientId(), error.getMessage(), error);
            context.sendResponse(new Response<>(ServerEvent.GET_MY_AUCTIONS.name(), ServerEvent.ERROR.name(),
                    "Lỗi lấy danh sách: " + error.getMessage(), null));
        }
    }

    public void getSessionBids(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();
            var bids = bidDAO.getBidsBySession(sessionId);
            context.sendResponse(new Response<>(ServerEvent.GET_SESSION_BIDS.name(), ServerEvent.SUCCESS.name(),
                    "Lấy danh sách Bid thành công", bids != null ? bids : new ArrayList<>()));
        } catch (Exception error) {
            log.error("Lỗi GET_SESSION_BIDS [{}]: {}", context.getClientId(), error.getMessage(), error);
            context.sendResponse(new Response<>(ServerEvent.GET_SESSION_BIDS.name(), ServerEvent.ERROR.name(),
                    "Lỗi lấy danh sách Bid: " + error.getMessage(), null));
        }
    }
}
