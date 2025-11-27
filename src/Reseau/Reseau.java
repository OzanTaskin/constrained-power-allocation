package reseau;

import java.util.*;

/**
 * Liaison d'une ou plusieurs maisons à un ou plusieurs generateurs
 */
public class Reseau {
    private String nom;


    /** Ensemble des generateurs du reseau */
    private List<Generateur> generateurs;

    /** Ensemble des connexions du reseau */
    private HashMap<Maison, Generateur> connexions;

    /** Cout du reseau */
    private double cout;

    /** La moyenne des taux d'utilisation de tous les generateurs */
    private double tauxUtilisationMoyen;

    /** la somme des écarts de chaque taux d'utilisation par rapport à la moyenne */
    private double disp;

    /** severite de la penalite en cas de surcharge */
    private double penalite;

    /** la somme de la surcharge de chaque generateur */
    private double surcharge;

    /** charge actuelle du reseau */
    private int charge;

    /** capacite du reseau */
    private int capacite;

    /**
     * Instancie un nouveau reseau
     * @param nom Nom du reseau
     * @param penalite Penalite de la surcharge
     */
    public Reseau(String nom, double penalite) {
        this.nom = nom;
        this.generateurs = new ArrayList<Generateur>();
        this.connexions = new HashMap<Maison, Generateur>();
        this.penalite = penalite;
        capacite = 0;
        charge = 0;
    }

    /**
     * Ajoute la maison au reseau
     * @param maison
     */
    public void addMaison(Maison maison) throws Exception{
        charge += maison.getConsommation();
        if(charge > capacite){
            throw new Exception("La capacité du réseau doit être supérieur à sa charge.\nAjoutez d'abord un générateur avant d'ajouter une nouvelle maison");
        }
        connexions.put(maison,null);
    }

    public void addMaison(String nom,String conso) throws Exception {
        Consommation c = Consommation.valueOf(conso);
        Maison m = new Maison(nom,c);
        addMaison(m);
    }

    /**
     * Ajoute un generateur au reseau
     * @param generateur
     */
    public void addGenerateur(Generateur generateur) {
        generateurs.add(generateur);
        capacite += generateur.getCapacite();
    }

    /**
     * Ajoute un generateur au reseau
     * @param nom
     * @param charge
     */
    public void addGenerateur(String nom, int charge) {
        addGenerateur(new Generateur(nom,charge));
    }

    /**
     * Ajoute une connexion au reseau
     * @param maison Maison à relié au generateur
     * @param generateur Generateur relié a la maison
     */
    public void addConnexion(Maison maison, Generateur generateur) {
        connexions.put(maison, generateur);
        generateur.addMaison(maison);
    }

    public void addConnexion(String maisonNom, String generateurNom) {
        addConnexion(getMaison(maisonNom),getGenerateur(generateurNom));
    }

    public void supprConnexion(Maison maison, Generateur generateur) {
        connexions.put(maison,null);
        generateur.supprimerMaison(maison);
    }

    /**
     * Changement de connexion de la maison m1
     * @param m1 Maison qui change de generateur
     * @param g1 Generateur initial
     * @param g2 Nouveau generateur
     */
    public void changeConnexion(Maison m1, Generateur g1, Generateur g2) {
        connexions.put(m1, g2);
        g1.deleteMaison(m1);
        g2.addMaison(m1);
    }

    /**
     * Verifie si chaque maison est bien connecté à un et un seul generateur
     * @return
     */
    public boolean verification() {
        return true;
    }

    /**
     * Calcule le taux d'utilisation moyen du reseau
     */
    private void calculTauxUtilisationMoyen() {
        tauxUtilisationMoyen = 0;
        for (Generateur g : generateurs) {
            tauxUtilisationMoyen += g.calculTauxUtilisation();
        }
        tauxUtilisationMoyen = tauxUtilisationMoyen / generateurs.size();
    }

    /**
     * Calcule disp et la surcharge totale du reseau
     */
    private void calculDispEtSurcharge(){
        for (Generateur g : generateurs) {
            double tauxUtilisation = g.calculTauxUtilisation();
            disp += Math.abs(tauxUtilisationMoyen - tauxUtilisation);
            if(tauxUtilisation>1){
                surcharge += tauxUtilisation-1;
            }
        }
    }


    /**
     * Calcul le cout du reseau
     */
    public void calculCout() {
        disp = 0;
        surcharge = 0;
        calculTauxUtilisationMoyen();
        calculDispEtSurcharge();
        cout = disp + (penalite*surcharge);
    }

    /**
     * Regarde si le generateur est deja dans le reseau
     * @param g generateur à vérifier
     * @return True si generateur existe deja sinon False
     */
    public boolean generateurDansReseau(Generateur g) {
        return generateurs.contains(g);
    }

    /**
     * Regarde si le generateur est deja dans le reseau
     * @param s nom du generateur à vérifier
     * @return True si generateur existe deja sinon False
     */
    public boolean generateurDansReseau(String s){
        for(Generateur g : generateurs){
            if(g.getNom().equals(s)){
                return true;
            }
        }
        return false;
    }

    /**
     * Regarde si la maison est deja dans le reseau
     * @param m maison à verifier
     * @return  True si maison exite deja sinon False
     */
    public boolean maisonDansReseau(Maison m) {
        return connexions.containsKey(m);
    }

    /**
     * Regarde si la maison est deja dans le reseau
     * @param s nom de la maison à verifier
     * @return  True si maison exite deja sinon False
     */
    public boolean maisonDansReseau(String s){
        for(Maison m : connexions.keySet()){
            if(m.getNom().equals(s)){
                return true;
            }
        }
        return false;
    }

    /**
     * Regarde si la maison est déjà connectée à un generateur
     * @param m maison a verifier
     * @return True si maison deja connectée sinon False
     */
    public boolean maisonConnecte(Maison m) {
        if(connexions.containsKey(m)){
            if(connexions.get(m)!=null){
                return true;
            }else{
                return false;
            }
        }
        return false;
    }

    public Generateur getGenerateur(String g) {
        for(Generateur generateur : generateurs){
            if(generateur.getNom().equals(g)){
                return generateur;
            }
        }
        return null;
    }

    public Maison getMaison(String maison) {
        for(Maison m : connexions.keySet()){
            if(m.getNom().equals(maison)){
                return m;
            }
        }
        return null;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<Maison, Generateur> e : connexions.entrySet()){
            if(e.getValue()!=null) {
                sb.append(e.getKey().getNom());
                sb.append(" ----- ");
                sb.append(e.getValue().getNom());
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public Set<Maison> getMaisons() {
        return connexions.keySet();
    }

    public Set<Maison> getMaisons(Generateur g) {
        Set<Maison> maisons = new HashSet<Maison>();
        for(Maison m : connexions.keySet()){
            if(connexions.get(m) == g){
                maisons.add(m);
            }
        }
        return maisons;
    }

    public void solveurNaif(){
        if (getMaisons().isEmpty() || generateurs.isEmpty()) {
            return;
        }
        int i=0, k=10000;

        List<Maison> maisons = new ArrayList<>(getMaisons());
        Random rand = new Random();

        while(i < k){
            Maison maison = maisons.get(rand.nextInt(maisons.size()));
            Generateur newGenerateur = generateurs.get(rand.nextInt(generateurs.size()));
            Generateur oldGenerateur = connexions.get(maison);

            calculCout();
            double oldCost = getCout();

            if (oldGenerateur != null) {
                changeConnexion(maison, oldGenerateur, newGenerateur);
            } else {
                addConnexion(maison, newGenerateur);
            }

            calculCout();
            double newCost = getCout();

            if (newCost >= oldCost) {
                if (oldGenerateur != null) {
                    changeConnexion(maison, newGenerateur, oldGenerateur);
                } else {
                    supprConnexion(maison, newGenerateur);
                }
            }
            i++;
        }
    }

    public HashMap<Maison,Generateur> getConnexions() {
        return connexions;
    }

    public double getCout() {
        return cout;
    }

    public double getTauxUtilisationMoyen() {
        return tauxUtilisationMoyen;
    }

    public double getDisp() {
        return disp;
    }

    public double getPenalite() {
        return penalite;
    }

    public double getSurcharge() {
        return surcharge;
    }

    public int getCharge() {
        return charge;
    }

    public int getCapacite() {
        return capacite;
    }

    public List<Generateur> getGenerateurs() {
        return generateurs;
    }

    public String getNom() {
        return nom;
    }

}
