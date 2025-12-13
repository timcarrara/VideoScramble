package org.example;

public class PearsonCorrelation {

    // Corrélation de Pearson
    public static double pearson(byte[] ligne1, byte[] ligne2) {
        int n = ligne1.length;
        double moyenneLigne1 = 0, moyenneLigne2 = 0;

        for (int i = 0; i < n; i++) {
            // Conversion l'octet en entier (en pixel)
            moyenneLigne1 += (ligne1[i] & 0xFF);
            moyenneLigne2 += (ligne2[i] & 0xFF);
        }
        // Calcul des moyennes des deux tableaux
        moyenneLigne1 /= n;
        moyenneLigne2 /= n;

        double numerateur = 0, denominateurLigne1 = 0, denominateurLigne2 = 0;
        // Calcul du numérateur et des dénominateurs
        for (int i = 0; i < n; i++) {
            double da = (ligne1[i] & 0xFF) - moyenneLigne1;
            double db = (ligne2[i] & 0xFF) - moyenneLigne2;
            numerateur += da * db;
            denominateurLigne1 += da * da;
            denominateurLigne2 += db * db;
        }
        // Si variance est nulle, la corrélation n'est pas définissable
        if (denominateurLigne1 == 0 || denominateurLigne2 == 0) return -1;
        // Formule de la corrélation de Pearson
        return numerateur / Math.sqrt(denominateurLigne1 * denominateurLigne2);
    }
}
