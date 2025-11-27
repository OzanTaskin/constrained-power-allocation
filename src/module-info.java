module UI {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    exports UI.test;
    exports UI.main;
    exports UI.controller;
    exports UI.view;
    opens UI.test to javafx.fxml;
    opens UI.main to javafx.fxml;
    opens UI.controller to javafx.fxml;
    opens UI.view to javafx.fxml;

}