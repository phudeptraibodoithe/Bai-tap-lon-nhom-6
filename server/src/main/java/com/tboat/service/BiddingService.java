package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.ParticipationDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.Participation;
import com.tboat.models.User;

public class BiddingService {

    private UserDAO userDAO;
    private AuctionSessionDAO sessionDAO;
    private HistoryBidDAO bidDAO;
    private ParticipationDAO participationDAO;

    // Khởi tạo các DAO
    public BiddingService() {
        this.userDAO = new UserDAO();
        this.sessionDAO = new AuctionSessionDAO();
        this.bidDAO = new HistoryBidDAO();
        this.participationDAO=new ParticipationDAO();
    }

    public boolean placeBid(String accountName, int sessionId, double bidAmount) {
        // 1. Lấy dữ liệu (Model)
        User user = userDAO.getUser(accountName);
        AuctionSession session = sessionDAO.getAuctionById(sessionId);

        Participation partRecord = participationDAO.getRoleType(accountName, sessionId);

        if (partRecord == null) {
            System.out.println("Lỗi: Người dùng chưa tham gia phiên đấu giá này!");
            return false;
        }
        // 3. Khởi tạo Context điều khiển hành vi
        ParticipationContext context;

        // 4. Quyết định Strategy dựa trên vai trò trong DB
        if (partRecord.getRoleType().equals("BIDDER")) {
            context = new ParticipationContext(new BidderRole());
        } else if (partRecord.getRoleType().equals("SELLER")) {
            context = new ParticipationContext(new SellerRole());
        } else {
            return false; // Không có quyền
        }

        // 5. Thực thi qua Context (Người điều khiển)
        return context.executeAction(user, session, bidAmount, userDAO, sessionDAO, bidDAO);
    }
}