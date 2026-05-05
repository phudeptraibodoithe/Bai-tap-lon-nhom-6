package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.User;

public class SellerService {
    private final UserDAO userDAO = new UserDAO();
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
    private final HistoryBidDAO bidDAO = new HistoryBidDAO();

    public boolean cancelAuction(String accountName, int sessionId) {
        User user = userDAO.getUser(accountName);
        AuctionSession session = sessionDAO.getAuctionById(sessionId);

        if (user == null || session == null) return false;

        ParticipationContext context = new ParticipationContext(new SellerCancelRole());
        return context.executeAction(user, session, 0, userDAO, sessionDAO, bidDAO);
    }
}