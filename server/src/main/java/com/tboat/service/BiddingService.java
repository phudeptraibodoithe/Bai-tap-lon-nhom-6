package com.tboat.service;

import com.tboat.dao.*;
import com.tboat.models.AuctionSession;
import com.tboat.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BiddingService {
    private static final Logger logger = LoggerFactory.getLogger(BiddingService.class);

    private final UserDAO userDAO=new UserDAO();
    private final AuctionSessionDAO sessionDAO=new AuctionSessionDAO();
    private final HistoryDAO historyDAO=new HistoryDAO();
    private final BidDAO bidDAO=new BidDAO();

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
        double previousPrice = session.getCurrentPrice();
        if (newPrice <= previousPrice) {
            return false;
        }
        ParticipationContext context = new ParticipationContext(new BidderRole());
        return context.executeAction(user, session, newPrice, userDAO, sessionDAO,historyDAO, bidDAO);
    }
}