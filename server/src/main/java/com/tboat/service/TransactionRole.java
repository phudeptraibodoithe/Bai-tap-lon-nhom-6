package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.core.User;

public interface TransactionRole {
    /*
     * Mỗi role nhận các DAO cần dùng để caller giữ được một luồng transaction rõ ràng,
     * không tạo dependency ẩn bên trong role.
     */
    boolean execute(User user, AuctionSession session, double amount,
                    UserDAO userDAO, AuctionSessionDAO sessionDAO, HistoryDAO historyDAO, BidDAO bidDAO);
}
