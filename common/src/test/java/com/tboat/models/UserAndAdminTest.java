package com.tboat.models;

import com.tboat.models.core.Admin;
import com.tboat.models.core.Person;
import com.tboat.models.core.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserAndAdminTest {

    @Test
    void testUserCreation() {
        User user = new User("nguyenvana", "pass123", "Teo", 500.0, "Mo ta", "avatar.png");

        assertEquals("nguyenvana", user.getAccountName());
        assertEquals(500.0, user.getBalance());
        assertEquals("Mo ta", user.getDescription());
        assertEquals("avatar.png", user.getAvatarURL());
    }

    @Test
    void testAdminInheritance() {
        Admin admin = new Admin("admin1", "root", "Boss", 1000.0);

        // Kiểm tra tính kế thừa từ Person
        assertTrue(admin instanceof Person);
        assertEquals("Boss", admin.getNickname());
        assertEquals(1000.0, admin.getBalance());
    }
}