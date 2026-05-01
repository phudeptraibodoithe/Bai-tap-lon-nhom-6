package com.tboat.service;

import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;
import com.tboat.models.User;

public class BidderRole implements TransactionRole {

    public boolean execute(User user, AuctionSession session, double amount) {
        if (session.getStatusOfAuction() != StatusOfAuction.ONGOING) {
            System.out.println("Lỗi: Phiên đấu giá không đang diễn ra!");
            return false;
        } else {
            if (user.getBalance() < amount) {
                System.out.println("Lỗi: Số dư  không đủ!");
                return false;
            } else {
                if (amount < session.getCurrentPrice() + session.getBidIncrease()) {
                    System.out.println("Lỗi: Giá không hợp lệ!");
                    return false;
                }
                else {
                    session.setCurrentPrice(amount);
                    session.setHighestBidderAccount(user.getAccountName());
                    System.out.println("Đặt giá thành công!");
                    return true;
                }
            }
        }
    }
}