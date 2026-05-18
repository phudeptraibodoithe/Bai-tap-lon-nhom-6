package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.core.User;

public class ParticipationContext {
    private TransactionRole roleBehavior;

    public ParticipationContext(TransactionRole roleBehavior) {
        this.roleBehavior = roleBehavior;
    }

    public boolean executeAction(User user, AuctionSession session, double amount, UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryDAO historyDAO, BidDAO bidDAO) {
        if (this.roleBehavior == null) return false;
        return this.roleBehavior.execute(user, session, amount, userDAO, sessionDAO, historyDAO,bidDAO);
    }
}