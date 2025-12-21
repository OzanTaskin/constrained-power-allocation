module ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    exports ui.controller;
    exports ui.view;
    opens ui.controller to javafx.fxml;
    opens ui.view to javafx.fxml;
    exports main;
    opens main to javafx.fxml;
    exports menu;
    opens menu to javafx.fxml;

}