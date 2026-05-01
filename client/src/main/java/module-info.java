module com.tboat.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.tboat.common;
    requires java.sql;
    opens com.tboat.controllers to javafx.fxml;
    opens com.tboat to javafx.fxml;
    exports com.tboat;
}