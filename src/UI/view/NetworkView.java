package UI.view;

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
        setStyle("-fx-background-color: #f0f0f0;"); // Light gray background
    }

    public void update() {
        getChildren().clear();
        generateurCircles.clear();
        maisonCircles.clear();

        drawGenerateurs();
        drawMaisons();
        drawConnections(); // Connections should be drawn after circles to be on top

        // Adjust pane size to fit all content, for the ScrollPane
        int maxItems = Math.max(reseau.getGenerateurs().size(), reseau.getMaisons().size());
        double paneHeight = Math.max(600, (maxItems * 80.0) + 50); // 80px per item + 50px bottom buffer
        setPrefSize(600, paneHeight);
    }

    private void drawGenerateurs() {
        double x = 100;
        double y = 50;
        for (Generateur generateur : reseau.getGenerateurs()) {
            Circle circle = new Circle(x, y, 20, Color.BLUE);
            Text text = new Text(x - 40, y + 35, "Capacité: " + generateur.getCapacite());
            getChildren().addAll(circle, text);
            generateurCircles.put(generateur, circle);
            y += 80;
        }
    }

    private void drawMaisons() {
        double x = 400;
        double y = 50;
        for (Maison maison : reseau.getMaisons()) {
            Circle circle = new Circle(x, y, 20, Color.GREEN);
            Text text = new Text(x - 50, y + 35, "Consommation: " + maison.getConsommation());
            getChildren().addAll(circle, text);
            maisonCircles.put(maison, circle);
            y += 80;
        }
    }

    private void drawConnections() {
        // Ensure lines are drawn behind the circles by inserting them at the beginning of the children list
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
                    getChildren().add(lineInsertIndex++, line); // Add line at the back
                }
            }
        }
    }
}
