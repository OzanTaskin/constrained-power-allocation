package UI.controller;

import UI.view.ControlsView;
import UI.view.TerminalView;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import reseau.Consommation;
import reseau.Generateur;
import reseau.Maison;
import reseau.Reseau;

import java.io.File;
import java.io.IOException;

public class MainController {

    private Reseau reseau;
    private TerminalView terminal;
    private ControlsView controls;
    private Stage stage;
    private ObservableList<String> generateurNames;
    private ObservableList<String> maisonNames;


    public MainController(Reseau reseau, TerminalView terminal, ControlsView controls, Stage stage, ObservableList<String> generateurNames, ObservableList<String> maisonNames) {
        this.reseau = reseau;
        this.terminal = terminal;
        this.controls = controls;
        this.stage = stage;
        this.generateurNames = generateurNames;
        this.maisonNames = maisonNames;

        attachEventHandlers();
    }

    private void attachEventHandlers() {
        controls.getAddGenerateurButton().setOnAction(e -> addGenerateur());
        controls.getAddMaisonButton().setOnAction(e -> addMaison());
        controls.getAddConnectionButton().setOnAction(e -> addConnection());
        controls.getSaveButton().setOnAction(e -> save());
        controls.getSolveButton().setOnAction(e -> solve());
        controls.getClearButton().setOnAction(e -> clear());
    }

    private void addGenerateur() {
        String nom = controls.getNomGenerateurField().getText();
        String capaciteText = controls.getCapaciteField().getText();

        if (nom.isEmpty() || capaciteText.isEmpty()) {
            terminal.appendText("Erreur : Le nom et la capacité du générateur ne peuvent pas être vides.\n");
            return;
        }
        if(reseau.generateurDansReseau(nom)){
            terminal.appendText("Erreur : Un générateur avec le nom '" + nom + "' existe déjà.\n");
            return;
        }
        try {
            int capacite = Integer.parseInt(capaciteText);
            reseau.addGenerateur(new Generateur(nom, capacite));
            generateurNames.add(nom);
            terminal.appendText("Générateur '" + nom + "' avec une capacité de " + capacite + " ajouté.\n");
        } catch (NumberFormatException e) {
            terminal.appendText("Erreur: La capacité doit être un nombre entier.\n");
        }
    }

    private void addMaison() {
        String nom = controls.getNomMaisonField().getText();
        Consommation consommation = controls.getConsommationComboBox().getValue();

        if (nom.isEmpty()) {
            terminal.appendText("Erreur : Le nom de la maison ne peut pas être vide.\n");
            return;
        }
        if(reseau.maisonDansReseau(nom)){
            terminal.appendText("Erreur : Une maison avec le nom '" + nom + "' existe déjà.\n");
            return;
        }
        try {
            reseau.addMaison(new Maison(nom, consommation));
            maisonNames.add(nom);
            terminal.appendText("Maison '" + nom + "' avec une consommation " + consommation + " ajoutée.\n");
        } catch (Exception e) {
            terminal.appendText("Erreur : " + e.getMessage() + "\n");
        }
    }

    private void addConnection() {
        String maisonNom = controls.getMaisonComboBox().getValue();
        String generateurNom = controls.getGenerateurComboBox().getValue();

        if (maisonNom == null || generateurNom == null) {
            terminal.appendText("Erreur : Veuillez sélectionner une maison et un générateur.\n");
            return;
        }
        reseau.addConnexion(maisonNom, generateurNom);
        terminal.appendText("Connexion ajoutée entre '" + maisonNom + "' et '" + generateurNom + "'.\n");
    }

    private void save() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder le réseau");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers texte", "*.txt"));
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                factory.ReseauFactory.reseauToFile(file.getAbsolutePath(), reseau);
                terminal.appendText("Réseau sauvegardé dans : " + file.getAbsolutePath() + "\n");
            } catch (IOException e) {
                terminal.appendText("Erreur lors de la sauvegarde du fichier : " + e.getMessage() + "\n");
            }
        }
    }

    private void solve() {
        if (reseau.getMaisons().isEmpty() || reseau.getGenerateurs().isEmpty()) {
            terminal.appendText("Erreur : Il n'y a pas de maisons ou de générateurs dans le réseau pour lancer le solveur.\n");
            return;
        }
        terminal.appendText("Lancement du solveur...\n");

        reseau.calculCout();
        double oldCost = reseau.getCout();
        terminal.appendText("Cout initial: " + oldCost + "\n");

        reseau.solveurNaif();

        reseau.calculCout();
        double newCost = reseau.getCout();
        terminal.appendText("Nouveau cout: " + newCost + "\n");
        
        terminal.appendText("Solveur terminé.\n");
        terminal.appendText(reseau.toString());
    }

    private void clear() {
        terminal.clear();
    }
}
