package reseau;

import java.util.*;

/**
 * Représente un réseau électrique avec ses générateurs et connexions.
 * <p>
 * Gère l'ensemble des maisons, générateurs et leurs connexions. Calcule
 * le coût global du réseau basé sur la dispersion des taux d'utilisation
 * et les pénalités de surcharge.
 *
 * @author Votre nom
 * @version 1.0
 */
public class Reseau {

    /** Liste des générateurs du réseau */
    private List<Generateur> generateurs;

    /** Map associant chaque maison à son générateur */
    private HashMap<Maison, Generateur> connexions;

    /** Coût total du réseau (dispersion + pénalités) */
    private double cout;

    /** Moyenne des taux d'utilisation de tous les générateurs */
    private double tauxUtilisationMoyen;

    /** Somme des écarts absolus entre chaque taux d'utilisation et la moyenne */
    private double disp;

    /** Coefficient de pénalité appliqué aux surcharges */
    private double penalite;

    /** Somme des surcharges de tous les générateurs */
    private double surcharge;

    /** Charge totale actuelle du réseau en kW */
    private int charge;

    /** Capacité totale du réseau en kW */
    private int capacite;

    /**
     * Crée un nouveau réseau électrique vide.
     *
     * @param penalite le coefficient de pénalité pour les surcharges
     */
    public Reseau(double penalite) {
        this.generateurs = new ArrayList<Generateur>();
        this.connexions = new HashMap<Maison, Generateur>();
        this.penalite = penalite;
        capacite = 0;
        charge = 0;
    }

    /**
     * Ajoute une maison au réseau sans la connecter.
     * <p>
     * Vérifie que la capacité totale du réseau est suffisante
     * pour supporter la charge supplémentaire.
     *
     * @param maison la maison à ajouter
     * @throws IllegalStateException si la capacité du réseau est insuffisante
     */
    public void addMaison(Maison maison) throws IllegalStateException {
        charge += maison.getConsommation();
        if (charge > capacite) {
            charge -= maison.getConsommation(); // Rollback
            throw new IllegalStateException(
                    "La capacité du réseau doit être supérieure à sa charge.\n" +
                            "Ajoutez d'abord un générateur avant d'ajouter une nouvelle maison");
        }
        connexions.put(maison, null);
    }

    /**
     * Ajoute une maison au réseau à partir de son nom et sa consommation.
     *
     * @param nom le nom de la maison
     * @param conso la consommation de la maison (BASSE, NORMAL ou FORTE)
     * @throws IllegalStateException si la capacité du réseau est insuffisante
     * @throws IllegalArgumentException si la consommation n'est pas valide
     */
    public void addMaison(String nom, String conso) throws IllegalStateException, IllegalArgumentException {
        Consommation c = Consommation.valueOf(conso); // Peut lancer IllegalArgumentException
        Maison m = new Maison(nom, c);
        addMaison(m);
    }

    /**
     * Ajoute un générateur au réseau et augmente la capacité totale.
     *
     * @param generateur le générateur à ajouter
     */
    public void addGenerateur(Generateur generateur) {
        generateurs.add(generateur);
        capacite += generateur.getCapacite();
    }

    /**
     * Ajoute un générateur au réseau à partir de son nom et sa capacité.
     *
     * @param nom le nom du générateur
     * @param charge la capacité du générateur en kW
     */
    public void addGenerateur(String nom, int charge) {
        addGenerateur(new Generateur(nom, charge));
    }

    /**
     * Crée une connexion entre une maison et un générateur.
     * <p>
     * Met à jour la map de connexions et ajoute la maison au générateur.
     *
     * @param maison la maison à connecter
     * @param generateur le générateur cible
     */
    public void addConnexion(Maison maison, Generateur generateur) {
        connexions.put(maison, generateur);
        generateur.addMaison(maison);
    }

    /**
     * Crée une connexion à partir des noms de la maison et du générateur.
     *
     * @param maisonNom le nom de la maison
     * @param generateurNom le nom du générateur
     */
    public void addConnexion(String maisonNom, String generateurNom) {
        addConnexion(getMaison(maisonNom), getGenerateur(generateurNom));
    }

    /**
     * Supprime la connexion entre une maison et un générateur.
     * <p>
     * La maison reste dans le réseau mais n'est plus connectée.
     *
     * @param maison la maison à déconnecter
     * @param generateur le générateur à déconnecter
     */
    public void supprConnexion(Maison maison, Generateur generateur) {
        connexions.put(maison, null);
        generateur.supprimerMaison(maison);
    }

    /**
     * Change la connexion d'une maison d'un générateur vers un autre.
     * <p>
     * Opération atomique utilisée par les algorithmes d'optimisation.
     *
     * @param m1 la maison à déplacer
     * @param g1 le générateur source
     * @param g2 le générateur cible
     */
    public void changeConnexion(Maison m1, Generateur g1, Generateur g2) {
        connexions.put(m1, g2);
        g1.supprimerMaison(m1);
        g2.addMaison(m1);
    }

    /**
     * Calcule le taux d'utilisation moyen de tous les générateurs.
     * <p>
     * Utilisé comme référence pour le calcul de dispersion.
     */
    private void calculTauxUtilisationMoyen() {
        tauxUtilisationMoyen = 0;
        for (Generateur g : generateurs) {
            tauxUtilisationMoyen += g.calculTauxUtilisation();
        }
        tauxUtilisationMoyen = tauxUtilisationMoyen / generateurs.size();
    }

    /**
     * Calcule la dispersion et la surcharge totale du réseau.
     * <p>
     * Dispersion : somme des écarts absolus au taux moyen.
     * Surcharge : somme des dépassements de capacité (taux &gt; 1).
     */
    private void calculDispEtSurcharge() {
        for (Generateur g : generateurs) {
            double tauxUtilisation = g.calculTauxUtilisation();
            disp += Math.abs(tauxUtilisationMoyen - tauxUtilisation);
            if (tauxUtilisation > 1) {
                surcharge += tauxUtilisation - 1;
            }
        }
    }

    /**
     * Calcule le coût total du réseau.
     * <p>
     * Formule : coût = dispersion + (pénalité × surcharge)
     * <p>
     * Réinitialise et recalcule tous les indicateurs.
     */
    public void calculCout() {
        disp = 0;
        surcharge = 0;
        calculTauxUtilisationMoyen();
        calculDispEtSurcharge();
        cout = disp + (penalite * surcharge);
    }

    /**
     * Vérifie si un générateur existe déjà dans le réseau.
     *
     * @param g le générateur à vérifier
     * @return true si le générateur est présent dans le réseau
     */
    public boolean generateurDansReseau(Generateur g) {
        return generateurs.contains(g);
    }

    /**
     * Vérifie si un générateur existe dans le réseau par son nom.
     *
     * @param s le nom du générateur
     * @return true si un générateur avec ce nom existe
     */
    public boolean generateurDansReseau(String s) {
        for (Generateur g : generateurs) {
            if (g.getNom().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si une maison existe dans le réseau.
     *
     * @param m la maison à vérifier
     * @return true si la maison est présente dans le réseau
     */
    public boolean maisonDansReseau(Maison m) {
        return connexions.containsKey(m);
    }

    /**
     * Vérifie si une maison existe dans le réseau par son nom.
     *
     * @param s le nom de la maison
     * @return true si une maison avec ce nom existe
     */
    public boolean maisonDansReseau(String s) {
        for (Maison m : connexions.keySet()) {
            if (m.getNom().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si une maison est actuellement connectée à un générateur.
     *
     * @param m la maison à vérifier
     * @return true si la maison est connectée, false sinon
     */
    public boolean maisonConnecte(Maison m) {
        return connexions.containsKey(m) && connexions.get(m) != null;
    }

    /**
     * Recherche un générateur par son nom.
     *
     * @param g le nom du générateur
     * @return le générateur correspondant, ou null si non trouvé
     */
    public Generateur getGenerateur(String g) {
        for (Generateur generateur : generateurs) {
            if (generateur.getNom().equals(g)) {
                return generateur;
            }
        }
        return null;
    }

    /**
     * Recherche une maison par son nom.
     *
     * @param maison le nom de la maison
     * @return la maison correspondante, ou null si non trouvée
     */
    public Maison getMaison(String maison) {
        for (Maison m : connexions.keySet()) {
            if (m.getNom().equals(maison)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Retourne une représentation textuelle des connexions du réseau.
     * <p>
     * Format : {@code maison ----- générateur} pour chaque connexion.
     *
     * @return la chaîne représentant toutes les connexions
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Maison, Generateur> e : connexions.entrySet()) {
            if (e.getValue() != null) {
                sb.append(e.getKey().getNom());
                sb.append(" ----- ");
                sb.append(e.getValue().getNom());
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Retourne l'ensemble de toutes les maisons du réseau.
     *
     * @return le Set des maisons
     */
    public Set<Maison> getMaisons() {
        return Collections.unmodifiableSet(connexions.keySet());
    }

    /**
     * Retourne l'ensemble des maisons connectées à un générateur donné.
     *
     * @param g le générateur
     * @return le Set des maisons connectées à ce générateur
     */
    public Set<Maison> getMaisons(Generateur g) {
        Set<Maison> maisons = new HashSet<Maison>();
        for (Maison m : connexions.keySet()) {
            if (connexions.get(m) == g) {
                maisons.add(m);
            }
        }
        return maisons;
    }

    /**
     * Retourne la map complète des connexions maison-générateur.
     *
     * @return la HashMap des connexions
     */
    public Map<Maison, Generateur> getConnexions() {
        return Collections.unmodifiableMap(connexions);
    }

    /**
     * Retourne le coût total du réseau.
     *
     * @return le coût calculé
     */
    public double getCout() {
        return cout;
    }

    /**
     * Retourne le taux d'utilisation moyen des générateurs.
     *
     * @return le taux moyen
     */
    public double getTauxUtilisationMoyen() {
        return tauxUtilisationMoyen;
    }

    /**
     * Retourne la dispersion du réseau.
     *
     * @return la dispersion calculée
     */
    public double getDisp() {
        return disp;
    }

    /**
     * Retourne le coefficient de pénalité.
     *
     * @return le coefficient de pénalité
     */
    public double getPenalite() {
        return penalite;
    }

    /**
     * Retourne la surcharge totale du réseau.
     *
     * @return la surcharge calculée
     */
    public double getSurcharge() {
        return surcharge;
    }

    /**
     * Retourne la charge totale actuelle du réseau.
     *
     * @return la charge en kW
     */
    public int getCharge() {
        return charge;
    }

    /**
     * Retourne la capacité totale du réseau.
     *
     * @return la capacité en kW
     */
    public int getCapacite() {
        return capacite;
    }

    /**
     * Retourne la liste des générateurs du réseau.
     *
     * @return la List des générateurs
     */
    public List<Generateur> getGenerateurs() {
        return generateurs;
    }
}
