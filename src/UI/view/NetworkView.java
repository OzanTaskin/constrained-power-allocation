package ui.view;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import reseau.Generateur;
import reseau.Maison;
import reseau.Reseau;

import java.util.HashMap;
import java.util.Map;

public class NetworkView extends Pane {

    private Reseau reseau;
    private Map<Generateur, Circle> generateurCircles = new HashMap<>();
    private Map<Maison, Circle> maisonCircles = new HashMap<>();


    public NetworkView(Reseau reseau) {
        this.reseau = reseau;
        setStyle("-fx-background-color: #f0f0f0;"); // Arrière-plan gris clair
    }

    public void update() {
        getChildren().clear();
        generateurCircles.clear();
        maisonCircles.clear();

        drawGenerateurs();
        drawMaisons();
        drawConnections(); // Les connexions doivent être dessinées après les cercles pour être au-dessus

        // Ajuster la taille du panneau pour s'adapter à tout le contenu, pour le ScrollPane
        int maxItems = Math.max(reseau.getGenerateurs().size(), reseau.getMaisons().size());
        double paneHeight = Math.max(600, (maxItems * 80.0) + 50); // 80px par item + 50px de marge inférieure
        setPrefSize(600, paneHeight);
    }

    private void drawGenerateurs() {
        double x = 100;
        double y = 50;
        for (Generateur generateur : reseau.getGenerateurs()) {
            Circle circle = new Circle(x, y, 20, Color.BLUE);
            Text nameText = new Text(x - (generateur.getNom().length() * 3), y - 30, generateur.getNom());
            Text text = new Text(x - 40, y + 35, "Capacité: " + generateur.getCapacite());
            getChildren().addAll(circle, nameText, text);
            generateurCircles.put(generateur, circle);
            y += 80;
        }
    }

    private void drawMaisons() {
        double x = 400;
        double y = 50;
        for (Maison maison : reseau.getMaisons()) {
            Circle circle = new Circle(x, y, 20, Color.GREEN);
            Text nameText = new Text(x - (maison.getNom().length() * 3), y - 30, maison.getNom());
            Text text = new Text(x - 50, y + 35, "Consommation: " + maison.getConsommation());
            getChildren().addAll(circle, nameText, text);
            maisonCircles.put(maison, circle);
            y += 80;
        }
    }

    private void drawConnections() {
        // S'assurer que les lignes sont dessinées derrière les cercles en les insérant au début de la liste des enfants
        int lineInsertIndex = 0;
        for (Map.Entry<Maison, Generateur> entry : reseau.getConnexions().entrySet()) {
            Maison maison = entry.getKey();
            Generateur generateur = entry.getValue();

            if (maison != null && generateur != null) {
                Circle maisonCircle = maisonCircles.get(maison);
                Circle generateurCircle = generateurCircles.get(generateur);

                if (maisonCircle != null && generateurCircle != null) {
                    Line line = new Line(
                            maisonCircle.getCenterX(), maisonCircle.getCenterY(),
                            generateurCircle.getCenterX(), generateurCircle.getCenterY()
                    );
                    line.setStroke(Color.BLACK);
                    getChildren().add(lineInsertIndex++, line); // Ajouter la ligne à l'arrière
                }
            }
        }
    }
}
