package org.example;

import org.opencv.core.Mat;

public class PermutationLignes {

    // Méthodes pour crypter l'image

    public static Mat scrambleFrame(Mat imageOrigine, int r, int s) {
        // Création d'une copie de l’image d’origine src
        Mat imageCrypte = imageOrigine.clone();
        // Récupération de la hauteur de l'image en pixel
        int hauteurImage = imageOrigine.rows();
        // On mélange les différentes lignes de l'image
        scrambleBlockByPowerOfTwo(imageCrypte, imageOrigine, 0, hauteurImage, r, s);
        return imageCrypte;
    }

    public static void scrambleBlockByPowerOfTwo(Mat imageCrypte, Mat imageOrigine, int debut, int fin, int r, int s) {
        // Si le bloc n’a qu’une ligne on ne mélange rien
        int tailleImage = fin - debut;
        if (tailleImage <= 1) return;
        // On récupère la puissance max de deux inférieur ou égale a la taille de l'image
        int p = Integer.highestOneBit(tailleImage);
        // Mélange des lignes du bloc déterminé par p
        for (int i = 0; i < p; i++) {
            // Formule de la permutation
            int newIndex = (r + ((2 * s + 1) * i)) % p;
            newIndex += debut;
            // Copie de la ligne i vers sa nouvelle position mélangée dans dst
            imageOrigine.row(debut + i).copyTo(imageCrypte.row(newIndex));
        }
        // Récursion pour le reste du bloc
        scrambleBlockByPowerOfTwo(imageCrypte, imageOrigine, debut + p, fin, r, s);
    }

    // Méthode pour décrypter l'image

    public static Mat unscrambleFrame(Mat imageCrypte, int r, int s) {
        // Création d'une copie de l'image cryptée
        Mat imageDecrypte = imageCrypte.clone();
        // Appel de la méthode qui décrypte l'image
        unscrambleBlockByPowerOfTwo(imageDecrypte, imageCrypte, 0, imageCrypte.rows(), r, s);
        return imageDecrypte;
    }

    // Décryptage de l'image avec clés connues
    public static void unscrambleBlockByPowerOfTwo(Mat imageDecrypte, Mat imageCrypte, int debut, int fin, int r, int s) {
        int tailleImage = fin - debut;
        if (tailleImage <= 1) return;

        int p = Integer.highestOneBit(tailleImage);
        int[] inverse = new int[p];
        // Réutilisation de la permutation utilisée au brouillage pour retrouver où chaque ligne avait été envoyée
        for (int i = 0; i < p; i++) {
            int newIndex = (r + ((2 * s + 1) * i)) % p;
            inverse[newIndex] = i;
        }
        // Application de la permutation inverse
        for (int newIndex = 0; newIndex < p; newIndex++) {
            int oldIndex = inverse[newIndex];
            // On recopie chaque ligne de sa position brouillée vers sa position d’origine
            imageCrypte.row(debut + newIndex).copyTo(imageDecrypte.row(debut + oldIndex));
        }
        unscrambleBlockByPowerOfTwo(imageDecrypte, imageCrypte, debut + p, fin, r, s);
    }

    public static void unscrambleBlockRowsFast(byte[][] dstRows, byte[][] srcRows, int start, int end, int r, int s) {
        // Les lignes de mon image sont déja extraites
        int size = end - start;
        if (size <= 1) {
            if (size == 1) dstRows[start] = srcRows[start];
            return;
        }
        int p = Integer.highestOneBit(size);

        int[] inverse = new int[p];
        for (int i = 0; i < p; i++) {
            int newIndex = (r + ((2 * s + 1) * i)) % p;
            inverse[newIndex] = i;
        }
        for (int newIndex = 0; newIndex < p; newIndex++) {
            int oldIndex = inverse[newIndex];
            dstRows[start + oldIndex] = srcRows[start + newIndex];
        }
        unscrambleBlockRowsFast(dstRows, srcRows, start + p, end, r, s);
    }
}
