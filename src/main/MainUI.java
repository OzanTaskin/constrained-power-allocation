package main;

import UI.controller.MainController;
import UI.view.ControlsView;
import UI.view.NetworkView;
import UI.view.TerminalView;
import factory.ReseauFactory;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import reseau.Reseau;

public class MainUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Application de Gestion de Réseau");

        Parameters params = getParameters();
        Reseau reseau;
        TerminalView terminalView = new TerminalView();

        if (!params.getRaw().isEmpty()) {
            String filePath = params.getRaw().get(0);
            try {
                reseau = ReseauFactory.parserReseau(10, filePath);
                terminalView.appendText("Réseau chargé depuis : " + filePath + "\n");
            } catch (Exception e) {
                reseau = new Reseau(10);
                terminalView.appendText("Erreur lors du chargement du fichier : " + e.getMessage() + "\n");
            }
        } else {
            reseau = new Reseau(10);
        }


        ObservableList<String> generateurNames = FXCollections.observableArrayList();
        ObservableList<String> maisonNames = FXCollections.observableArrayList();


        // Vues
        ControlsView controlsView = new ControlsView(maisonNames, generateurNames);
        NetworkView networkView = new NetworkView(reseau);

        // Contrôleur
        new MainController(reseau, terminalView, controlsView, networkView, primaryStage, generateurNames, maisonNames);

        // Mise en page
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(networkView);
        scrollPane.setFitToWidth(true);
        root.setCenter(scrollPane);
        root.setRight(controlsView);
        root.setBottom(terminalView);


        // Scène
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        networkView.update();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

