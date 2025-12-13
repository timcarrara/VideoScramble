package org.example;

import org.opencv.core.Mat;

public class KeyEmbedder {

    /**
     * Embarque la clé (r, s) dans le pixel (0,0) de l'image.
     * r : 8 bits (0-255)
     * s : 7 bits (0-127)
     * Les 15 bits sont répartis en 3 groupes de 5 bits,
     * stockés dans les 3 canaux (B, G, R) du pixel (0,0).
     */
    public static void embedKeyInPixel(Mat image, int r, int s) {
        if (image == null || image.empty()) {
            throw new IllegalArgumentException("Image invalide");
        }
        if (r < 0 || r > 255 || s < 0 || s > 127) {
            throw new IllegalArgumentException("Clé r et s hors bornes");
        }

        // Combine r et s en un entier 15 bits : s dans les bits hauts, r dans les bits bas
        int key = (s << 8) | r;

        // Récupération du pixel (0,0)
        // Attention, OpenCV stocke les canaux dans l'ordre B, G, R
        double[] pixel = image.get(0, 0);

        int blue = (int) pixel[0];
        int green = (int) pixel[1];
        int red = (int) pixel[2];

        // Extraction des 5 bits pour chaque canal
        int bitsRed = key & 0b11111;            // bits 0-4
        int bitsGreen = (key >> 5) & 0b11111;   // bits 5-9
        int bitsBlue = (key >> 10) & 0b11111;   // bits 10-14

        // Remplacement des 5 bits de poids faible dans chaque canal (conserve les 3 bits hauts)
        red = (red & 0b11100000) | bitsRed;
        green = (green & 0b11100000) | bitsGreen;
        blue = (blue & 0b11100000) | bitsBlue;

        // Réécriture du pixel modifié dans l’image
        image.put(0, 0, blue, green, red);
    }

    /**
     * Extrait la clé (r, s) embarquée dans le pixel (0,0) de l'image.
     * Retourne un tableau [r, s].
     */
    public static int[] extractKeyFromPixel(Mat image) {
        if (image == null || image.empty()) {
            throw new IllegalArgumentException("Image invalide");
        }

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

        // Extraction des deux clés r et s
        int r = key & 0xFF;
        int s = (key >> 8) & 0x7F;

        return new int[]{r, s};
    }
}

