package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.core.User;

public interface TransactionRole {
    // Thêm các DAO cần thiết vào tham số
    boolean execute(User user, AuctionSession session, double amount,
                    UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryDAO historyDAO, BidDAO bidDAO);
}