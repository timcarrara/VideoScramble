/**
 * Recherche de clés par force brute
 * Auteurs : BONNIN Simon, CARRARA Tim
 * Groupe  : S5 - A2
 * Date    : Décembre 2024
 * Description : Cette classe implémente une attaque par force brute pour trouver
 * les clés de déchiffrement d'une image cryptée.
 * L'algorithme teste toutes les combinaisons possibles de clés (r, s) en utilisant
 * le multithreading pour accélérer le processus.
 */

package org.example;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.example.EvaluationScoreImage.evaluateKeyScore;
import static org.example.PermutationLignes.unscrambleBlockRowsFast;

/**
 * Classe fournissant des méthodes pour retrouver les clés de chiffrement
 * d'une image par une attaque de force brute parallélisée.
 * L'algorithme teste systématiquement toutes les combinaisons possibles de clés
 * (256 valeurs pour r × 128 valeurs pour s = 32 768 clés) en utilisant tous
 * les cœurs du processeur disponibles pour accélérer le calcul.
 *
 * @author BONNIN Simon, CARRARA Tim
 * @version 1.0
 */
public class TrouveCleBruteForce {

    /**
     * Classe interne pour construire dynamiquement un tableau d'entiers.
     * Permet d'ajouter des éléments sans connaître la taille finale à l'avance,
     * avec redimensionnement automatique.
     */
    private static class IntArrayListBuilder {
        // Tableau interne de stockage redimensionné au besoin
        private int[] data = new int[16];
        // Nombre d'éléments actuellement stockées
        private int size = 0;

        /**
         * Ajoute un entier au tableau.
         * Double la capacité si le tableau est plein.
         * @param v La valeur à ajouter
         */
        void add(int v) {
            if (size == data.length) {
                int[] n = new int[data.length * 2];
                // On copie tous les éléments existants
                System.arraycopy(data, 0, n, 0, data.length);
                // Remplace l'ancien tableau
                data = n;
            }
            data[size++] = v;
        }

        /**
         * Convertit en tableau d'entiers de taille exacte.
         * @return Un tableau contenant exactement les éléments ajoutés
         */
        int[] toArray() {
            int[] out = new int[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }
    }

    /**
     * Classe interne représentant le résultat d'une recherche de clé.
     * Contient les valeurs des clés et le score associé.
     */
    private static class Resultat {
        final int r;
        final int s;
        final double score;

        /**
         * Construit un résultat avec les clés et le score.
         * @param r     Première clé
         * @param s     Seconde clé
         * @param score Score de qualité du déchiffrement
         */
        Resultat(int r, int s, double score) {
            this.r = r;
            this.s = s;
            this.score = score;
        }
    }

    /**
     * Trouve les clés de déchiffrement par force brute parallélisée.
     * Cette méthode teste toutes les combinaisons possibles de clés (r, s)
     * soit 32 768 combinaisons au total.
     * Le travail est réparti entre tous les cœurs du processeur disponibles pour accélérer le calcul.
     * Principe de l'attaque :
     * 1. Pour chaque paire de clés (r, s), déchiffrer l'image
     * 2. Calculer la corrélation de Pearson entre lignes adjacentes
     * 3. La bonne clé produit l'image avec la plus forte corrélation
     *
     * @param imageCryptee L'image chiffrée à déchiffrer
     * @return Un tableau [r, s] contenant les clés trouvées, ou [0, 0] en cas d'erreur
     */
    public static int[] bruteForce(Mat imageCryptee) {
        final int TOTAL_CLES = 256 * 128;
        // Pourcentage de lignes à tester pour évaluer une clé
        final double LIGNES_POURCENTAGE_TEST = 0.55;
        // Nombre de cœurs CPU disponibles pour le multithreading
        final int nbCoeurs = Math.max(1, Runtime.getRuntime().availableProcessors());
        // Ensemble de threads pré-crée pour lancer les calculs en parallèle
        ExecutorService pool = Executors.newFixedThreadPool(nbCoeurs);


        int lignesImage = imageCryptee.rows();
        // Cas où l'image est trop petite
        if (lignesImage < 2) {
            return new int[]{0, 0};
        }

        final int octetsParLigne = imageCryptee.cols() * imageCryptee.channels();

        // Préchargement de toutes les lignes dans des tableaux de bytes
        // Évite les accès répétés à la structure Mat d'OpenCV (plus lent)
        final byte[][] lignesSource = new byte[lignesImage][];
        for (int i = 0; i < lignesImage; i++) {
            byte[] ligne = new byte[octetsParLigne];
            imageCryptee.row(i).get(0, 0, ligne);
            lignesSource[i] = ligne;
        }

        // Nombre total de paires de lignes adjacentes possibles
        int totalPaires = lignesImage - 1;
        // Nombre de paires possibles à tester
        int nbPaireTest = Math.max(1, (int) Math.round(totalPaires * LIGNES_POURCENTAGE_TEST));
        int pas = Math.max(1, totalPaires / nbPaireTest);

        // Construction du tableau des indices de lignes à échantillonner
        final int[] indicesEchantillon;
        {
            IntArrayListBuilder liste = new IntArrayListBuilder();
            for (int i = 0; i < totalPaires; i += pas) {
                liste.add(i);
            }
            indicesEchantillon = liste.toArray();
        }
        // Tous les threads peuvent lire/mettre à jour le meilleur score actuel
        AtomicReference<Double> scoreMeilleurCle = new AtomicReference<>(Double.NEGATIVE_INFINITY);

        // Taille de bloc de clés assigné à chaque thread
        // Exemple : 32768 clés / 8 coeurs = 4096 clés par thread
        int tailleBloc = (TOTAL_CLES + nbCoeurs - 1) / nbCoeurs;

        // Liste des tâches à exécuter en parallèle
        List<Callable<Resultat>> taches = new ArrayList<>();

        // Création d'une tâche par thread
        for (int t = 0; t < nbCoeurs; t++) {
            // Plage de clés que ce thread doit traiter
            final int cleDebut = t * tailleBloc;
            final int cleFin = Math.min(TOTAL_CLES, cleDebut + tailleBloc);

            // Définition de la tâche pour ce thread
            taches.add(() -> {
                byte[][] lignesDecryptees = new byte[lignesImage][];
                // Meilleur score trouvé par ce thread
                double meilleurScoreLocal = Double.NEGATIVE_INFINITY;
                int meilleurR = 0, meilleurS = 0;
                // Test de chaque clé dans la plage assignée
                for (int key = cleDebut; key < cleFin; key++) {
                    // Extraction des clés
                    int r = key & 0xFF;
                    int s = (key >> 8) & 0x7F;

                    // Déchiffrement rapide de l'image avec cette paire de clés
                    unscrambleBlockRowsFast(lignesDecryptees, lignesSource, 0, lignesImage, r, s);
                    // Calcul du score de Pearson
                    double score = evaluateKeyScore(lignesDecryptees, indicesEchantillon, scoreMeilleurCle.get());
                    // Mise à jour du meilleur score global et des clés
                    if (score > meilleurScoreLocal) {
                        meilleurScoreLocal = score;
                        meilleurR = r;
                        meilleurS = s;

                        Double scoreActuel;
                        do {
                            // Si un autre thread a trouvé mieux entre temps, on abandonne
                            scoreActuel = scoreMeilleurCle.get();
                            if (meilleurScoreLocal <= scoreActuel) break;
                        } while (!scoreMeilleurCle.compareAndSet(scoreActuel, meilleurScoreLocal));
                    }
                }

                return new Resultat(meilleurR, meilleurS, meilleurScoreLocal);
            });
        }
        try {
            // Lancement de toutes les tâches en parallèle
            List<Future<Resultat>> futures = pool.invokeAll(taches);

            // Récupération des meilleurs résultats de tous les threads
            double bestScore = Double.NEGATIVE_INFINITY;
            int bestR = 0, bestS = 0;
            // Récupération et comparaison des résultats de tous les threads
            for (Future<Resultat> f : futures) {
                Resultat r = f.get();
                if (r.score > bestScore) {
                    bestScore = r.score;
                    bestR = r.r;
                    bestS = r.s;
                }
            }
            // On arrête tout les threads
            pool.shutdownNow();
            // Retour de la meilleur clé trouvée
            return new int[]{bestR, bestS};
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            pool.shutdown();
            return new int[]{0, 0};
        }
    }
}
