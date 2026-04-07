package models;

public abstract class User{
    // Attributes
    private String account;
    private String password;
    private String username;
    private double balance;
    private String email;
    private String id;
    
    // Constructor
    public User(String account, String email, String password, String username){
        this.account = account;
        this.email = email;
        this.password = password;
        this.username = username;       
    }

    // Getters for attributes
    public String getUsername{ return username; }
    public String getEmail{ return email; }
    public double getBalance{ return balance; }

    
}