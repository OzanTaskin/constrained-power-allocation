package UI.view;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import reseau.Consommation;

public class ControlsView extends VBox {

    private TextField nomGenerateurField;
    private TextField capaciteField;
    private Button addGenerateurButton;

    private TextField nomMaisonField;
    private ComboBox<Consommation> consommationComboBox;
    private Button addMaisonButton;

    private ComboBox<String> maisonComboBox;
    private ComboBox<String> generateurComboBox;
    private Button addConnectionButton;

    private Button saveButton;
    private Button solveButton;

    public ControlsView(ObservableList<String> maisonNames, ObservableList<String> generateurNames) {
        super(20);
        setPadding(new Insets(10));
        setSpacing(10);

        // --- Ajout d'un générateur ---
        Label titleGenerateur = new Label("Ajouter un générateur");
        titleGenerateur.setStyle("-fx-font-weight: bold;");

        GridPane gridGenerateur = new GridPane();
        gridGenerateur.setHgap(10);
        gridGenerateur.setVgap(5);

        nomGenerateurField = new TextField();
        nomGenerateurField.setPromptText("Nom du générateur");
        capaciteField = new TextField();
        capaciteField.setPromptText("Capacité");
        addGenerateurButton = new Button("Ajouter");

        gridGenerateur.add(new Label("Nom:"), 0, 0);
        gridGenerateur.add(nomGenerateurField, 1, 0);
        gridGenerateur.add(new Label("Capacité:"), 0, 1);
        gridGenerateur.add(capaciteField, 1, 1);
        gridGenerateur.add(addGenerateurButton, 1, 2);

        // --- Ajout d'une maison ---
        Label titleMaison = new Label("Ajouter une maison");
        titleMaison.setStyle("-fx-font-weight: bold;");

        GridPane gridMaison = new GridPane();
        gridMaison.setHgap(10);
        gridMaison.setVgap(5);

        nomMaisonField = new TextField();
        nomMaisonField.setPromptText("Nom de la maison");
        consommationComboBox = new ComboBox<>();
        consommationComboBox.getItems().setAll(Consommation.values());
        consommationComboBox.setValue(Consommation.NORMAL);
        addMaisonButton = new Button("Ajouter");

        gridMaison.add(new Label("Nom:"), 0, 0);
        gridMaison.add(nomMaisonField, 1, 0);
        gridMaison.add(new Label("Consommation:"), 0, 1);
        gridMaison.add(consommationComboBox, 1, 1);
        gridMaison.add(addMaisonButton, 1, 2);

        // --- Ajout d'une connexion ---
        Label titleConnection = new Label("Ajouter une connexion");
        titleConnection.setStyle("-fx-font-weight: bold;");

        GridPane gridConnection = new GridPane();
        gridConnection.setHgap(10);
        gridConnection.setVgap(5);

        maisonComboBox = new ComboBox<>(maisonNames);
        generateurComboBox = new ComboBox<>(generateurNames);
        addConnectionButton = new Button("Connecter");

        gridConnection.add(new Label("Maison:"), 0, 0);
        gridConnection.add(maisonComboBox, 1, 0);
        gridConnection.add(new Label("Générateur:"), 0, 1);
        gridConnection.add(generateurComboBox, 1, 1);
        gridConnection.add(addConnectionButton, 1, 2);

        // --- Actions ---
        Label titleActions = new Label("Actions");
        titleActions.setStyle("-fx-font-weight: bold;");

        saveButton = new Button("Sauvegarder");
        solveButton = new Button("Optimiser");

        VBox actionsBox = new VBox(10, saveButton, solveButton);

        getChildren().addAll(
                titleGenerateur, gridGenerateur,
                new Separator(),
                titleMaison, gridMaison,
                new Separator(),
                titleConnection, gridConnection,
                new Separator(),
                titleActions, actionsBox
        );
    }

    // Getters pour que le contrôleur puisse accéder aux composants
    public TextField getNomGenerateurField() { return nomGenerateurField; }
    public TextField getCapaciteField() { return capaciteField; }
    public Button getAddGenerateurButton() { return addGenerateurButton; }
    public TextField getNomMaisonField() { return nomMaisonField; }
    public ComboBox<Consommation> getConsommationComboBox() { return consommationComboBox; }
    public Button getAddMaisonButton() { return addMaisonButton; }
    public ComboBox<String> getMaisonComboBox() { return maisonComboBox; }
    public ComboBox<String> getGenerateurComboBox() { return generateurComboBox; }
    public Button getAddConnectionButton() { return addConnectionButton; }
    public Button getSaveButton() { return saveButton; }
    public Button getSolveButton() { return solveButton; }
}
