package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.BidResult;
import com.tboat.models.core.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BiddingService {
    private static final Logger logger = LoggerFactory.getLogger(BiddingService.class);

    private final UserDAO           userDAO    = new UserDAO();
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
    private final HistoryDAO        historyDAO = new HistoryDAO();
    private final BidDAO            bidDAO     = new BidDAO();

    private static final TransactionRole  BIDDER_ROLE = new BidderRole();
    private final ParticipationContext    bidContext  = new ParticipationContext(BIDDER_ROLE);

    public BidResult placeBid(String bidderAccount, int sessionId, double newPrice) {
        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null) return BidResult.SESSION_NOT_FOUND;

        User user = userDAO.getUser(bidderAccount);
        if (user == null) return BidResult.USER_NOT_FOUND;

        if (newPrice <= session.getCurrentPrice()) return BidResult.PRICE_TOO_LOW;

        // Kiểm tra số dư trước khi thử transaction
        if (user.getBalance() < newPrice) return BidResult.INSUFFICIENT_BALANCE;

        boolean ok = bidContext.executeAction(user, session, newPrice, userDAO, sessionDAO, historyDAO, bidDAO);
        return ok ? BidResult.OK : BidResult.DB_ERROR;
    }
}