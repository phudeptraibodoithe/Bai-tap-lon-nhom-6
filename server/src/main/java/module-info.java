module com.tboat.server {
    requires java.sql;
    requires com.tboat.common; // Dòng này cực kỳ quan trọng để Server thấy User.java
    
    // Mở các package để các thư viện khác có thể truy cập nếu cần
    opens com.tboat.server.dao to com.tboat.common;
}