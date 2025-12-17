package reseau;

import UI.view.TerminalView;

import java.util.*;
import java.util.concurrent.*;

/**
 * Optimisation avancée du réseau électrique par algorithme hybride.
 * <p>
 * Combine plusieurs techniques d'optimisation de pointe :
 * <ul>
 * <li>Construction gloutonne avec heuristique MRV</li>
 * <li>Recherche Locale Itérée (ILS) avec perturbations</li>
 * <li>Recuit simulé adaptatif avec réchauffe automatique</li>
 * <li>Mouvements complexes (déplacement + swap)</li>
 * <li>Descente locale pour convergence finale</li>
 * </ul>
 *
 * @author Votre nom
 * @version 2.1 - Optimisations avancées + corrections
 */
public class Optimisation {

    private static final int NB_RESTARTS = 5;
    private static final double TEMPERATURE_INITIALE = 1000.0;
    private static final double TEMPERATURE_MIN = 0.001;
    private static final int MAX_ITERATIONS_RECUIT = 50000; // Limite de sécurité
    private static final int MAX_ITERATIONS_DESCENTE = 1000;
    private static final int TAILLE_FENETRE_ADAPTATION = 100;
    private static final int SEUIL_RECHAUFFE = 800;
    private static final int MAX_RECHAUFFES = 3; // Limite le nombre de réchauffes
    private static final double PROPORTION_PERTURBATION = 0.3;
    private static final double PROBABILITE_SWAP = 0.3;

    private static Random random = new Random();

    /**
     * Optimise la distribution électrique par Recherche Locale Itérée.
     * <p>
     * Utilise ILS (Iterated Local Search) : au lieu de restarts indépendants,
     * perturbe la meilleure solution trouvée pour explorer de nouvelles régions
     * tout en conservant les bonnes structures découvertes.
     *
     * @param reseau le réseau à optimiser
     * @return le coût final de la meilleure solution trouvée
     */
    public static double optimiser(Reseau reseau) {
        System.out.println("\n=== RESOLUTION AUTOMATIQUE AVANCEE V2.0 ===");
        System.out.println("Algorithme : ILS + Recuit Adaptatif + Reheating + Mouvements Complexes\n");

        long debutTotal = System.currentTimeMillis();

        // Solution initiale
        System.out.println("--- Initialisation ---");
        construireSolutionInitiale(reseau);
        System.out.println("Construction initiale terminée");

        recuitSimuleAdaptatif(reseau);
        descenteLocale(reseau);
        reseau.calculCout();

        double meilleurCoutGlobal = reseau.getCout();
        Map<Maison, Generateur> meilleureSolutionGlobale = new HashMap<>(reseau.getConnexions());
        System.out.printf("Solution initiale optimisée : %.3f%n%n", meilleurCoutGlobal);

        // ILS : Perturbation + Optimisation itérées
        for (int restart = 1; restart < NB_RESTARTS; restart++) {
            System.out.printf("--- ILS Itération %d/%d ---%n", restart + 1, NB_RESTARTS);

            // Restaure la meilleure solution et la perturbe
            restaurerSolution(meilleureSolutionGlobale, reseau);
            perturbationForte(reseau, PROPORTION_PERTURBATION);
            double coutApresPerturbation = reseau.getCout();
            System.out.printf("Après perturbation : %.3f%n", coutApresPerturbation);

            // Réoptimise
            recuitSimuleAdaptatif(reseau);
            reseau.calculCout();
            double coutApresRecuit = reseau.getCout();
            System.out.printf("Après recuit       : %.3f%n", coutApresRecuit);

            descenteLocale(reseau);
            reseau.calculCout();
            double coutFinal = reseau.getCout();
            System.out.printf("Après descente     : %.3f%n", coutFinal);

            // Critère d'acceptation ILS
            if (coutFinal < meilleurCoutGlobal) {
                meilleurCoutGlobal = coutFinal;
                meilleureSolutionGlobale = new HashMap<>(reseau.getConnexions());
                System.out.println("✓ Nouvelle meilleure solution !");
            } else {
                System.out.println("✗ Pas d'amélioration, retour à la meilleure");
            }
            System.out.println();
        }

        restaurerSolution(meilleureSolutionGlobale, reseau);
        reseau.calculCout();

        long tempsTotal = System.currentTimeMillis() - debutTotal;

        System.out.println("=== RESULTAT FINAL ===");
        System.out.printf("Meilleur coût trouvé : %.3f%n", meilleurCoutGlobal);
        System.out.printf("Temps total          : %dms%n", tempsTotal);
        System.out.println();

        return meilleurCoutGlobal;
    }

    /**
     * Construit une solution initiale par distribution gloutonne.
     * <p>
     * Trie les maisons par consommation décroissante (heuristique MRV)
     * et assigne chaque maison au générateur minimisant le déséquilibre.
     *
     * @param reseau le réseau à initialiser
     */
    private static void construireSolutionInitiale(Reseau reseau) {
        List<Maison> maisons = new ArrayList<>(reseau.getMaisons());
        maisons.sort((m1, m2) -> Integer.compare(m2.getConsommation(), m1.getConsommation()));

        // Réinitialiser toutes les connexions
        Map<Maison, Generateur> connexionsCopie = new HashMap<>(reseau.getConnexions());
        for (Map.Entry<Maison, Generateur> entry : connexionsCopie.entrySet()) {
            if (entry.getValue() != null) {
                reseau.supprConnexion(entry.getKey(), entry.getValue());
            }
        }

        // Distribuer toutes les maisons
        for (Maison m : maisons) {
            Generateur meilleur = trouverMeilleurGenerateur(m, reseau);
            if (meilleur != null) {
                reseau.addConnexion(m, meilleur);
            }
        }
    }

    /**
     * Sélectionne le générateur optimal pour une maison donnée.
     * <p>
     * Calcule un score privilégiant les générateurs sous-utilisés
     * tout en pénalisant fortement les risques de surcharge.
     *
     * @param m la maison à connecter
     * @param reseau le réseau
     * @return le générateur optimal, ou null si aucun n'est disponible
     */
    private static Generateur trouverMeilleurGenerateur(Maison m, Reseau reseau) {
        List<Generateur> generateurs = reseau.getGenerateurs();
        Generateur meilleur = null;
        double meilleurScore = Double.NEGATIVE_INFINITY;

        for (Generateur g : generateurs) {
            int charge = g.getChargeActuelle();
            double capaciteRestante = g.getCapacite() - charge;
            double taux = (double) charge / g.getCapacite();
            double score = capaciteRestante * (1.0 - taux);

            if (capaciteRestante < m.getConsommation()) {
                score -= 100000;
            }

            if (score > meilleurScore) {
                meilleurScore = score;
                meilleur = g;
            }
        }

        return meilleur;
    }

    /**
     * Applique le recuit simulé avec refroidissement adaptatif et réchauffe.
     * <p>
     * Ajuste dynamiquement la vitesse de refroidissement selon le taux
     * d'acceptation observé. Réchauffe automatiquement si l'algorithme stagne.
     * Combine mouvements simples (déplacement) et complexes (swap).
     *
     * @param reseau le réseau à optimiser
     */
    private static void recuitSimuleAdaptatif(Reseau reseau) {
        List<Maison> maisons = new ArrayList<>(reseau.getMaisons());
        List<Generateur> generateurs = reseau.getGenerateurs();

        double temperature = TEMPERATURE_INITIALE;
        int iterationsParTemp = Math.max(maisons.size() * 2, 20); // Réduit pour éviter trop d'itérations

        int iterations = 0;
        int acceptations = 0;
        int ameliorations = 0;
        int acceptationsFenetre = 0;
        int iterationsSansAmelioration = 0;
        double meilleurCout = reseau.getCout();
        int nombreRechauffes = 0;

        while (temperature > TEMPERATURE_MIN && iterations < MAX_ITERATIONS_RECUIT) {
            for (int i = 0; i < iterationsParTemp && iterations < MAX_ITERATIONS_RECUIT; i++) {
                iterations++;
                boolean accepte = false;

                // 30% du temps : tentative de swap, sinon déplacement simple
                if (random.nextDouble() < PROBABILITE_SWAP && maisons.size() > 1) {
                    accepte = tentativeSwap(reseau, temperature);
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

                // Adaptation du taux de refroidissement
                if (iterations % TAILLE_FENETRE_ADAPTATION == 0 && acceptationsFenetre > 0) {
                    double tauxAcceptation = acceptationsFenetre / (double) TAILLE_FENETRE_ADAPTATION;

                    if (tauxAcceptation > 0.85) {
                        temperature *= 0.95; // Refroidissement rapide
                    } else if (tauxAcceptation < 0.15) {
                        temperature *= 0.985; // Refroidissement modéré (pas trop lent)
                    } else {
                        temperature *= 0.97; // Normal
                    }

                    acceptationsFenetre = 0;
                } else if (iterations % TAILLE_FENETRE_ADAPTATION == 0) {
                    // Si aucune acceptation, refroidir normalement
                    temperature *= 0.97;
                    acceptationsFenetre = 0;
                }

                // Réchauffe si stagnation (limité)
                if (iterationsSansAmelioration > SEUIL_RECHAUFFE && nombreRechauffes < MAX_RECHAUFFES) {
                    temperature = Math.min(temperature * 15, TEMPERATURE_INITIALE * 0.4);
                    iterationsSansAmelioration = 0;
                    nombreRechauffes++;
                }
            }

            // Refroidissement par défaut si pas d'adaptation
            if (iterations % TAILLE_FENETRE_ADAPTATION != 0) {
                temperature *= 0.97;
            }
        }

        if (iterations >= MAX_ITERATIONS_RECUIT) {
            System.out.println("  (Limite d'itérations atteinte)");
        }

        System.out.printf("  Recuit : %d iterations | %d acceptations (%.1f%%) | %d ameliorations | %d rechauffes%n",
                iterations, acceptations,
                iterations > 0 ? 100.0 * acceptations / iterations : 0.0,
                ameliorations, nombreRechauffes);
    }

    /**
     * Effectue une tentative de déplacement simple d'une maison.
     *
     * @param reseau le réseau
     * @param maisons liste des maisons
     * @param generateurs liste des générateurs
     * @param temperature température actuelle
     * @return true si le mouvement a été accepté
     */
    private static boolean tentativeDeplacement(Reseau reseau, List<Maison> maisons,
                                                List<Generateur> generateurs, double temperature) {
        if (maisons.isEmpty() || generateurs.isEmpty()) return false;

        Maison m = choisirMaisonIntelligente(maisons, reseau);
        Generateur gNouveau = choisirGenerateurIntelligent(m, generateurs, reseau);
        Generateur gActuel = reseau.getConnexions().get(m);

        if (gActuel == null || gActuel == gNouveau) return false;

        double coutAvant = reseau.getCout();
        reseau.changeConnexion(m, gActuel, gNouveau);
        reseau.calculCout();
        double coutApres = reseau.getCout();
        double delta = coutApres - coutAvant;

        if (delta < 0 || Math.exp(-delta / temperature) > random.nextDouble()) {
            return true;
        } else {
            reseau.changeConnexion(m, gNouveau, gActuel);
            reseau.calculCout();
            return false;
        }
    }

    /**
     * Effectue une tentative de swap (échange) entre deux maisons.
     * <p>
     * Échange les générateurs de deux maisons prises au hasard.
     * Ce mouvement permet d'explorer des transitions impossibles
     * avec des déplacements simples.
     *
     * @param reseau le réseau
     * @param temperature température actuelle
     * @return true si le swap a été accepté
     */
    private static boolean tentativeSwap(Reseau reseau, double temperature) {
        List<Maison> maisons = new ArrayList<>(reseau.getMaisons());

        if (maisons.size() < 2) return false;

        Maison m1 = maisons.get(random.nextInt(maisons.size()));
        Maison m2 = maisons.get(random.nextInt(maisons.size()));

        Generateur g1 = reseau.getConnexions().get(m1);
        Generateur g2 = reseau.getConnexions().get(m2);

        if (g1 == null || g2 == null || g1 == g2 || m1 == m2) return false;

        double coutAvant = reseau.getCout();

        // Swap
        reseau.changeConnexion(m1, g1, g2);
        reseau.changeConnexion(m2, g2, g1);
        reseau.calculCout();

        double coutApres = reseau.getCout();
        double delta = coutApres - coutAvant;

        if (delta < 0 || Math.exp(-delta / temperature) > random.nextDouble()) {
            return true; // Accepté
        } else {
            // Annule le swap
            reseau.changeConnexion(m1, g2, g1);
            reseau.changeConnexion(m2, g1, g2);
            reseau.calculCout();
            return false;
        }
    }

    /**
     * Sélectionne une maison prioritaire pour déplacement.
     * <p>
     * Privilégie les maisons connectées à des générateurs déséquilibrés
     * ou surchargés (70% du temps). Effectue une sélection aléatoire
     * pour diversification (30% du temps).
     *
     * @param maisons la liste des maisons disponibles
     * @param reseau le réseau
     * @return la maison sélectionnée
     */
    private static Maison choisirMaisonIntelligente(List<Maison> maisons, Reseau reseau) {
        if (maisons.isEmpty()) return null;

        List<Maison> candidatsPrioritaires = new ArrayList<>();
        double tauxMoyen = reseau.getTauxUtilisationMoyen();

        for (Maison m : maisons) {
            Generateur g = reseau.getConnexions().get(m);
            if (g == null) continue;

            double taux = g.calculTauxUtilisation();

            if (Math.abs(taux - tauxMoyen) > 0.15 || taux > 1.0) {
                candidatsPrioritaires.add(m);
            }
        }

        if (!candidatsPrioritaires.isEmpty() && random.nextDouble() < 0.7) {
            return candidatsPrioritaires.get(random.nextInt(candidatsPrioritaires.size()));
        }

        return maisons.get(random.nextInt(maisons.size()));
    }

    /**
     * Sélectionne un générateur cible pour une maison.
     * <p>
     * Score les générateurs selon leur proximité au taux moyen d'utilisation,
     * avec bonus pour les sous-utilisés et pénalité pour risque de surcharge.
     * Sélection probabiliste parmi les meilleurs candidats.
     *
     * @param m la maison à déplacer
     * @param generateurs la liste des générateurs disponibles
     * @param reseau le réseau
     * @return le générateur sélectionné
     */
    private static Generateur choisirGenerateurIntelligent(Maison m, List<Generateur> generateurs, Reseau reseau) {
        if (generateurs.isEmpty()) return null;

        double tauxMoyen = reseau.getTauxUtilisationMoyen();
        List<GenerateurScore> scores = new ArrayList<>();

        for (Generateur g : generateurs) {
            double taux = g.calculTauxUtilisation();
            int charge = g.getChargeActuelle();

            double score = -Math.abs(taux - tauxMoyen);

            if (taux < tauxMoyen) {
                score += 0.5;
            }

            if (charge + m.getConsommation() > g.getCapacite()) {
                score -= 10.0;
            }

            scores.add(new GenerateurScore(g, score));
        }

        scores.sort((a, b) -> Double.compare(b.score, a.score));

        if (!scores.isEmpty() && random.nextDouble() < 0.8) {
            int maxIdx = Math.min(3, scores.size());
            int idx = random.nextInt(maxIdx);
            return scores.get(idx).generateur;
        }

        return generateurs.get(random.nextInt(generateurs.size()));
    }

    /**
     * Classe auxiliaire pour associer un générateur à son score.
     */
    private static class GenerateurScore {
        Generateur generateur;
        double score;

        GenerateurScore(Generateur g, double s) {
            this.generateur = g;
            this.score = s;
        }
    }

    /**
     * Applique une perturbation forte à la solution actuelle.
     * <p>
     * Déplace aléatoirement un certain pourcentage de maisons vers
     * d'autres générateurs. Utilisé dans ILS pour explorer de nouvelles
     * régions tout en conservant une partie de la structure.
     *
     * @param reseau le réseau à perturber
     * @param proportion proportion de maisons à déplacer (entre 0 et 1)
     */
    private static void perturbationForte(Reseau reseau, double proportion) {
        List<Maison> maisons = new ArrayList<>(reseau.getMaisons());
        List<Generateur> generateurs = reseau.getGenerateurs();
        Collections.shuffle(maisons, random);

        int nbDeplacements = Math.max(1, (int) (maisons.size() * proportion));

        for (int i = 0; i < nbDeplacements; i++) {
            Maison m = maisons.get(i);
            Generateur actuel = reseau.getConnexions().get(m);

            if (actuel == null) continue;

            // Choisit un générateur différent aléatoirement
            List<Generateur> autresGenerateurs = new ArrayList<>(generateurs);
            autresGenerateurs.remove(actuel);

            if (!autresGenerateurs.isEmpty()) {
                Generateur nouveau = autresGenerateurs.get(random.nextInt(autresGenerateurs.size()));
                reseau.changeConnexion(m, actuel, nouveau);
            }
        }

        reseau.calculCout();
    }

    /**
     * Applique une descente locale pure jusqu'à convergence.
     * <p>
     * Accepte uniquement les mouvements strictement améliorants.
     * S'arrête quand aucune amélioration n'est possible ou après
     * un nombre maximal d'itérations.
     *
     * @param reseau le réseau à optimiser
     */
    private static void descenteLocale(Reseau reseau) {
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
                        gActuel = gNouveau;
                    } else {
                        reseau.changeConnexion(m, gNouveau, gActuel);
                        reseau.calculCout();
                    }
                }
            }
        }

        System.out.printf("  Descente : %d iterations | %d ameliorations%n",
                iterations, ameliorationsTotales);
    }

    /**
     * Restaure une solution sauvegardée dans le réseau.
     *
     * @param solution la map maison-générateur à restaurer
     * @param reseau le réseau cible
     */
    private static void restaurerSolution(Map<Maison, Generateur> solution, Reseau reseau) {
        if (solution == null) return;

        for (Map.Entry<Maison, Generateur> entry : solution.entrySet()) {
            Generateur actuel = reseau.getConnexions().get(entry.getKey());
            Generateur cible = entry.getValue();

            if (actuel != cible && actuel != null && cible != null) {
                reseau.changeConnexion(entry.getKey(), actuel, cible);
            }
        }
    }

    /**
     * Affiche un rapport détaillé du réseau optimisé.
     * <p>
     * Présente pour chaque générateur : charge, taux d'utilisation,
     * maisons connectées. Affiche ensuite les statistiques globales
     * et le coût total final.
     *
     * @param reseau le réseau à afficher
     */
    public static void afficherDetails(Reseau reseau) {
        System.out.println("=== DETAILS DU RESEAU OPTIMISE ===\n");

        List<String> nomsGen = new ArrayList<>();
        for (Generateur g : reseau.getGenerateurs()) {
            nomsGen.add(g.getNom());
        }
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
            System.out.printf("  Charge : %d/%dkW | Taux : %.3f%n", charge, g.getCapacite(), taux);
            System.out.println("  Maisons : " +
                    (infosMaisons.isEmpty() ? "(aucune)" : String.join(", ", infosMaisons)));
            if (taux > 1.0) System.out.println("  /!\\ SURCHARGE");
            System.out.println();
        }

        reseau.calculCout();
        System.out.println("--- STATISTIQUES GLOBALES ---");
        System.out.printf("Taux moyen      : %.5f%n", reseau.getTauxUtilisationMoyen());
        System.out.printf("Dispersion      : %.5f%n", reseau.getDisp());
        System.out.printf("Surcharge       : %.5f%n", reseau.getSurcharge());
        System.out.printf("Penalite lambda : %.5f%n", reseau.getPenalite());
        System.out.printf("COUT TOTAL      : %.5f%n", reseau.getCout());
        System.out.println();
    }
}
