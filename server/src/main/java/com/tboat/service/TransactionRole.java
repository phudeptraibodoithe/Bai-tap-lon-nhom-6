package com.tboat.service;

import com.tboat.models.AuctionSession;
import com.tboat.models.User;

public interface TransactionRole {
    boolean execute(User user,AuctionSession session, double amount);
}