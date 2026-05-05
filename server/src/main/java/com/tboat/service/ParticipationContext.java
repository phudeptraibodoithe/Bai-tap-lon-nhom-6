package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.User;

public class ParticipationContext {
    private TransactionRole roleBehavior;

    public ParticipationContext(TransactionRole roleBehavior) {
        this.roleBehavior = roleBehavior;
    }

    public void setRoleBehavior(TransactionRole roleBehavior) {
        this.roleBehavior = roleBehavior;
    }

    public boolean executeAction(User user, AuctionSession session, double amount, UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryBidDAO bidDAO) {
        if (this.roleBehavior == null) return false;
        return this.roleBehavior.execute(user, session, amount, userDAO, sessionDAO, bidDAO);
    }
}