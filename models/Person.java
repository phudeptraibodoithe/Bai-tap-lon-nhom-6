package models;

public abstract class Person {
    // Attributes
    protected String accountName;
    protected String password;
    protected String nickname;
    protected double balance;

    // Constructor
    public Person(String accountName, String password, String nickname) {
        this.accountName = accountName;
        this.password = password;
        this.nickname = nickname;
        this.balance = 0.0;
    }

    // Setters
    public void setNickname(String nickname) { this.nickname = nickname; }

    // Getters
    public String getAccountName() { return accountName; }
    public String getNickname() { return nickname; }
    public double getBalance() { return balance; }
    public String getPassword() { return password; }

    // Methods
    public void withdraw(double amount) {
        if (amount > 0 && this.balance >= amount) {
            this.balance -= amount;
        }
        else { 
            System.out.println("Invalid!");
        }
    }
}