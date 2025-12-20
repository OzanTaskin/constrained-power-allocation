package factory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import reseau.Consommation;
import reseau.Generateur;
import reseau.Maison;
import reseau.Reseau;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test pour la création d'un réseau à partir d'un fichier et l'écriture d'un réseau dans un fichier.
 */
class ReseauFactoryTest {

    /*
     * Test pour vérifier que le parsing d'un fichier de configuration réseau fonctionne correctement.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/parser_reseau_tests.csv", numLinesToSkip = 1)
    void testAnalyseurReseau(String contenu, double penalite, int nombreGenerateursAttendu, int nombreMaisonsAttendu, String nomGenerateur, int capaciteGenerateur, String nomMaison, String consommationMaison) throws Exception {
        File fichierEntree = File.createTempFile("test_reseau", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichierEntree))) {
            writer.write(contenu.replace("\\n", System.lineSeparator()));
        }

        // On parse le reseau
        Reseau reseau = ReseauFactory.parserReseau(penalite, fichierEntree.getAbsolutePath());

        // On vérifie que l'objet Reseau est correct
        assertNotNull(reseau);
        assertEquals(penalite, reseau.getPenalite());

        // On vérifie le générateur
        assertEquals(nombreGenerateursAttendu, reseau.getGenerateurs().size());
        Generateur generateur = reseau.getGenerateur(nomGenerateur);
        assertNotNull(generateur);
        assertEquals(nomGenerateur, generateur.getNom());
        assertEquals(capaciteGenerateur, generateur.getCapacite());

        // On vérifie la maison
        assertEquals(nombreMaisonsAttendu, reseau.getMaisons().size());
        Maison maison = reseau.getMaison(nomMaison);
        assertNotNull(maison);
        assertEquals(nomMaison, maison.getNom());
        assertEquals(Consommation.valueOf(consommationMaison).getConsommation(), maison.getConsommation());

        // On vérifie la connexion
        assertNotNull(reseau.getConnexions().get(maison));
        assertEquals(generateur, reseau.getConnexions().get(maison));

        fichierEntree.delete();
    }

    /*
     * Test pour vérifier que l'écriture d'un objet Reseau dans un fichier fonctionne correctement.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/reseau_to_file_tests.csv", numLinesToSkip = 1)
    void testReseauVersFichier(double penalite, String nomGenerateur, int capaciteGenerateur, String nomMaison, String consommationMaison, String maisonConnectee, String generateurConnecte) throws Exception {
        // 1. Création d'un objet Reseau
        Reseau reseau = new Reseau(penalite);
        reseau.addGenerateur(nomGenerateur, capaciteGenerateur);
        reseau.addMaison(nomMaison, consommationMaison);
        reseau.addConnexion(maisonConnectee, generateurConnecte);

        File fichierSortie = File.createTempFile("output_reseau", ".txt");

        // 2. On écrit le réseau dans un fichier
        ReseauFactory.reseauToFile(fichierSortie.getAbsolutePath(), reseau);

        // 3. On lit le fichier et on vérifie son contenu
        List<String> lignes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fichierSortie))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lignes.add(line);
            }
        }
        
        // L'ordre n'est pas garanti, donc on vérifie la présence de chaque ligne attendue
        assertEquals(3, lignes.size());
        assertTrue(lignes.contains("generateur(" + nomGenerateur + "," + capaciteGenerateur + ")."));
        assertTrue(lignes.contains("maison(" + nomMaison + "," + consommationMaison + ")."));
        assertTrue(lignes.contains("connexion(" + maisonConnectee + "," + generateurConnecte + ")."));

        fichierSortie.delete();
    }
}
