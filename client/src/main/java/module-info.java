module com.tboat.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.tboat.common;
    requires java.sql;
    requires com.google.gson;
    requires org.slf4j;
    requires java.prefs;

    opens com.tboat.controllers to javafx.fxml;
    opens com.tboat to javafx.fxml;
    opens com.tboat.ucb to com.google.gson;

    exports com.tboat;
    exports com.tboat.ucb;
    exports com.tboat.controllers;
    exports com.tboat.controllers.helper;
    opens com.tboat.controllers.helper to javafx.fxml;
}