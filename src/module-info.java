module UI {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    exports UI.controller;
    exports UI.view;
    opens UI.controller to javafx.fxml;
    opens UI.view to javafx.fxml;
    exports main;
    opens main to javafx.fxml;
    exports menu;
    opens menu to javafx.fxml;

}