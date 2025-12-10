package org.example;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class FrameScrambler {

    // --- Crypter l'image ---

    public static Mat scramble(Mat src, int r, int s) {
        Mat dst = src.clone();
        int height = src.rows();
        scrambleBlock(dst, src, 0, height, r, s);
        return dst;
    }

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


    // --- Décrypter l'image ---

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


    // --- Corrélation de Pearson ---

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

    private static double sampledScoreWithEarlyExit(byte[][] dstRows, int[] sampleIndices, double globalBest) {
        double total = 0.0;
        int processed = 0;
        int sampleCount = sampleIndices.length;
        for (int idx : sampleIndices) {
            double p = pearson(dstRows[idx], dstRows[idx + 1]);
            if (p > -1) total += p;

            processed++;
            int remaining = sampleCount - processed;
            double optimistic = total + remaining * 1.0;
            if (optimistic <= globalBest) {
                return Double.NEGATIVE_INFINITY;
            }
        }
        return total;
    }

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


    // -----------------------------------------
    // BRUTE-FORCE MULTI-THREAD OPTIMISÉ (TURBO)
    // -----------------------------------------

    /**
     * Brute-force optimisé, très rapide et fiable.
     */
    public static int[] bruteForceTurbo(Mat scrambled) {
        final int TOTAL_KEYS = 256 * 128;
        final double SAMPLE_FRACTION = 0.55;
        final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(cores);

        int rows = scrambled.rows();
        if (rows < 2) {
            return new int[]{0, 0};
        }
        final int colsBytes = scrambled.cols() * scrambled.channels();

        // Préchargement des lignes dans des buffers
        final byte[][] srcRows = new byte[rows][];
        for (int i = 0; i < rows; i++) {
            byte[] row = new byte[colsBytes];
            scrambled.row(i).get(0, 0, row);
            srcRows[i] = row;
        }

        // Indices échantillonnés
        int totalPairs = rows - 1;
        int samplePairs = Math.max(1, (int) Math.round(totalPairs * SAMPLE_FRACTION));
        int step = Math.max(1, totalPairs / samplePairs);
        final int[] sampleIndices;
        {
            IntArrayListBuilder b = new IntArrayListBuilder();
            for (int i = 0; i < totalPairs; i += step) {
                b.add(i);
            }
            sampleIndices = b.toArray();
        }

        AtomicReference<Double> globalBest = new AtomicReference<>(Double.NEGATIVE_INFINITY);

        int chunk = (TOTAL_KEYS + cores - 1) / cores;
        List<Callable<Result>> tasks = new ArrayList<>();

        for (int t = 0; t < cores; t++) {
            final int startKey = t * chunk;
            final int endKey = Math.min(TOTAL_KEYS, startKey + chunk);

            tasks.add(() -> {
                byte[][] dstRows = new byte[rows][];

                double bestScoreLocal = Double.NEGATIVE_INFINITY;
                int bestR = 0, bestS = 0;

                for (int key = startKey; key < endKey; key++) {
                    int r = key & 0xFF;
                    int s = (key >> 8) & 0x7F;

                    arrayUnscrambleRows(dstRows, srcRows, 0, rows, r, s);

                    double score = sampledScoreWithEarlyExit(dstRows, sampleIndices, globalBest.get());

                    if (score > bestScoreLocal) {
                        bestScoreLocal = score;
                        bestR = r;
                        bestS = s;

                        Double cur;
                        do {
                            cur = globalBest.get();
                            if (bestScoreLocal <= cur) break;
                        } while (!globalBest.compareAndSet(cur, bestScoreLocal));
                    }
                }

                return new Result(bestR, bestS, bestScoreLocal);
            });
        }

        try {
            List<Future<Result>> futures = pool.invokeAll(tasks);

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


    private static void arrayUnscrambleRows(byte[][] dstRows, byte[][] srcRows, int start, int end, int r, int s) {
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

        arrayUnscrambleRows(dstRows, srcRows, start + p, end, r, s);
    }
}
