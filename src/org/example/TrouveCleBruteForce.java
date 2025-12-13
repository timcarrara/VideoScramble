package org.example;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.example.EvaluationScoreImage.evaluateKeyScore;
import static org.example.PermutationLignes.unscrambleBlockRowsFast;

public class TrouveCleBruteForce {

    private static class IntArrayListBuilder {
        private int[] data = new int[16];
        private int size = 0;
        void add(int v) {
            if (size == data.length) {
                int[] n = new int[data.length * 2];
                System.arraycopy(data, 0, n, 0, data.length);
                data = n;
            }
            data[size++] = v;
        }
        int[] toArray() {
            int[] out = new int[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }
    }

    private static class Result {
        final int r;
        final int s;
        final double score;

        Result(int r, int s, double score) {
            this.r = r;
            this.s = s;
            this.score = score;
        }
    }

    // Méthode Brute Force
    public static int[] bruteForce(Mat scrambled) {
        final int TOTAL_CLES = 256 * 128;
        // 55% des lignes seront testé pour scorer une clé
        final double LIGNES_POURCENTAGE_TEST = 0.55;
        // Nombre de threads
        final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        // Ensemble de threads pré-crée pour lancer les calculs en parallèle
        ExecutorService pool = Executors.newFixedThreadPool(cores);

        // Préparation de l'image
        int lignesImage = scrambled.rows();
        if (lignesImage < 2) {
            return new int[]{0, 0};
        }

        final int bytesParLigne = scrambled.cols() * scrambled.channels();

        // Préchargement des lignes dans des buffers
        final byte[][] srcRows = new byte[lignesImage][];
        for (int i = 0; i < lignesImage; i++) {
            byte[] row = new byte[bytesParLigne];
            scrambled.row(i).get(0, 0, row);
            srcRows[i] = row;
        }

        int totalPaires = lignesImage - 1;
        // Nombre de paires possibles à tester
        int nbPaireTest = Math.max(1, (int) Math.round(totalPaires * LIGNES_POURCENTAGE_TEST));
        int step = Math.max(1, totalPaires / nbPaireTest);
        final int[] sampleIndices;
        {
            IntArrayListBuilder b = new IntArrayListBuilder();
            for (int i = 0; i < totalPaires; i += step) {
                b.add(i);
            }
            sampleIndices = b.toArray();
        }
        // Tous les threads peuvent lire/mettre à jour le meilleur score actuel
        AtomicReference<Double> scoreMeilleurCle = new AtomicReference<>(Double.NEGATIVE_INFINITY);

        // Division du travail entre threads
        // Chaque thread traite une partie des clés
        int chunk = (TOTAL_CLES + cores - 1) / cores;
        List<Callable<Result>> tasks = new ArrayList<>();

        // Début et fin des clés que ce thread doit traiter
        for (int t = 0; t < cores; t++) {
            final int startKey = t * chunk;
            final int endKey = Math.min(TOTAL_CLES, startKey + chunk);
            // Création de la tâche multithread
            tasks.add(() -> {
                byte[][] dstRows = new byte[lignesImage][];
                // Meilleur score local au thread
                double bestScoreLocal = Double.NEGATIVE_INFINITY;
                int bestR = 0, bestS = 0;
                // Parcours des différentes clés
                for (int key = startKey; key < endKey; key++) {
                    // Extraction des clés
                    int r = key & 0xFF;
                    int s = (key >> 8) & 0x7F;

                    unscrambleBlockRowsFast(dstRows, srcRows, 0, lignesImage, r, s);
                    // Calcul du score de Pearson
                    double score = evaluateKeyScore(dstRows, sampleIndices, scoreMeilleurCle.get());
                    // Mise à jour du meilleur score global et des clés
                    if (score > bestScoreLocal) {
                        bestScoreLocal = score;
                        bestR = r;
                        bestS = s;

                        Double cur;
                        do {
                            cur = scoreMeilleurCle.get();
                            if (bestScoreLocal <= cur) break;
                        } while (!scoreMeilleurCle.compareAndSet(cur, bestScoreLocal));
                    }
                }

                return new Result(bestR, bestS, bestScoreLocal);
            });
        }
        try {
            List<Future<Result>> futures = pool.invokeAll(tasks);

            // Récupération des meilleurs résultats de tous les threads
            double bestScore = Double.NEGATIVE_INFINITY;
            int bestR = 0, bestS = 0;

            for (Future<Result> f : futures) {
                Result r = f.get();
                if (r.score > bestScore) {
                    bestScore = r.score;
                    bestR = r.r;
                    bestS = r.s;
                }
            }

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
