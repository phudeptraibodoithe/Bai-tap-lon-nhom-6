package com.tboat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tboat.models.core.Person;
import com.tboat.models.core.User;

public class UserTest {
 
    private User user;
 
    @BeforeEach
    void setUp() {
        user = new User("user01", "ThanVAn", "secret",
                        200.0,"Regular buyer", "https://avatar.com/user01.png");
    }
 
    @Test
    void testGetDescription() {
        assertEquals("Regular buyer", user.getDescription());
    }
 
    @Test
    void testGetAvatarURL() {
        assertEquals("https://avatar.com/user01.png", user.getAvatarURL());
    }
 
    @Test
    void testSetDescription() {
        user.setDescription("Premium seller");
        assertEquals("Premium seller", user.getDescription());
    }
 
    @Test
    void testSetAvatarURL() {
        user.setAvatar("https://new-avatar.com/img.png");
        assertEquals("https://new-avatar.com/img.png", user.getAvatarURL());
    }
 
    @Test
    void testUserInheritsPersonFields() {
        assertEquals("user01", user.getAccountName());
        assertEquals(200.0, user.getBalance(), 0.001);
    }
 
    @Test
    void testUserIsInstanceOfPerson() {
        assertInstanceOf(Person.class, user);
    }
 
    @Test
    void testNullDescription() {
        user.setDescription(null);
        assertNull(user.getDescription());
    }
}