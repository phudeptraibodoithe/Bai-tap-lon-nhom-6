package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.core.User;

public class SellerService {
    private final UserDAO userDAO = new UserDAO();
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
    private final HistoryDAO historyDAO = new HistoryDAO();
    private final BidDAO bidDAO=new BidDAO();

    public boolean cancelAuction(String accountName, int sessionId) {
        User user = userDAO.getUser(accountName);
        AuctionSession session = sessionDAO.getAuctionById(sessionId);

        if (user == null || session == null) return false;

        ParticipationContext context = new ParticipationContext(new SellerCancelRole());
        return context.executeAction(user, session, 0, userDAO, sessionDAO, historyDAO,bidDAO);
    }

    public boolean editAuction(String accountName, AuctionSession updatedSession) {
        User user = userDAO.getUser(accountName);
        if (user == null || updatedSession == null) return false;

        ParticipationContext context = new ParticipationContext(new SellerEditRole());

        return context.executeAction(user, updatedSession, 0, userDAO, sessionDAO,historyDAO, bidDAO);
    }
}