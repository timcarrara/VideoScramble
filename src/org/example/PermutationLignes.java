/**
 * Chiffrement d'image par permutation de lignes
 * Auteurs : BONNIN Simon, CARRARA Tim
 * Groupe  : S5 - A2
 * Date    : Décembre 2025
 * Description : Cette classe implémente un algorithme de chiffrement d'image
 * basé sur la permutation des lignes. L'algorithme utilise une approche récursive
 * avec des puissances de deux pour mélanger et démélanger les lignes d'une image.
 * Deux clés (r et s) contrôlent la permutation utilisée.
 */

package org.example;

import org.opencv.core.Mat;

/**
 * Classe fournissant des méthodes pour chiffrer et déchiffrer des images
 * en utilisant une permutation contrôlée des lignes.
 * Le chiffrement se fait par permutation récursive des lignes de l'image
 * selon une formule mathématique dépendant de deux clés entières.
 *
 * @author BONNIN Simon, CARRARA Tim
 * @version 1.0
 */

public class PermutationLignes {

    /**
     * Chiffre une image en mélangeant ses lignes selon les clés fournies.
     * Cette méthode crée une copie de l'image originale et applique une permutation
     * de ses lignes basée sur les paramètres r et s.
     *
     * @param imageOrigine      L'image source à chiffrer (OpenCV Mat)
     * @param r                 Première clé de chiffrement (paramètre de décalage)
     * @param s                 Seconde clé de chiffrement (paramètre de multiplication)
     * @return Une nouvelle image avec les lignes permutées (chiffrée)
     */
    public static Mat scrambleFrame(Mat imageOrigine, int r, int s) {
        // Création d'une copie de l'image d'origine pour le chiffrement
        Mat imageCrypte = imageOrigine.clone();
        // Récupération de la hauteur de l'image en pixel
        int hauteurImage = imageOrigine.rows();
        // On mélange les différentes lignes de l'image
        scrambleBlockByPowerOfTwo(imageCrypte, imageOrigine, 0, hauteurImage, r, s);
        return imageCrypte;
    }

    /**
     * Mélange récursivement un bloc de lignes de l'image en utilisant des puissances de deux.
     * L'algorithme divise l'image en blocs de taille puissance de deux et applique
     * une permutation basée sur la formule : newIndex = (r + (2s+1) * i) mod p
     * où p est la plus grande puissance de 2 inférieure ou égale à la taille du bloc.
     *
     * @param imageCrypte       L'image destination recevant les lignes permutées
     * @param imageOrigine      L'image source contenant les lignes originales
     * @param debut             Index de début du bloc à traiter
     * @param fin               Index de fin du bloc à traiter (exclusif)
     * @param r                 Première clé de chiffrement (paramètre de décalage)
     * @param s                 Seconde clé de chiffrement (paramètre de multiplication)
     */
    public static void scrambleBlockByPowerOfTwo(Mat imageCrypte, Mat imageOrigine, int debut, int fin, int r, int s) {
        int tailleImage = fin - debut;
        // Si le bloc n’a qu’une ligne on ne mélange rien
        if (tailleImage <= 1) return;
        // On récupère la puissance max de deux inférieur ou égale a la taille du bloc
        int p = Integer.highestOneBit(tailleImage);
        // Mélange des lignes du bloc déterminé par p
        for (int i = 0; i < p; i++) {
            // Formule de la permutation
            int newIndex = (r + ((2 * s + 1) * i)) % p;
            newIndex += debut;
            // Copie de la ligne i vers sa nouvelle position mélangée
            imageOrigine.row(debut + i).copyTo(imageCrypte.row(newIndex));
        }
        // Appel récursif pour traiter le reste du bloc
        scrambleBlockByPowerOfTwo(imageCrypte, imageOrigine, debut + p, fin, r, s);
    }

    /**
     * Déchiffre une image en inversant la permutation des lignes.
     * Utilise les mêmes clés que pour le chiffrement pour retrouver l'ordre
     * original des lignes de l'image.
     *
     * @param imageCrypte       L'image chiffrée à déchiffrer
     * @param r                 Première clé de déchiffrement
     * @param s                 Seconde clé de déchiffrement
     * @return L'image déchiffrée avec les lignes dans leur ordre original
     */
    public static Mat unscrambleFrame(Mat imageCrypte, int r, int s) {
        // Création d'une copie de l'image chiffrée
        Mat imageDecrypte = imageCrypte.clone();
        // Appel de la méthode qui décrypte l'image
        unscrambleBlockByPowerOfTwo(imageDecrypte, imageCrypte, 0, imageCrypte.rows(), r, s);
        return imageDecrypte;
    }

    /**
     * Déchiffre récursivement un bloc de lignes en appliquant la permutation inverse.
     * Calcule la permutation inverse de celle utilisée au chiffrement pour replacer
     * chaque ligne à sa position d'origine.
     *
     * @param imageDecrypte     L'image destination recevant les lignes déchiffrées
     * @param imageCrypte       L'image source contenant les lignes chiffrées
     * @param debut             Index de début du bloc à traiter
     * @param fin               Index de fin du bloc à traiter (exclusif)
     * @param r                 Première clé de déchiffrement
     * @param s                 Seconde clé de déchiffrement
     */
    public static void unscrambleBlockByPowerOfTwo(Mat imageDecrypte, Mat imageCrypte, int debut, int fin, int r, int s) {
        int tailleImage = fin - debut;
        if (tailleImage <= 1) return;

        int p = Integer.highestOneBit(tailleImage);
        int[] inverse = new int[p];
        // Calcul de la permutation inverse : on réutilise la formule de chiffrement
        // pour retrouver où chaque ligne avait été envoyée
        for (int i = 0; i < p; i++) {
            int newIndex = (r + ((2 * s + 1) * i)) % p;
            inverse[newIndex] = i;
        }
        // Application de la permutation inverse
        for (int newIndex = 0; newIndex < p; newIndex++) {
            int oldIndex = inverse[newIndex];
            // On recopie chaque ligne de sa position brouillée vers sa position d'origine
            imageCrypte.row(debut + newIndex).copyTo(imageDecrypte.row(debut + oldIndex));
        }
        unscrambleBlockByPowerOfTwo(imageDecrypte, imageCrypte, debut + p, fin, r, s);
    }

    /**
     * Version optimisée du déchiffrement travaillant directement sur des tableaux de bytes.
     * Cette méthode est plus rapide que la version utilisant Mat car elle manipule
     * directement les lignes sous forme de tableaux de bytes pré-extraits.
     * Cela va nous servir pour tester rapidement de nombreuses clés.
     *
     * @param lignesDecryptees      Tableau destination pour les lignes déchiffrées
     * @param lignesCryptees        Tableau source contenant les lignes chiffrées
     * @param debut                 Index de début du bloc à traiter
     * @param fin                   Index de fin du bloc à traiter (exclusif)
     * @param r                     Première clé de déchiffrement
     * @param s                     Seconde clé de déchiffrement
     */
    public static void unscrambleBlockRowsFast(byte[][] lignesDecryptees, byte[][] lignesCryptees, int debut, int fin, int r, int s) {
        // Les lignes de mon image sont déja extraites
        int size = fin - debut;
        if (size <= 1) {
            if (size == 1) lignesDecryptees[debut] = lignesCryptees[debut];
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
            lignesDecryptees[debut + oldIndex] = lignesCryptees[debut + newIndex];
        }
        unscrambleBlockRowsFast(lignesDecryptees, lignesCryptees, debut + p, fin, r, s);
    }
}
