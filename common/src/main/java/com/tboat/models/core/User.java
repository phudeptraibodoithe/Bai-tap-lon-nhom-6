package com.tboat.models.core;

public class User extends Person {
    // Attributes
    private String description;
    private String avatarURL;
    private String email;
    private String phone;

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
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    // Getters
    public String getDescription() {
        return description;
    }
    public String getAvatarURL() {
        return avatarURL;
    }
    public String getEmail() {
        return email;
    }
    public String getPhone() {
        return phone;
    }
}