package com.tboat.service;

import com.tboat.models.AuctionSession;
import com.tboat.models.User;

public class SellerRole implements TransactionRole {

    public boolean execute(User user, AuctionSession session, double amount) {
        System.out.println("Seller " + user.getAccountName() + " đang quản lý phiên.");
        return true;
    }
}