/**
 * Calcul de corrélation de Pearson
 * Auteurs : BONNIN Simon, CARRARA Tim
 * Groupe  : S5 - A2
 * Date    : Décembre 2025
 * Description : Cette classe implémente le calcul du coefficient de corrélation
 * de Pearson entre deux lignes de pixels d'une image. Ce coefficient mesure
 * la similarité linéaire entre deux ensembles de données et est utilisé pour
 * évaluer la qualité du déchiffrement d'une image.
 */

package org.example;

/**
 * Classe fournissant une méthode pour calculer la corrélation de Pearson
 * entre des lignes de pixels d'image.
 * Le coefficient de Pearson mesure la corrélation linéaire entre deux variables.
 * Il varie entre -1 (corrélation négative parfaite) et +1 (corrélation positive parfaite),
 * avec 0 indiquant une absence de corrélation linéaire.
 *
 * @author BONNIN Simon, CARRARA Tim
 * @version 1.0
 */

public class PearsonCorrelation {

    /**
     * Calcule le coefficient de corrélation de Pearson entre deux lignes de pixels.
     * Un coefficient proche de +1 indique que les deux lignes sont très similaires,
     * ce qui suggère que l'image est correctement déchiffrée.
     *
     * @param ligne1    Premier tableau de pixels (valeurs en bytes de 0 à 255)
     * @param ligne2    Second tableau de pixels (valeurs en bytes de 0 à 255)
     * @return Le coefficient de corrélation de Pearson (valeur entre -1 et +1),
     *         ou -1 si la corrélation n'est pas calculable (variance nulle)
     */
    public static double pearson(byte[] ligne1, byte[] ligne2) {
        int nombrePixels = ligne1.length;
        double moyenneLigne1 = 0, moyenneLigne2 = 0;

        for (int i = 0; i < nombrePixels; i++) {
            // Conversion l'octet en entier (en pixel)
            moyenneLigne1 += (ligne1[i] & 0xFF);
            moyenneLigne2 += (ligne2[i] & 0xFF);
        }
        // Calcul des moyennes des deux tableaux
        moyenneLigne1 /= nombrePixels;
        moyenneLigne2 /= nombrePixels;

        double numerateur = 0, denominateurLigne1 = 0, denominateurLigne2 = 0;
        // Calcul du numérateur et des dénominateurs
        for (int i = 0; i < nombrePixels; i++) {
            double ecartLigne1 = (ligne1[i] & 0xFF) - moyenneLigne1;
            double ecartLigne2 = (ligne2[i] & 0xFF) - moyenneLigne2;
            numerateur += ecartLigne1 * ecartLigne2;
            denominateurLigne1 += ecartLigne1 * ecartLigne1;
            denominateurLigne2 += ecartLigne2 * ecartLigne2;
        }
        // Si variance est nulle, la corrélation n'est pas définissable
        if (denominateurLigne1 == 0 || denominateurLigne2 == 0) return -1;
        // Formule de la corrélation de Pearson
        return numerateur / Math.sqrt(denominateurLigne1 * denominateurLigne2);
    }
}
