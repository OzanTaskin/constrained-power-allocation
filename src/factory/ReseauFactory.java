package factory;

import reseau.Reseau;
import reseau.Maison;
import reseau.Generateur;

import java.io.*;
import java.util.Map;

/**
 * Fabrique pour la création et la sauvegarde de réseaux électriques.
 * <p>
 * Gère le parsing de fichiers texte au format Prolog et la sérialisation
 * des réseaux. Valide la structure et l'ordre des déclarations.
 *
 * @author Votre nom
 * @version 1.0
 */
public class ReseauFactory {

    private ReseauFactory() {}

    /**
     * Parse un réseau électrique depuis un fichier texte.
     * <p>
     * Format attendu (ordre strict) :
     * <ol>
     * <li>Déclarations des générateurs : {@code generateur(nom, capacite).}</li>
     * <li>Déclarations des maisons : {@code maison(nom, CONSOMMATION).}</li>
     * <li>Déclarations des connexions : {@code connexion(maison, generateur).}</li>
     * </ol>
     * La consommation doit être BASSE, NORMAL ou FORTE.
     *
     * @param penalite le coefficient de pénalité du réseau
     * @param fichier le chemin du fichier à parser
     * @return le réseau construit
     * @throws FileNotFoundException si le fichier n'existe pas
     * @throws IOException si erreur de lecture ou format invalide
     */
    public static Reseau parserReseau(double penalite, String fichier)
            throws FileNotFoundException, IOException {

        Reseau r = new Reseau(penalite);

        boolean maisonsCommencees = false;
        boolean connexionsCommencees = false;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            int compteur = 0;

            while ((ligne = br.readLine()) != null) {
                compteur++;
                ligne = ligne.trim();

                if (ligne.isEmpty()) {
                    continue;
                }

                String type = extraireType(ligne);

                try {
                    switch (type) {
                        case "generateur": {
                            if (maisonsCommencees || connexionsCommencees) {
                                throw new IOException("Ligne " + compteur
                                        + " : les générateurs doivent être déclarés avant les maisons et connexions");
                            }
                            traiterGenerateur(ligne, r, compteur);
                            break;
                        }
                        case "maison": {
                            if (connexionsCommencees) {
                                throw new IOException("Ligne " + compteur
                                        + " : les maisons doivent être déclarées avant les connexions");
                            }
                            maisonsCommencees = true;
                            traiterMaison(ligne, r, compteur);
                            break;
                        }
                        case "connexion": {
                            connexionsCommencees = true;
                            traiterConnexion(ligne, r, compteur);
                            break;
                        }
                        default: {
                            throw new IOException("Ligne " + compteur
                                    + " : type invalide. Attendu : generateur, maison ou connexion");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    throw new IOException("Ligne " + compteur + " : " + e.getMessage(), e);
                } catch (Exception e) {
                    throw new IOException("Ligne " + compteur + " : " + e.getMessage(), e);
                }
            }
        }

        return r;
    }

    /**
     * Extrait le type de déclaration d'une ligne Prolog.
     *
     * @param ligne la ligne à analyser
     * @return le type (generateur, maison, ou connexion)
     * @throws IllegalArgumentException si le format est invalide
     */
    private static String extraireType(String ligne) throws IllegalArgumentException {
        if (!ligne.contains("(")) {
            throw new IllegalArgumentException("parenthèse ouvrante manquante");
        }
        return ligne.substring(0, ligne.indexOf('(')).trim();
    }

    /**
     * Traite une ligne de déclaration de générateur.
     *
     * @param ligne la ligne à traiter
     * @param r le réseau cible
     * @param compteur le numéro de ligne
     * @throws IllegalArgumentException si le format ou les valeurs sont invalides
     */
    private static void traiterGenerateur(String ligne, Reseau r, int compteur)
            throws IllegalArgumentException {
        String[] data = recupererDonnees(ligne);
        validerNombreParametres(data, 2, compteur);

        String nom = data[0].trim();
        int capacite;

        try {
            capacite = Integer.parseInt(data[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("la capacité doit être un entier");
        }

        if (capacite <= 0) {
            throw new IllegalArgumentException("la capacité doit être supérieure à 0");
        }

        r.addGenerateur(nom, capacite);
    }

    /**
     * Traite une ligne de déclaration de maison.
     *
     * @param ligne la ligne à traiter
     * @param r le réseau cible
     * @param compteur le numéro de ligne
     * @throws Exception si le format ou les valeurs sont invalides
     */
    private static void traiterMaison(String ligne, Reseau r, int compteur) throws Exception {
        String[] data = recupererDonnees(ligne);
        validerNombreParametres(data, 2, compteur);

        String nom = data[0].trim();
        String consommation = data[1].trim().toUpperCase();

        if (!consoValide(consommation)) {
            throw new IllegalArgumentException("la consommation doit être BASSE, NORMAL ou FORTE");
        }

        r.addMaison(nom, consommation);
    }

    /**
     * Traite une ligne de déclaration de connexion.
     *
     * @param ligne la ligne à traiter
     * @param r le réseau cible
     * @param compteur le numéro de ligne
     * @throws IllegalArgumentException si la connexion est invalide
     */
    private static void traiterConnexion(String ligne, Reseau r, int compteur)
            throws IllegalArgumentException {
        String[] data = recupererDonnees(ligne);
        validerNombreParametres(data, 2, compteur);

        String elem1 = data[0].trim();
        String elem2 = data[1].trim();

        if (r.maisonDansReseau(elem1) && r.generateurDansReseau(elem2)) {
            r.addConnexion(elem1, elem2);
        } else if (r.maisonDansReseau(elem2) && r.generateurDansReseau(elem1)) {
            r.addConnexion(elem2, elem1);
        } else {
            throw new IllegalArgumentException("maison et/ou générateur inexistant : "
                    + elem1 + ", " + elem2);
        }
    }

    /**
     * Extrait les données d'une ligne au format Prolog.
     * <p>
     * Format attendu : {@code type(param1, param2).}
     *
     * @param ligne la ligne à parser
     * @return un tableau contenant les paramètres extraits
     * @throws IllegalArgumentException si le format est incorrect
     */
    private static String[] recupererDonnees(String ligne) throws IllegalArgumentException {
        if (ligne == null || ligne.isEmpty()) {
            throw new IllegalArgumentException("ligne vide");
        }

        if (ligne.charAt(ligne.length() - 1) != '.') {
            throw new IllegalArgumentException("point final manquant");
        }

        if (!ligne.contains("(") || !ligne.contains(")")) {
            throw new IllegalArgumentException("parenthèses manquantes");
        }

        int indexDebut = ligne.indexOf('(');
        int indexFin = ligne.lastIndexOf(')');

        if (indexDebut >= indexFin) {
            throw new IllegalArgumentException("parenthèses mal placées");
        }

        String contenu = ligne.substring(indexDebut + 1, indexFin);
        String[] donnees = contenu.split(",");

        return donnees;
    }

    /**
     * Valide le nombre de paramètres extraits.
     *
     * @param data le tableau de paramètres
     * @param attendu le nombre attendu
     * @param ligne le numéro de ligne
     * @throws IllegalArgumentException si le nombre est incorrect
     */
    private static void validerNombreParametres(String[] data, int attendu, int ligne)
            throws IllegalArgumentException {
        if (data.length != attendu) {
            throw new IllegalArgumentException("nombre de paramètres incorrect : "
                    + data.length + " trouvé(s), " + attendu + " attendu(s)");
        }
    }

    /**
     * Vérifie si une valeur de consommation est valide.
     *
     * @param s la chaîne à vérifier
     * @return true si la consommation est BASSE, NORMAL ou FORTE
     */
    private static boolean consoValide(String s) {
        return s.equals("BASSE") || s.equals("NORMAL") || s.equals("FORTE");
    }

    /**
     * Sauvegarde un réseau dans un fichier texte au format Prolog.
     * <p>
     * Format de sortie :
     * <ol>
     * <li>Tous les générateurs</li>
     * <li>Toutes les maisons</li>
     * <li>Toutes les connexions existantes</li>
     * </ol>
     * Les maisons non connectées ne génèrent pas de ligne connexion.
     *
     * @param fichier le chemin du fichier de destination
     * @param r le réseau à sauvegarder
     * @throws IOException si erreur d'écriture
     */
    public static void reseauToFile(String fichier, Reseau r) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fichier))) {
            // Écriture des générateurs
            for (Generateur g : r.getGenerateurs()) {
                bw.write(g.toString());
                bw.newLine();
            }

            // Écriture des maisons
            for (Maison m : r.getMaisons()) {
                bw.write(m.toString());
                bw.newLine();
            }

            // Écriture des connexions (seulement les maisons connectées)
            for (Map.Entry<Maison, Generateur> connexion : r.getConnexions().entrySet()) {
                if (connexion.getValue() != null) {
                    bw.write("connexion(" + connexion.getKey().getNom() + ","
                            + connexion.getValue().getNom() + ").");
                    bw.newLine();
                }
            }
        }
    }
}
