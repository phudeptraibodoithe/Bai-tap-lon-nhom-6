module com.tboat.common {
    requires com.google.gson;
    requires org.slf4j;

    opens com.tboat.models to com.google.gson;

    exports com.tboat.models;
    exports com.tboat.utils;
    exports com.tboat.logging;
}