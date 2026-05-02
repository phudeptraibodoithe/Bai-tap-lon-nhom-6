package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.User;

public class SellerRole implements TransactionRole {

    public boolean execute(User user, AuctionSession session, double amount,
                           UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryBidDAO bidDAO) {
        System.out.println("Seller " + user.getAccountName() + " đang quản lý phiên.");
        return true;
    }
}