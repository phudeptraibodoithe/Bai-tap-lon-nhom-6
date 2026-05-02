module com.tboat.server {
    requires java.sql;
    requires com.tboat.common;
    requires com.google.gson;

    // Xuất các package để hệ thống tìm thấy class khi chạy
    exports com.tboat;
    exports com.tboat.dao;
    exports com.tboat.database;
    exports com.tboat.service;
    exports com.tboat.socket;

    // Cho phép thư viện SQL truy cập vào DAO để đổ dữ liệu
    opens com.tboat.dao to java.sql;
}