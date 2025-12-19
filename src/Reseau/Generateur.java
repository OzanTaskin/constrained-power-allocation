package reseau;

/**
 * Représente un générateur électrique du réseau.
 * <p>
 * Chaque générateur possède une capacité maximale et gère
 * dynamiquement sa charge actuelle en fonction des maisons connectées.
 *
 * @author Votre nom
 * @version 1.0
 */
public class Generateur {

    /** Capacité maximale du générateur en kW */
    private int capacite;

    /** Charge actuelle du générateur en kW */
    private int chargeActuelle;

    /** Nom identifiant unique du générateur */
    private String nom;

    /**
     * Crée un nouveau générateur avec une capacité définie.
     * <p>
     * La charge actuelle est initialisée à 0.
     *
     * @param nom le nom identifiant du générateur
     * @param capacite la capacité maximale en kW, ne peut pas être négative.
     * @throws IllegalArgumentException si la capacité est négative.
     */
    public Generateur(String nom, int capacite) {
        if (capacite < 0) {
            throw new IllegalArgumentException("La capacité d'un générateur ne peut pas être négative.");
        }
        this.capacite = capacite;
        this.nom = nom;
        this.chargeActuelle = 0;
    }

    /**
     * Calcule le taux d'utilisation actuel du générateur.
     * <p>
     * Ratio entre la charge actuelle et la capacité maximale.
     * Un taux supérieur à 1.0 indique une surcharge.
     *
     * @return le taux d'utilisation (0.0 si capacité nulle)
     */
    public double calculTauxUtilisation() {
        return capacite == 0 ? 0.0 : (double) chargeActuelle / capacite;
    }

    /**
     * Ajoute une maison au générateur et augmente la charge actuelle.
     * <p>
     * Utilisée lors de la création d'une connexion.
     *
     * @param m la maison à ajouter
     */
    public void addMaison(Maison m) {
        chargeActuelle += m.getConsommation();
    }

    /**
     * Retire une maison du générateur et diminue la charge actuelle.
     * <p>
     * Utilisée lors de la suppression ou du changement d'une connexion.
     *
     * @param m la maison à retirer
     */
    public void supprimerMaison(Maison m) {
        chargeActuelle -= m.getConsommation();
    }

    /**
     * Retourne la charge actuelle du générateur.
     *
     * @return la charge actuelle en kW
     */
    public int getChargeActuelle() {
        return chargeActuelle;
    }

    /**
     * Retourne la capacité maximale du générateur.
     *
     * @return la capacité en kW
     */
    public int getCapacite() {
        return capacite;
    }

    /**
     * Retourne le nom du générateur.
     *
     * @return le nom identifiant du générateur
     */
    public String getNom() {
        return nom;
    }

    /**
     * Retourne une représentation textuelle du générateur au format Prolog.
     * <p>
     * Format : {@code generateur(nom, capacite).}
     *
     * @return la chaîne de caractères représentant le générateur
     */
    public String toString() {
        return "generateur(" + nom + "," + capacite + ").";
    }
}

