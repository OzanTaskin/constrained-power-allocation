package reseau;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Tests pour la classe Reseau.
 */
class ReseauTest {

    /*
     * Crée un réseau initial pour les tests.
     */
    private Reseau creerReseauInitial() throws Exception {
        Reseau reseau = new Reseau( 50.0);
        reseau.addGenerateur(new Generateur("g1", 100));
        reseau.addGenerateur(new Generateur("g2", 50));
        reseau.addMaison(new Maison("m1", Consommation.NORMAL)); // 20
        reseau.addMaison(new Maison("m2", Consommation.BASSE));   // 10
        reseau.addMaison(new Maison("m3", Consommation.FORTE));  // 40
        return reseau;
    }

    /*
     * Teste la création d'un réseau.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/reseau_creation_tests.csv", numLinesToSkip = 1)
    void testCreationReseau(double penalite, int capaciteAttendue, int chargeAttendue) throws Exception {
        Reseau reseau = new Reseau(penalite);
        reseau.addGenerateur(new Generateur("g1", 100));
        reseau.addGenerateur(new Generateur("g2", 50));
        reseau.addMaison(new Maison("m1", Consommation.NORMAL)); // 20
        reseau.addMaison(new Maison("m2", Consommation.BASSE));   // 10
        reseau.addMaison(new Maison("m3", Consommation.FORTE));  // 40

        assertEquals(penalite, reseau.getPenalite());
        assertEquals(2, reseau.getGenerateurs().size());
        assertEquals(3, reseau.getMaisons().size());
        assertEquals(capaciteAttendue, reseau.getCapacite());
        assertEquals(chargeAttendue, reseau.getCharge());
    }

    /*
     * Teste l'ajout et la récupération d'une maison et d'un générateur.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/add_get_maison_generateur_tests.csv", numLinesToSkip = 1)
    void testAjoutEtRecuperationMaisonEtGenerateur(String nomMaison, String nomGenerateur, boolean maisonDevraitExister, boolean generateurDevraitExister) throws Exception {
        Reseau reseau = creerReseauInitial();
        assertEquals(maisonDevraitExister, reseau.maisonDansReseau(nomMaison));
        assertEquals(generateurDevraitExister, reseau.generateurDansReseau(nomGenerateur));
        if (maisonDevraitExister) {
            assertNotNull(reseau.getMaison(nomMaison));
        }
        if (generateurDevraitExister) {
            assertNotNull(reseau.getGenerateur(nomGenerateur));
        }
    }

    /*
     * Teste que l'ajout d'une maison lève une exception lorsque la capacité est dépassée.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/add_maison_capacity_tests.csv", numLinesToSkip = 1)
    void testAjoutMaisonLeveExceptionSiCapaciteDepassee(int capaciteGenerateur, String maisonsAAjouter, boolean doitLeverException) {
        Reseau petitReseau = new Reseau(10.0);
        petitReseau.addGenerateur(new Generateur("petitG", capaciteGenerateur));

        String[] maisons = maisonsAAjouter.split(",");

        if (doitLeverException) {
            Exception exceptionLevee = null;
            try {
                for (int i = 0; i < maisons.length; i++) {
                    petitReseau.addMaison(new Maison("m" + i, Consommation.valueOf(maisons[i])));
                }
            } catch (Exception e) {
                exceptionLevee = e;
            }
            assertNotNull(exceptionLevee);
        } else {
            assertDoesNotThrow(() -> {
                for (int i = 0; i < maisons.length; i++) {
                    petitReseau.addMaison(new Maison("m" + i, Consommation.valueOf(maisons[i])));
                }
            });
        }
    }

    /*
     * Teste l'ajout d'une connexion.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/connexion_tests.csv", numLinesToSkip = 1)
    void testAjoutConnexion(String nomMaison, String nomGenerateur, double chargeAttendue) throws Exception {
        Reseau reseau = creerReseauInitial();
        Maison maison = reseau.getMaison(nomMaison);
        Generateur generateur = reseau.getGenerateur(nomGenerateur);
        reseau.addConnexion(maison, generateur);

        assertEquals(generateur, reseau.getConnexions().get(maison));
        assertTrue(reseau.maisonConnecte(maison));
        assertEquals(chargeAttendue, generateur.calculTauxUtilisation() * generateur.getCapacite(), 0.001);
    }

    /*
     * Teste la suppression d'une connexion.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/suppr_connexion_tests.csv", numLinesToSkip = 1)
    void testSuppressionConnexion(String nomMaison, String nomGenerateur) throws Exception {
        Reseau reseau = creerReseauInitial();
        Maison maison = reseau.getMaison(nomMaison);
        Generateur generateur = reseau.getGenerateur(nomGenerateur);
        reseau.addConnexion(maison, generateur);
        assertTrue(reseau.maisonConnecte(maison));

        reseau.supprConnexion(maison, generateur);
        assertNull(reseau.getConnexions().get(maison));
        assertFalse(reseau.maisonConnecte(maison));
        assertEquals(0, generateur.calculTauxUtilisation() * generateur.getCapacite());
    }

    /*
     * Teste le changement d'une connexion.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/change_connexion_tests.csv", numLinesToSkip = 1)
    void testChangementConnexion(String nomMaison, String nomGenerateurSource, String nomGenerateurDestination) throws Exception {
        Reseau reseau = creerReseauInitial();
        Maison maison = reseau.getMaison(nomMaison);
        Generateur generateurSource = reseau.getGenerateur(nomGenerateurSource);
        Generateur generateurDestination = reseau.getGenerateur(nomGenerateurDestination);

        reseau.addConnexion(maison, generateurSource);
        assertEquals(maison.getConsommation(), generateurSource.calculTauxUtilisation() * generateurSource.getCapacite());
        assertEquals(0, generateurDestination.calculTauxUtilisation() * generateurDestination.getCapacite());

        reseau.changeConnexion(maison, generateurSource, generateurDestination);

        assertEquals(generateurDestination, reseau.getConnexions().get(maison));
        assertEquals(0, generateurSource.calculTauxUtilisation() * generateurSource.getCapacite());
        assertEquals(maison.getConsommation(), generateurDestination.calculTauxUtilisation() * generateurDestination.getCapacite());
    }

    /*
     * Teste le calcul du coût.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/calcul_cout_tests.csv", numLinesToSkip = 1)
    void testCalculCout(String connexions, double coutAttendu, double dispoAttendue, double surchargeAttendue) throws Exception {
        Reseau reseau = creerReseauInitial();
        String[] pairesConnexion = connexions.split(";");
        for (String paire : pairesConnexion) {
            String[] parties = paire.split(":");
            Maison maison = reseau.getMaison(parties[0]);
            Generateur generateur = reseau.getGenerateur(parties[1]);
            reseau.addConnexion(maison, generateur);
        }

        reseau.calculCout();

        assertEquals(coutAttendu, reseau.getCout(), 0.001);
        assertEquals(dispoAttendue, reseau.getDisp(), 0.001);
        assertEquals(surchargeAttendue, reseau.getSurcharge(), 0.001);
    }

    /*
     * Ce test vérifie que la création d'un générateur avec une capacité négative
     * lève bien une exception de type IllegalArgumentException.
     */
    @Test
    void testCreationGenerateurAvecCapaciteNegativeLeveException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Generateur("g_neg", -100);
        });

        String messageAttendu = "La capacité d'un générateur ne peut pas être négative.";
        String messageReel = exception.getMessage();

        assertTrue(messageReel.contains(messageAttendu));
    }
}
