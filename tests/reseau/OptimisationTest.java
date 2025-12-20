package reseau;

import factory.ReseauFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;

/*
Test des coûts optimaux des différentes instances données sur moodle avec un delta de 0,1
 */
class OptimisationTest {

    private Reseau createReseau(String fichier) throws IOException {
        Reseau reseau = ReseauFactory.parserReseau(10,fichier);
        return reseau;
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/instanceCoutOptimal.csv", numLinesToSkip = 1)
    void testInstances(String instance,double resultatAttendu) throws IOException {
        Reseau r = createReseau("./tests/resources/"+ instance +".txt");
        assertEquals(resultatAttendu, Optimisation.optimiser(r), 0.1);
    }
}
