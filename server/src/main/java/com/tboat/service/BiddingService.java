package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BiddingService {
    private static final Logger logger = LoggerFactory.getLogger(BiddingService.class);

    private final UserDAO            userDAO    = new UserDAO();
    private final AuctionSessionDAO  sessionDAO = new AuctionSessionDAO();
    private final HistoryDAO         historyDAO = new HistoryDAO();
    private final BidDAO             bidDAO     = new BidDAO();

    //Khởi tạo một lần, dùng mãi — BidderRole không có state
    private static final TransactionRole  BIDDER_ROLE = new BidderRole();
    private final ParticipationContext    bidContext  = new ParticipationContext(BIDDER_ROLE);

    public boolean placeBid(String bidderAccount, int sessionId, double newPrice) {
        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null) {
            logger.error("Lỗi: Không tìm thấy phiên đấu giá {}", sessionId);
            return false;
        }
        User user = userDAO.getUser(bidderAccount);
        if (user == null) {
            logger.error("Lỗi: Không tìm thấy user {}", bidderAccount);
            return false;
        }
        if (newPrice <= session.getCurrentPrice()) {
            return false;
        }

        //Dùng lại instance đã tạo sẵn
        return bidContext.executeAction(user, session, newPrice, userDAO, sessionDAO, historyDAO, bidDAO);
    }
}