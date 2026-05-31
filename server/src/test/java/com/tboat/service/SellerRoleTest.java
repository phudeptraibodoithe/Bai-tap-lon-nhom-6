package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.BidDAO;
import com.tboat.dao.HistoryDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.core.User;
import com.tboat.models.item.OtherItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SellerRoleTest {

    private final UserDAO userDAO = new UserDAO();
    private final HistoryDAO historyDAO = new HistoryDAO();
    private final BidDAO bidDAO = new BidDAO();

    @Test
    @DisplayName("SellerEditRole: đúng chủ sở hữu, chưa bắt đầu, chưa có bidder -> cập nhật thành công")
    void sellerEditRoleSuccess() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();
        AuctionSession dbSession = session("seller", StatusOfAuction.PENDING, futureStart(), null);
        AuctionSession editedSession = session("seller", StatusOfAuction.PENDING, futureStart(), null);
        sessionDAO.sessionToReturn = dbSession;

        boolean result = new SellerEditRole().execute(
                user("seller"), editedSession, 0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertTrue(result);
        assertSame(editedSession, sessionDAO.updatedSession);
    }

    @Test
    @DisplayName("SellerEditRole: không tìm thấy phiên -> từ chối cập nhật")
    void sellerEditRoleRejectsMissingSession() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();
        sessionDAO.sessionToReturn = null;

        boolean result = new SellerEditRole().execute(
                user("seller"), session("seller", StatusOfAuction.PENDING, futureStart(), null),
                0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertFalse(result);
        assertNull(sessionDAO.updatedSession);
    }

    @Test
    @DisplayName("SellerEditRole: sai chủ sở hữu -> từ chối cập nhật")
    void sellerEditRoleRejectsWrongOwner() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();
        sessionDAO.sessionToReturn = session("otherSeller", StatusOfAuction.PENDING, futureStart(), null);

        boolean result = new SellerEditRole().execute(
                user("seller"), session("seller", StatusOfAuction.PENDING, futureStart(), null),
                0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertFalse(result);
        assertNull(sessionDAO.updatedSession);
    }

    @Test
    @DisplayName("SellerEditRole: phiên đã bắt đầu -> từ chối cập nhật")
    void sellerEditRoleRejectsStartedAuction() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();
        sessionDAO.sessionToReturn = session("seller", StatusOfAuction.ONGOING,
                LocalDateTime.now().minusMinutes(1), null);

        boolean result = new SellerEditRole().execute(
                user("seller"), session("seller", StatusOfAuction.PENDING, futureStart(), null),
                0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertFalse(result);
        assertNull(sessionDAO.updatedSession);
    }

    @Test
    @DisplayName("SellerEditRole: trạng thái không cho sửa -> từ chối cập nhật")
    void sellerEditRoleRejectsNonEditableStatus() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();
        sessionDAO.sessionToReturn = session("seller", StatusOfAuction.CANCELED, futureStart(), null);

        boolean result = new SellerEditRole().execute(
                user("seller"), session("seller", StatusOfAuction.PENDING, futureStart(), null),
                0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertFalse(result);
        assertNull(sessionDAO.updatedSession);
    }

    @Test
    @DisplayName("SellerEditRole: đã có bidder -> từ chối cập nhật")
    void sellerEditRoleRejectsSessionWithBidder() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();
        sessionDAO.sessionToReturn = session("seller", StatusOfAuction.PENDING, futureStart(), "bidder");

        boolean result = new SellerEditRole().execute(
                user("seller"), session("seller", StatusOfAuction.PENDING, futureStart(), null),
                0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertFalse(result);
        assertNull(sessionDAO.updatedSession);
    }

    @Test
    @DisplayName("SellerEditRole: DAO cập nhật thất bại -> trả false")
    void sellerEditRoleReturnsFalseWhenDaoUpdateFails() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();
        sessionDAO.sessionToReturn = session("seller", StatusOfAuction.NOT_STARTED, futureStart(), null);
        sessionDAO.updateResult = false;

        boolean result = new SellerEditRole().execute(
                user("seller"), session("seller", StatusOfAuction.PENDING, futureStart(), null),
                0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertFalse(result);
        assertNotNull(sessionDAO.updatedSession);
    }

    @Test
    @DisplayName("SellerCancelRole: đúng chủ sở hữu, chưa có bidder -> hủy thành công")
    void sellerCancelRoleSuccess() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();
        AuctionSession session = session("seller", StatusOfAuction.PENDING, futureStart(), null);

        boolean result = new SellerCancelRole().execute(
                user("seller"), session, 0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertTrue(result);
        assertEquals(session.getId(), sessionDAO.canceledSessionId);
    }

    @Test
    @DisplayName("SellerCancelRole: sai chủ sở hữu -> từ chối hủy")
    void sellerCancelRoleRejectsWrongOwner() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();

        boolean result = new SellerCancelRole().execute(
                user("seller"), session("otherSeller", StatusOfAuction.PENDING, futureStart(), null),
                0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertFalse(result);
        assertEquals(0, sessionDAO.canceledSessionId);
    }

    @Test
    @DisplayName("SellerCancelRole: đã có bidder -> từ chối hủy")
    void sellerCancelRoleRejectsSessionWithBidder() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();

        boolean result = new SellerCancelRole().execute(
                user("seller"), session("seller", StatusOfAuction.PENDING, futureStart(), "bidder"),
                0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertFalse(result);
        assertEquals(0, sessionDAO.canceledSessionId);
    }

    @Test
    @DisplayName("SellerCancelRole: DAO hủy thất bại -> trả false")
    void sellerCancelRoleReturnsFalseWhenDaoCancelFails() {
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO();
        sessionDAO.cancelResult = false;

        boolean result = new SellerCancelRole().execute(
                user("seller"), session("seller", StatusOfAuction.PENDING, futureStart(), null),
                0, userDAO, sessionDAO, historyDAO, bidDAO);

        assertFalse(result);
        assertNotEquals(0, sessionDAO.canceledSessionId);
    }

    private static LocalDateTime futureStart() {
        return LocalDateTime.now().plusHours(1);
    }

    private static User user(String accountName) {
        return new User(accountName, "password", accountName, 0, "", "");
    }

    private static AuctionSession session(String seller, StatusOfAuction status,
                                          LocalDateTime startTime, String highestBidder) {
        AuctionSession session = new AuctionSession(
                startTime,
                startTime.plusHours(1),
                100,
                10,
                new OtherItem(seller, "Sản phẩm test", "Mô tả", "")
        );
        session.setId(123);
        session.setStatusOfAuction(status);
        session.setHighestBidderAccount(highestBidder);
        return session;
    }

    private static class FakeAuctionSessionDAO extends AuctionSessionDAO {
        private AuctionSession sessionToReturn;
        private AuctionSession updatedSession;
        private boolean updateResult = true;
        private boolean cancelResult = true;
        private int canceledSessionId;

        @Override
        public AuctionSession getAuctionById(int sessionId) {
            return sessionToReturn;
        }

        @Override
        public boolean updateAuction(AuctionSession session) {
            updatedSession = session;
            return updateResult;
        }

        @Override
        public boolean cancelAuction(int sessionId) {
            canceledSessionId = sessionId;
            return cancelResult;
        }
    }
}
