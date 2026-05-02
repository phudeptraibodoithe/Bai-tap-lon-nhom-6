package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.ParticipationDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.User;

public class BiddingService {

    private UserDAO userDAO;
    private AuctionSessionDAO sessionDAO;
    private HistoryBidDAO bidDAO;
    private ParticipationDAO participationDAO;

    public BiddingService() {
        this.userDAO = new UserDAO();
        this.sessionDAO = new AuctionSessionDAO();
        this.bidDAO = new HistoryBidDAO();
        this.participationDAO = new ParticipationDAO();
    }

    public boolean placeBid(String bidderAccount, int sessionId, double newPrice) {
        AuctionSession session = sessionDAO.getAuctionById(sessionId);
        if (session == null) {
            System.err.println("Lỗi: Không tìm thấy phiên đấu giá " + sessionId);
            return false;
        }
        User user = userDAO.getUser(bidderAccount);
        if (user == null) {
            System.err.println("Lỗi: Không tìm thấy user " + bidderAccount);
            return false;
        }
        double previousPrice = session.getCurrentPrice();
        if (newPrice <= previousPrice) {
            return false;
        }
        ParticipationContext context = new ParticipationContext(new BidderRole());
        return context.executeAction(user, session, newPrice, userDAO, sessionDAO, bidDAO);
    }
}