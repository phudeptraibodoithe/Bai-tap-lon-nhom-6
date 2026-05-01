package com.tboat.database;

import java.sql.*;

public class DatabaseConnection {
    public static final String URL = "jdbc:mysql://localhost:3306/auction_db";
    public static final String USER = "root";
    public static final String PASSWORD = "123456789";

    public static Connection getConnection(){
        Connection connection=null;
        try {
             connection= DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("MySQL connection error!");
            throw new RuntimeException(e);
        }
        return connection;
    }

}