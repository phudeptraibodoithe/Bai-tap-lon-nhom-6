module com.tboat.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.tboat.common;
    requires java.sql;
    requires com.google.gson;
    requires org.slf4j;

    opens com.tboat.controllers to javafx.fxml;
    opens com.tboat to javafx.fxml;

    exports com.tboat;
}