package com.tboat.service;

import com.tboat.database.DatabaseConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
class DatabaseIntegrationTest {

    @Test
    @DisplayName("DB thật: kết nối được và schema user có các cột hồ sơ đang dùng")
    void realDatabaseHasExpectedUserProfileColumns() throws Exception {
        assumeTrue("true".equalsIgnoreCase(System.getenv("TBOAT_RUN_DB_TESTS")),
                "Bỏ qua DB integration test. Đặt TBOAT_RUN_DB_TESTS=true để chạy.");

        try (Connection connection = DatabaseConnection.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "user", null)) {

            boolean hasEmail = false;
            boolean hasPhone = false;
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                hasEmail |= "email".equalsIgnoreCase(columnName);
                hasPhone |= "phone".equalsIgnoreCase(columnName);
            }

            assertTrue(hasEmail, "Bảng user phải có cột email");
            assertTrue(hasPhone, "Bảng user phải có cột phone");
        }
    }
}
