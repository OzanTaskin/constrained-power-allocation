package menu;

import factory.ReseauFactory;
import reseau.Reseau;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class MainSansUI {
    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        Reseau r = new Reseau(10);
        /**
         *  Récupère les arguments mis en ligne de commande pour savoir si on passe directement au Menu3 ou d'abord par le Menu1
         */
        if(args.length > 0) {
            String fichier = args[0];
            try {
                if(args.length > 1) {
                    r = ReseauFactory.parserReseau(Double.parseDouble(args[1]), fichier);
                    System.out.println("Fichier " + fichier + " trouvé !");
                }else{
                    r = ReseauFactory.parserReseau(10, fichier);
                    System.out.println("Fichier " + fichier + " trouvé !");
                }
            } catch (FileNotFoundException e) {
                System.out.println("Fichier non trouvé.");
                Menu1.menu(sc, r);
                Menu2.menu(sc, r);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
            //Menu2.menu(sc,r);
            Menu3.menu(sc, r,fichier);
        }else{
            Menu1.menu(sc, r);
            Menu2.menu(sc, r);
        }
        sc.close();
    }
}
