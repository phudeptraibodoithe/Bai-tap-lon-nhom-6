package com.tboat.service;

import com.tboat.models.AuctionSession;
import com.tboat.models.User;

public class Participation {
    private String userAccountName;
    private int sessionId;
    private TransactionRole roleBehavior;

    public Participation(String userAccountName, int sessionId, TransactionRole roleBehavior) {
        this.userAccountName = userAccountName;
        this.sessionId = sessionId;
        this.roleBehavior = roleBehavior;
    }

    public void executeAction(User user, AuctionSession session, double amount) {
    }
}