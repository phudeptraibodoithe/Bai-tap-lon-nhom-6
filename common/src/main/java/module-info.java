module com.tboat.common {
    requires com.google.gson;
    requires org.slf4j;

    exports com.tboat.utils;
    exports com.tboat.logging;
    opens com.tboat.models.core to com.google.gson;
    exports com.tboat.models.core;
    exports com.tboat.models.auction;
    opens com.tboat.models.auction to com.google.gson;
    exports com.tboat.models.item;
    opens com.tboat.models.item to com.google.gson;
    exports com.tboat.models.item.factory;
    opens com.tboat.models.item.factory to com.google.gson;
    exports com.tboat.models.network;
    opens com.tboat.models.network to com.google.gson;
}