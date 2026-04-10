module com.example.giaodiendau {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.giaodiendau to javafx.fxml;
    exports com.example.giaodiendau;
}