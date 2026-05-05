module com.tboat.common {
    // 1. Yêu cầu module Gson (để có thể sử dụng thư viện Gson)
    requires com.google.gson;

    // 2. Yêu cầu SLF4J API (để dùng Logger trong các class models/utils)
    requires org.slf4j;

    // 3. MỞ KHÓA package models cho Gson để nó có thể đọc/ghi các biến private
    opens com.tboat.models to com.google.gson;

    // 4. Nơi bạn EXPORTS các package của mình để các module khác (như server, client) sử dụng
    exports com.tboat.models;
    exports com.tboat.utils;
}