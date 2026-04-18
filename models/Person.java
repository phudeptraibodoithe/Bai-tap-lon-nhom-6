package models;

public abstract class Person {
    // Attributes
    private String account;
    private String password;
    private String username;

    // Constructor
    public Person(String account, String password, String username) {
        this.account = account;
        this.password = password;
        this.username = username;
    }

    // Methods
    public String getUsername() {
        return username;
    }

    // Setter
    public void setUsername(String username) {
        this.username = username;
    }
}