package com.tboat.service;

import com.tboat.dao.UserDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionRoomPaymentTest {

    @Test
    @DisplayName("AuctionRoom: chia tiền đúng 10% admin và 90% seller")
    void divideMoneyPaysAdminAndSeller() throws Exception {
        AuctionRoom room = new AuctionRoom(888, 100);
        FakeUserDAO userDAO = new FakeUserDAO(true);
        setField(room, "userDAO", userDAO);

        boolean result = invokeDivideMoney(room, "seller", 1_000_000);

        assertTrue(result);
        assertEquals(100_000, userDAO.amounts.get("admin"));
        assertEquals(900_000, userDAO.amounts.get("seller"));
    }

    @Test
    @DisplayName("AuctionRoom: chia tiền thất bại nếu DAO không cập nhật đủ hai bên")
    void divideMoneyReturnsFalseWhenDaoFails() throws Exception {
        AuctionRoom room = new AuctionRoom(888, 100);
        FakeUserDAO userDAO = new FakeUserDAO(false);
        setField(room, "userDAO", userDAO);

        boolean result = invokeDivideMoney(room, "seller", 1_000_000);

        assertFalse(result);
        assertEquals(100_000, userDAO.amounts.get("admin"));
        assertFalse(userDAO.amounts.containsKey("seller"),
                "Do toán tử && ngắn mạch, seller không được cập nhật khi admin update fail");
    }

    private static boolean invokeDivideMoney(AuctionRoom room, String seller, double totalAmount)
            throws Exception {
        Method method = AuctionRoom.class.getDeclaredMethod(
                "divideMoney", Connection.class, String.class, double.class);
        method.setAccessible(true);
        return (boolean) method.invoke(room, null, seller, totalAmount);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeUserDAO extends UserDAO {
        private final boolean result;
        private final Map<String, Double> amounts = new LinkedHashMap<>();

        FakeUserDAO(boolean result) {
            this.result = result;
        }

        @Override
        public boolean updateBalance(Connection conn, String accountName, double amount)
                throws SQLException {
            amounts.put(accountName, amount);
            return result;
        }
    }
}
