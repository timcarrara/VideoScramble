package org.example;

import org.opencv.core.Mat;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.*;

/**
 * FrameScrambler : scramble / unscramble et brute-force (multithread)
 */
public class FrameScrambler {

    /**
     * Applique la permutation (scramble) selon r,s et blocs de puissances de 2
     */
    public static Mat scramble(Mat src, int r, int s) {
        Mat dst = src.clone();
        int height = src.rows();
        scrambleBlock(dst, src, 0, height, r, s);
        return dst;
    }

    /**
     * Mélange récursivement par blocs de tailles puissances de 2
     */
    private static void scrambleBlock(Mat dst, Mat src, int start, int end, int r, int s) {
        int size = end - start;
        if (size <= 1) return;

        int p = Integer.highestOneBit(size);

        for (int i = 0; i < p; i++) {
            int newIndex = (r + ((2 * s + 1) * i)) % p;
            newIndex += start;
            src.row(start + i).copyTo(dst.row(newIndex));
        }

        scrambleBlock(dst, src, start + p, end, r, s);
    }

    /**
     * Inverse la permutation — déchiffrement avec clé connue
     */
    public static Mat unscramble(Mat src, int r, int s) {
        Mat dst = src.clone();
        unscrambleBlock(dst, src, 0, src.rows(), r, s);
        return dst;
    }

    private static void unscrambleBlock(Mat dst, Mat src, int start, int end, int r, int s) {
        int size = end - start;
        if (size <= 1) return;

        int p = Integer.highestOneBit(size);
        int[] inverse = new int[p];

        for (int i = 0; i < p; i++) {
            int newIndex = (r + ((2 * s + 1) * i)) % p;
            inverse[newIndex] = i;
        }

        for (int newIndex = 0; newIndex < p; newIndex++) {
            int oldIndex = inverse[newIndex];
            src.row(start + newIndex).copyTo(dst.row(start + oldIndex));
        }

        unscrambleBlock(dst, src, start + p, end, r, s);
    }

    // ----------------------------
    //  CORRÉLATION DE PEARSON
    // ----------------------------
    private static double pearson(byte[] a, byte[] b) {
        int n = a.length;
        double meanA = 0, meanB = 0;

        for (int i = 0; i < n; i++) {
            meanA += (a[i] & 0xFF);
            meanB += (b[i] & 0xFF);
        }

        meanA /= n;
        meanB /= n;

        double num = 0, denA = 0, denB = 0;

        for (int i = 0; i < n; i++) {
            double da = (a[i] & 0xFF) - meanA;
            double db = (b[i] & 0xFF) - meanB;

            num += da * db;
            denA += da * da;
            denB += db * db;
        }

        if (denA == 0 || denB == 0) return -1;
        return num / Math.sqrt(denA * denB);
    }

    /**
     * Score global = somme des corrélations entre lignes consécutives
     */
    private static double computeScore(Mat img) {
        int rows = img.rows();
        int cols = img.cols() * img.channels();

        double total = 0;

        byte[] row1 = new byte[cols];
        byte[] row2 = new byte[cols];

        for (int i = 0; i < rows - 1; i++) {
            img.row(i).get(0, 0, row1);
            img.row(i + 1).get(0, 0, row2);

            total += pearson(row1, row2);
        }

        return total;
    }

    // ----------------------------
    //  BRUTE-FORCE MULTI-THREAD
    // ----------------------------
    /**
     * Parcourt toutes les clés (r: 0..255, s: 0..127) en parallèle et retourne la meilleure clé [r,s].
     * On n'extrait pas l'image gagnante ici (économie mémoire) : on retourne la clé et tu peux
     * ensuite appeler unscramble(scrambled, r, s) pour obtenir l'image.
     */
    public static int[] bruteForce(Mat scrambled) {
        final int TOTAL_KEYS = 256 * 128; // 32768
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(cores);

        List<Callable<Result>> tasks = new ArrayList<>();

        // Découper l'espace de clés en 'cores' plages
        int chunk = (TOTAL_KEYS + cores - 1) / cores;

        for (int t = 0; t < cores; t++) {
            final int startKey = t * chunk;
            final int endKey = Math.min(TOTAL_KEYS, startKey + chunk);

            tasks.add(() -> {
                double bestScore = Double.NEGATIVE_INFINITY;
                int bestR = 0, bestS = 0;

                Mat tmp = new Mat(); // Mat local au thread (réutilisable)
                for (int key = startKey; key < endKey; key++) {
                    int r = key & 0xFF;
                    int s = (key >> 8) & 0x7F;

                    // décramble dans tmp
                    unscramble(scrambled, tmp, r, s);

                    double score = computeScore(tmp);

                    if (score > bestScore) {
                        bestScore = score;
                        bestR = r;
                        bestS = s;
                    }
                }
                return new Result(bestR, bestS, bestScore);
            });
        }

        try {
            List<Future<Result>> futures = pool.invokeAll(tasks);

            // Combiner
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
            return new int[]{bestR, bestS};

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            pool.shutdownNow();
            return new int[]{0, 0};
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

    /**
     * Variation de unscramble qui écrit dans dst (utilisée par bruteForce)
     */
    private static void unscramble(Mat src, Mat dst, int r, int s) {
        src.copyTo(dst);
        unscrambleBlock(dst, src, 0, src.rows(), r, s);
    }
}
