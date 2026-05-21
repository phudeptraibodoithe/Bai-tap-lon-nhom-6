module com.tboat.server {
    requires java.sql;
    requires com.tboat.common;
    requires com.google.gson;
    requires org.slf4j;

    exports com.tboat;
    exports com.tboat.dao;
    exports com.tboat.database;
    exports com.tboat.service;
    exports com.tboat.socket;

    opens com.tboat.dao to java.sql;
    exports com.tboat.socket.handler;
}