package com.tboat.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class User extends Person {
    private static final Logger logger = LoggerFactory.getLogger(User.class);

    // Attributes
    private String description;
    private String avatarURL;

    // Constructor
    public User(String accountName, String password, String nickname, double balance, String description, String avatarURL) {
        super(accountName, password, nickname, balance);
        this.avatarURL = avatarURL;
        this.description = description;
    }
    public User() {}

    // Setters
    public void setDescription(String description) {
        this.description = description;
    }
    public void setAvatar(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    // Getters
    public String getDescription() {
        return description;
    }
    public String getAvatarURL() {
        return avatarURL;
    }

    // Methods
    public void deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
        } else {
            logger.warn("Deposit failed: invalid amount {}", amount);
        }
    }
}