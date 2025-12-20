package reseau;

import java.util.*;

/**
 * Optimisation avancée du réseau électrique par algorithme hybride.
 *
 * Pipeline :
 *  - Construction gloutonne (tri décroissant des consommations)
 *  - Recuit simulé adaptatif (fenêtres + reheating)
 *  - Descente locale (améliorations strictes)
 *  - ILS : perturbation + (recuit + descente), best-so-far
 */
public class Optimisation {

    private static final int NB_RESTARTS = 5;

    private static final double TEMPERATURE_INITIALE = 1000.0;
    private static final double TEMPERATURE_MIN = 0.001;

    private static final int MAX_ITERATIONS_RECUIT = 50_000;   // limite de sécurité
    private static final int MAX_ITERATIONS_DESCENTE = 1000;

    // Fenêtre d'adaptation : on fixe T, on fait W itérations, puis on ajuste T une seule fois.
    private static final int TAILLE_FENETRE_ADAPTATION = 100;

    private static final int SEUIL_RECHAUFFE = 800;
    private static final int MAX_RECHAUFFES = 3;

    private static final double PROPORTION_PERTURBATION = 0.3;
    private static final double PROBABILITE_SWAP = 0.3;

    private static final Random random = new Random();

    public static double optimiser(Reseau reseau) {
        System.out.println("\n=== RESOLUTION AUTOMATIQUE (ILS + Recuit + Descente) ===\n");
        long debutTotal = System.currentTimeMillis();

        // A) Solution initiale
        construireSolutionInitiale(reseau);
        reseau.calculCout();

        // B) Première optimisation
        recuitSimuleAdaptatif(reseau);
        descenteLocale(reseau);
        reseau.calculCout();

        double meilleurCoutGlobal = reseau.getCout();
        Map<Maison, Generateur> meilleureSolutionGlobale = new HashMap<>(reseau.getConnexions());
        System.out.printf("Solution initiale optimisée : %.6f%n%n", meilleurCoutGlobal);

        // D) ILS : perturbation + ré-optimisation
        for (int restart = 1; restart < NB_RESTARTS; restart++) {
            System.out.printf("--- ILS itération %d/%d ---%n", restart + 1, NB_RESTARTS);

            restaurerSolution(meilleureSolutionGlobale, reseau);
            perturbationForte(reseau, PROPORTION_PERTURBATION);
            System.out.printf("Après perturbation : %.6f%n", reseau.getCout());

            recuitSimuleAdaptatif(reseau);
            System.out.printf("Après recuit       : %.6f%n", reseau.getCout());

            descenteLocale(reseau);
            reseau.calculCout();
            double coutFinal = reseau.getCout();
            System.out.printf("Après descente     : %.6f%n", coutFinal);

            if (coutFinal < meilleurCoutGlobal) {
                meilleurCoutGlobal = coutFinal;
                meilleureSolutionGlobale = new HashMap<>(reseau.getConnexions());
                System.out.println("Nouvelle meilleure solution.");
            } else {
                System.out.println("Pas d'amélioration, retour à la meilleure.");
            }
            System.out.println();
        }

        // Restaurer best-so-far
        restaurerSolution(meilleureSolutionGlobale, reseau);
        reseau.calculCout();

        long tempsTotal = System.currentTimeMillis() - debutTotal;
        System.out.println("=== RESULTAT FINAL ===");
        System.out.printf("Meilleur coût trouvé : %.6f%n", meilleurCoutGlobal);
        System.out.printf("Temps total          : %d ms%n%n", tempsTotal);

        return meilleurCoutGlobal;
    }

    /**
     * Construction initiale gloutonne : maisons triées par consommation décroissante,
     * puis affectation au générateur maximisant un score (capacité restante, sous-utilisation, pénalité surcharge).
     */
    private static void construireSolutionInitiale(Reseau reseau) {
        List<Maison> maisons = new ArrayList<>(reseau.getMaisons());
        maisons.sort((m1, m2) -> Integer.compare(m2.getConsommation(), m1.getConsommation()));

        // Supprime toutes les connexions existantes
        Map<Maison, Generateur> copie = new HashMap<>(reseau.getConnexions());
        for (Map.Entry<Maison, Generateur> e : copie.entrySet()) {
            if (e.getValue() != null) {
                reseau.supprConnexion(e.getKey(), e.getValue());
            }
        }

        // Assigne toutes les maisons
        for (Maison m : maisons) {
            Generateur meilleur = trouverMeilleurGenerateur(m, reseau);
            if (meilleur != null) {
                reseau.addConnexion(m, meilleur);
            }
        }
        reseau.calculCout();
    }

    private static Generateur trouverMeilleurGenerateur(Maison m, Reseau reseau) {
        List<Generateur> generateurs = reseau.getGenerateurs();
        Generateur meilleur = null;
        double meilleurScore = Double.NEGATIVE_INFINITY;

        for (Generateur g : generateurs) {
            int charge = g.getChargeActuelle();
            double capaciteRestante = g.getCapacite() - charge;
            double taux = (double) charge / g.getCapacite();
            double score = capaciteRestante * (1.0 - taux);

            // pénalité massive si risque immédiat de dépassement
            if (capaciteRestante < m.getConsommation()) {
                score -= 100000.0;
            }

            if (score > meilleurScore) {
                meilleurScore = score;
                meilleur = g;
            }
        }

        return meilleur;
    }

    /**
     * Recuit simulé adaptatif :
     * - on effectue des fenêtres de W itérations à température courante,
     * - on ajuste T UNE FOIS par fenêtre selon le taux d'acceptation,
     * - reheating si stagnation prolongée (limité).
     */
    private static void recuitSimuleAdaptatif(Reseau reseau) {
        List<Maison> maisons = new ArrayList<>(reseau.getMaisons());
        List<Generateur> generateurs = reseau.getGenerateurs();
        if (maisons.isEmpty() || generateurs.isEmpty()) return;

        reseau.calculCout(); // garantit cout à jour au démarrage

        double temperature = TEMPERATURE_INITIALE;

        int iterations = 0;
        int acceptations = 0;
        int ameliorations = 0;

        int iterationsSansAmelioration = 0;
        double meilleurCout = reseau.getCout();
        int nombreRechauffes = 0;

        final int W = TAILLE_FENETRE_ADAPTATION;

        while (temperature > TEMPERATURE_MIN && iterations < MAX_ITERATIONS_RECUIT) {
            int acceptationsFenetre = 0;

            // Fenêtre à température "fixe"
            for (int k = 0; k < W && iterations < MAX_ITERATIONS_RECUIT; k++) {
                iterations++;

                boolean accepte;
                if (random.nextDouble() < PROBABILITE_SWAP && maisons.size() >= 2) {
                    accepte = tentativeSwap(reseau, maisons, temperature);
                } else {
                    accepte = tentativeDeplacement(reseau, maisons, generateurs, temperature);
                }

                if (accepte) {
                    acceptations++;
                    acceptationsFenetre++;

                    double coutActuel = reseau.getCout();
                    if (coutActuel < meilleurCout) {
                        meilleurCout = coutActuel;
                        ameliorations++;
                        iterationsSansAmelioration = 0;
                    } else {
                        iterationsSansAmelioration++;
                    }
                } else {
                    iterationsSansAmelioration++;
                }

                // Reheating si stagnation
                if (iterationsSansAmelioration > SEUIL_RECHAUFFE && nombreRechauffes < MAX_RECHAUFFES) {
                    temperature = Math.min(temperature * 15.0, TEMPERATURE_INITIALE * 0.4);
                    iterationsSansAmelioration = 0;
                    nombreRechauffes++;
                }
            }

            // Ajustement de T UNE FOIS par fenêtre
            double tauxAcceptation = acceptationsFenetre / (double) W;

            if (tauxAcceptation > 0.85) {
                temperature *= 0.95;
            } else if (tauxAcceptation < 0.15) {
                temperature *= 0.985;
            } else {
                temperature *= 0.97;
            }
        }

        if (iterations >= MAX_ITERATIONS_RECUIT) {
            System.out.println("  (Limite d'itérations atteinte)");
        }

        System.out.printf(
                "  Recuit : %d itérations | %d acceptations (%.1f%%) | %d améliorations | %d réchauffes%n",
                iterations, acceptations,
                iterations > 0 ? 100.0 * acceptations / iterations : 0.0,
                ameliorations, nombreRechauffes
        );
    }

    private static boolean tentativeDeplacement(Reseau reseau,
                                                List<Maison> maisons,
                                                List<Generateur> generateurs,
                                                double temperature) {
        if (maisons.isEmpty() || generateurs.isEmpty()) return false;

        Maison m = choisirMaisonIntelligente(maisons, reseau);
        if (m == null) return false;

        Generateur gNouveau = choisirGenerateurIntelligent(m, generateurs, reseau);
        if (gNouveau == null) return false;

        Generateur gActuel = reseau.getConnexions().get(m);
        if (gActuel == null || gActuel == gNouveau) return false;

        double coutAvant = reseau.getCout();

        reseau.changeConnexion(m, gActuel, gNouveau);
        reseau.calculCout();
        double coutApres = reseau.getCout();

        double delta = coutApres - coutAvant;

        if (delta < 0 || Math.exp(-delta / temperature) > random.nextDouble()) {
            return true;
        }

        // rollback
        reseau.changeConnexion(m, gNouveau, gActuel);
        reseau.calculCout();
        return false;
    }

    private static boolean tentativeSwap(Reseau reseau, List<Maison> maisons, double temperature) {
        if (maisons.size() < 2) return false;

        int i1 = random.nextInt(maisons.size());
        int i2 = random.nextInt(maisons.size());
        while (i2 == i1) i2 = random.nextInt(maisons.size());

        Maison m1 = maisons.get(i1);
        Maison m2 = maisons.get(i2);

        Generateur g1 = reseau.getConnexions().get(m1);
        Generateur g2 = reseau.getConnexions().get(m2);

        if (g1 == null || g2 == null || g1 == g2) return false;

        double coutAvant = reseau.getCout();

        // swap
        reseau.changeConnexion(m1, g1, g2);
        reseau.changeConnexion(m2, g2, g1);
        reseau.calculCout();

        double coutApres = reseau.getCout();
        double delta = coutApres - coutAvant;

        if (delta < 0 || Math.exp(-delta / temperature) > random.nextDouble()) {
            return true;
        }

        // rollback du swap
        reseau.changeConnexion(m1, g2, g1);
        reseau.changeConnexion(m2, g1, g2);
        reseau.calculCout();
        return false;
    }

    private static Maison choisirMaisonIntelligente(List<Maison> maisons, Reseau reseau) {
        if (maisons.isEmpty()) return null;

        List<Maison> prioritaires = new ArrayList<>();
        double tauxMoyen = reseau.getTauxUtilisationMoyen();

        for (Maison m : maisons) {
            Generateur g = reseau.getConnexions().get(m);
            if (g == null) continue;

            double taux = g.calculTauxUtilisation();
            if (Math.abs(taux - tauxMoyen) > 0.15 || taux > 1.0) {
                prioritaires.add(m);
            }
        }

        if (!prioritaires.isEmpty() && random.nextDouble() < 0.7) {
            return prioritaires.get(random.nextInt(prioritaires.size()));
        }

        return maisons.get(random.nextInt(maisons.size()));
    }

    private static Generateur choisirGenerateurIntelligent(Maison m, List<Generateur> generateurs, Reseau reseau) {
        if (generateurs.isEmpty()) return null;

        double tauxMoyen = reseau.getTauxUtilisationMoyen();
        List<GenerateurScore> scores = new ArrayList<>(generateurs.size());

        for (Generateur g : generateurs) {
            double taux = g.calculTauxUtilisation();
            int charge = g.getChargeActuelle();

            double score = -Math.abs(taux - tauxMoyen);

            if (taux < tauxMoyen) score += 0.5;

            if (charge + m.getConsommation() > g.getCapacite()) score -= 10.0;

            scores.add(new GenerateurScore(g, score));
        }

        scores.sort((a, b) -> Double.compare(b.score, a.score));

        if (random.nextDouble() < 0.8) {
            int maxIdx = Math.min(3, scores.size());
            return scores.get(random.nextInt(maxIdx)).generateur;
        }

        return generateurs.get(random.nextInt(generateurs.size()));
    }

    private static class GenerateurScore {
        final Generateur generateur;
        final double score;

        GenerateurScore(Generateur g, double s) {
            this.generateur = g;
            this.score = s;
        }
    }

    private static void perturbationForte(Reseau reseau, double proportion) {
        List<Maison> maisons = new ArrayList<>(reseau.getMaisons());
        List<Generateur> generateurs = reseau.getGenerateurs();
        if (maisons.isEmpty() || generateurs.isEmpty()) return;

        Collections.shuffle(maisons, random);

        int nbDeplacements = Math.max(1, (int) Math.floor(maisons.size() * proportion));

        for (int i = 0; i < nbDeplacements; i++) {
            Maison m = maisons.get(i);
            Generateur actuel = reseau.getConnexions().get(m);
            if (actuel == null) continue;

            List<Generateur> autres = new ArrayList<>(generateurs);
            autres.remove(actuel);

            if (!autres.isEmpty()) {
                Generateur nouveau = autres.get(random.nextInt(autres.size()));
                reseau.changeConnexion(m, actuel, nouveau);
            }
        }

        reseau.calculCout();
    }

    private static void descenteLocale(Reseau reseau) {
        reseau.calculCout();

        boolean amelioration = true;
        int iterations = 0;
        int ameliorationsTotales = 0;

        while (amelioration && iterations < MAX_ITERATIONS_DESCENTE) {
            amelioration = false;
            iterations++;

            List<Maison> maisons = new ArrayList<>(reseau.getMaisons());
            Collections.shuffle(maisons, random);

            for (Maison m : maisons) {
                Generateur gActuel = reseau.getConnexions().get(m);
                if (gActuel == null) continue;

                List<Generateur> generateurs = new ArrayList<>(reseau.getGenerateurs());
                Collections.shuffle(generateurs, random);

                for (Generateur gNouveau : generateurs) {
                    if (gActuel == gNouveau) continue;

                    double coutAvant = reseau.getCout();

                    reseau.changeConnexion(m, gActuel, gNouveau);
                    reseau.calculCout();
                    double coutApres = reseau.getCout();

                    if (coutApres < coutAvant) {
                        amelioration = true;
                        ameliorationsTotales++;
                        gActuel = gNouveau; // on conserve le changement
                    } else {
                        // rollback
                        reseau.changeConnexion(m, gNouveau, gActuel);
                        reseau.calculCout();
                    }
                }
            }
        }

        System.out.printf("  Descente : %d itérations | %d améliorations%n", iterations, ameliorationsTotales);
    }

    /**
     * Restauration plus robuste :
     *  - si actuel != cible : on replace (changeConnexion)
     *  - gère aussi les cas où une des deux connexions serait null (si ton modèle l'autorise)
     */
    private static void restaurerSolution(Map<Maison, Generateur> solution, Reseau reseau) {
        if (solution == null) return;

        for (Map.Entry<Maison, Generateur> e : solution.entrySet()) {
            Maison m = e.getKey();
            Generateur cible = e.getValue();
            Generateur actuel = reseau.getConnexions().get(m);

            if (actuel == cible) continue;

            if (actuel == null && cible != null) {
                reseau.addConnexion(m, cible);
            } else if (actuel != null && cible == null) {
                reseau.supprConnexion(m, actuel);
            } else if (actuel != null) {
                reseau.changeConnexion(m, actuel, cible);
            }
        }

        reseau.calculCout();
    }

    public static void afficherDetails(Reseau reseau) {
        System.out.println("=== DETAILS DU RESEAU OPTIMISE ===\n");

        List<String> nomsGen = new ArrayList<>();
        for (Generateur g : reseau.getGenerateurs()) nomsGen.add(g.getNom());
        Collections.sort(nomsGen);

        for (String nomGen : nomsGen) {
            Generateur g = reseau.getGenerateur(nomGen);
            Set<Maison> maisons = reseau.getMaisons(g);

            int charge = g.getChargeActuelle();

            List<String> infosMaisons = new ArrayList<>();
            for (Maison m : maisons) {
                infosMaisons.add(m.getNom() + "(" + m.getConsommation() + "kW)");
            }
            Collections.sort(infosMaisons);

            double taux = g.calculTauxUtilisation();

            System.out.printf("%s (capacite : %dkW)%n", g.getNom(), g.getCapacite());
            System.out.printf("  Charge : %d/%dkW | Taux : %.6f%n", charge, g.getCapacite(), taux);
            System.out.println("  Maisons : " + (infosMaisons.isEmpty() ? "(aucune)" : String.join(", ", infosMaisons)));
            if (taux > 1.0) System.out.println("  ATTENTION : SURCHARGE");
            System.out.println();
        }

        reseau.calculCout();
        System.out.println("--- STATISTIQUES GLOBALES ---");
        System.out.printf("Taux moyen      : %.6f%n", reseau.getTauxUtilisationMoyen());
        System.out.printf("Dispersion      : %.6f%n", reseau.getDisp());
        System.out.printf("Surcharge       : %.6f%n", reseau.getSurcharge());
        System.out.printf("Penalite lambda : %.6f%n", reseau.getPenalite());
        System.out.printf("COUT TOTAL      : %.6f%n", reseau.getCout());
        System.out.println();
    }
}
