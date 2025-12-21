package main;

import ui.controller.MainController;
import ui.view.ControlsView;
import ui.view.NetworkView;
import ui.view.TerminalView;
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
        double penalite = 10.0; // Pénalité par défaut 10.0
        boolean fileProvided = false;

        if (!params.getRaw().isEmpty()) {
            String firstArg = params.getRaw().get(0);

            // On cherche à savoir si seulement la pénalité est passé en argument
            boolean firstArgIspenalite = false;
            try {
                if (firstArg.matches("-?\\d+(\\.\\d+)?")) {
                    penalite = Double.parseDouble(firstArg);
                    firstArgIspenalite = true;
                }
            } catch (NumberFormatException e) {
                // C'est probablement un fichier
            }


            // Handle cases: file_path, penalite, file_path penalite, or just penalite (if not file)
            if (!firstArgIspenalite || params.getRaw().size() > 1) { // It's a file path or file_path + penalite
                String filePath = firstArgIspenalite ? (params.getRaw().size() > 1 ? params.getRaw().get(1) : null) : firstArg;
                if (filePath != null) {
                    fileProvided = true;
                    if (params.getRaw().size() > 1 && !firstArgIspenalite) { // file_path penalite
                        try {
                            penalite = Double.parseDouble(params.getRaw().get(1));
                        } catch (NumberFormatException e) {
                            terminalView.appendText("Pénalite invalide, pénalité par défaut utilisée (10.0)\n");
                        }
                    } else if (params.getRaw().size() > 2) { // file_path penalite (extra args) - take the second as penalite
                        try {
                            penalite = Double.parseDouble(params.getRaw().get(2));
                        } catch (NumberFormatException e) {
                            terminalView.appendText("Pénalite invalide, pénalité par défaut utilisée (10.0)\n");
                        }
                    }

                    try {
                        reseau = ReseauFactory.parserReseau(penalite, filePath);
                        terminalView.appendText("Réseau chargé depuis : " + filePath + " avec pénalité " + penalite + "\n");
                    } catch (Exception e) {
                        reseau = new Reseau(penalite);
                        terminalView.appendText("Erreur lors du chargement du fichier '" + filePath + "': " + e.getMessage() + "\n");
                        terminalView.appendText("Création d'un réseau vide avec pénalité " + penalite + ".\n");
                    }
                } else { // No explicit file path, only penalite
                    reseau = new Reseau(penalite);
                    terminalView.appendText("Création d'un réseau vide avec pénalité " + penalite + ".\n");
                }
            } else { // Only a penalite argument was provided
                reseau = new Reseau(penalite);
                terminalView.appendText("Création d'un réseau vide avec pénalité " + penalite + ".\n");
            }

        } else { // No arguments
            reseau = new Reseau(penalite); // Use default penalite
            terminalView.appendText("Création d'un réseau vide avec pénalité " + penalite + ".\n");
        }


        ObservableList<String> generateurNames = FXCollections.observableArrayList();
        ObservableList<String> maisonNames = FXCollections.observableArrayList();


        // Vues
        ControlsView controlsView = new ControlsView(maisonNames, generateurNames, fileProvided);
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

