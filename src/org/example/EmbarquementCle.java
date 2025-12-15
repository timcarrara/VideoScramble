package org.example;

import org.opencv.core.Mat;

/**
 * Classe pour embarquer et extraire une clé de chiffrement dans une image.
 * @author BONNIN Simon, CARRARA Tim
 * @version 1.0
 */
public class EmbarquementCle {

    /**
     * Embarque la clé (r, s) dans le pixel (0,0) de l'image.
     * Les 15 bits sont répartis en 3 groupes de 5 bits,
     * stockés dans les 5 bits de poids faible des 3 canaux (B, G, R) du pixel (0,0).
     * IMPORTANT : Cette méthode doit être appelée APRÈS le chiffrement de l'image.
     *
     * @param image L'image dans laquelle embarquer la clé (doit être chiffrée)
     * @param r Le décalage (offset) de la clé (0-255)
     * @param s Le pas (step) de la clé (0-127)
     */
    public static void cleDansPixel(Mat image, int r, int s) {
        // Combine r et s en un entier 15 bits : s dans les bits hauts (7-14), r dans les bits bas (0-7)
        int key = (s << 8) | r;

        // Récupération du pixel (0,0)
        double[] pixel = image.get(0, 0);
        int blue = (int) pixel[0];
        int green = (int) pixel[1];
        int red = (int) pixel[2];

        // Extraction des 5 bits pour chaque canal RGB
        int bitsRed = key & 0b11111;           // bits 0-4
        int bitsGreen = (key >> 5) & 0b11111;  // bits 5-9
        int bitsBlue = (key >> 10) & 0b11111;  // bits 10-14

        // Remplacement des 5 bits de poids faible dans chaque canal
        // On garde les 3 bits de poids fort intacts
        red = (red & 0b11100000) | bitsRed;
        green = (green & 0b11100000) | bitsGreen;
        blue = (blue & 0b11100000) | bitsBlue;

        // Réécriture du pixel modifié dans l'image
        image.put(0, 0, blue, green, red);

        System.out.println("Clé embarquée au pixel (0,0) : r=" + r + ", s=" + s);
    }

    /**
     * Extrait la clé (r, s) embarquée dans le pixel (0,0) de l'image.
     * @param image L'image chiffrée contenant la clé embarquée
     * @return Un tableau de deux entiers [r, s] représentant la clé
     */
    public static int[] extractionCleFromPixel(Mat image) {
        // Récupération du pixel (0,0)
        double[] pixel = image.get(0, 0);
        int blue = (int) pixel[0];
        int green = (int) pixel[1];
        int red = (int) pixel[2];

        // Récupération des 5 bits de poids faible dans chaque canal
        int bitsRed = red & 0b11111;
        int bitsGreen = green & 0b11111;
        int bitsBlue = blue & 0b11111;

        // Reconstruction de la clé complète 15 bits
        int key = (bitsBlue << 10) | (bitsGreen << 5) | bitsRed;

        // Extraction des deux composantes r et s
        int r = key & 0xFF;        // 8 bits de poids faible
        int s = (key >> 8) & 0x7F; // 7 bits suivants
        System.out.println("Clé extraite du pixel (0,0) : r=" + r + ", s=" + s);
        return new int[]{r, s};
    }

    /**
     * Vérifie si une clé extraite est valide.
     * @param key Tableau [r, s] à vérifier
     * @return true si la clé est valide (r entre 0-255, s entre 0-127)
     */
    public static boolean estUneCleValide(int[] key) {
        if (key == null || key.length != 2) {
            return false;
        }
        int r = key[0];
        int s = key[1];
        return (r >= 0 && r <= 255) && (s >= 0 && s <= 127);
    }
}
