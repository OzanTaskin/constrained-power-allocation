package UI.main;

import UI.controller.MainController;
import UI.view.ControlsView;
import UI.view.TerminalView;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import reseau.Reseau;

public class MainUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Application de Gestion de Réseau");

        // Model
        Reseau reseau = new Reseau("Mon Réseau", 10);
        ObservableList<String> generateurNames = FXCollections.observableArrayList();
        ObservableList<String> maisonNames = FXCollections.observableArrayList();

        // Views
        TerminalView terminalView = new TerminalView();
        ControlsView controlsView = new ControlsView(maisonNames, generateurNames);

        // Controller
        new MainController(reseau, terminalView, controlsView, primaryStage, generateurNames, maisonNames);

        // Layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setLeft(terminalView);
        root.setRight(controlsView);

        // Scene
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
