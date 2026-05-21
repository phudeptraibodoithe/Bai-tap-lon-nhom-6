package com.tboat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tboat.models.core.Person;

public class PersonTest {
 
    // Concrete subclass for testing abstract Person
    static class ConcretePerson extends Person {
        public ConcretePerson(String accountName, String nickname,
                              String password, double balance) {
            this.accountName = accountName;
            this.nickname = nickname;
            this.password = password;
            this.balance = balance;
        }
    }
 
    private ConcretePerson person;
 
    @BeforeEach
    void setUp() {
        person = new ConcretePerson("user01", "Nguyen Van A", "pass123",500.0);
    }
 
    @Test
    void testGetAccountName() {
        assertEquals("user01", person.getAccountName());
    }
 
    @Test
    void testGetNickname() {
        assertEquals("Nguyen Van A", person.getNickname());
    }
 
    @Test
    void testGetPassword() {
        assertEquals("pass123", person.getPassword());
    }
 
    @Test
    void testGetBalance() {
        assertEquals(500.0, person.getBalance(), 0.001);
    }
 
    @Test
    void testSetBalance() {
        person.setBalance(1000.0);
        assertEquals(1000.0, person.getBalance(), 0.001);
    }
 
    @Test
    void testNegativeBalanceIsAllowed() {
        person.setBalance(-100.0);
        assertEquals(-100.0, person.getBalance(), 0.001);
    }
}