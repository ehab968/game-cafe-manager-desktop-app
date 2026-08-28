module com.gamecafe.gamecafemanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.gamecafe.gamecafemanager.presentation.controller to javafx.fxml;
    exports com.gamecafe.gamecafemanager.presentation;
}
