package com.tboat.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    public static final String URL = "jdbc:mysql://localhost:3306/auction_db";
    public static final String USER = "root";
    public static final String PASSWORD = "123456789";

    public static Connection getConnection(){
        Connection connection;
        try {
            connection= DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            logger.error("MySQL connection error!", e);
            throw new RuntimeException(e);
        }
        return connection;
    }
}