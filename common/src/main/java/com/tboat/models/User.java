package com.tboat.models;

public class User extends Person {
    // Attributes
    private String description;
    private String avatarURL;

    // Constructor
    public User(String accountName, String password, String nickname,double balance,String description,String avatarURL) {
        super(accountName, password, nickname,balance);
        this.avatarURL=avatarURL;
        this.description=description;
    }
    public User(){    }

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
        }
        else {
            System.out.println("Invalid!");
        }
    }
}