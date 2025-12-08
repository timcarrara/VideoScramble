package org.example;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class FrameScrambler {

    /**
     * Applique la permutation (scramble) selon r,s et blocs de puissances de 2
     */
    public static Mat scramble(Mat src, int r, int s) {
        Mat dst = src.clone();
        int height = src.rows();
        int width = src.cols();

        scrambleBlock(dst, src, 0, height, r, s);

        return dst;
    }

    /**
     * Mélange récursivement par blocs de tailles puissances de 2
     */
    private static void scrambleBlock(Mat dst, Mat src, int start, int end, int r, int s) {
        int size = end - start;
        if (size <= 1) return;

        // Trouve la plus grande puissance de 2 <= size
        int p = Integer.highestOneBit(size);

        // On traite le bloc [start ; start+p[
        for (int i = 0; i < p; i++) {
            int newIndex = (r + ((2 * s + 1) * i)) % p;
            newIndex += start;

            src.row(start + i).copyTo(dst.row(newIndex));
        }

        // Puis on continue avec le reste
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

        // Inverse est un tableau tel que inverse[newIndex] = oldIndex
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
    //  BRUTE-FORCE
    // ----------------------------
    public static int[] bruteForce(Mat scrambled) {
        double bestScore = Double.NEGATIVE_INFINITY;
        int bestR = 0, bestS = 0;

        Mat tmp = new Mat();

        for (int r = 0; r < 256; r++) {
            for (int s = 0; s < 128; s++) {

                unscramble(scrambled, tmp, r, s);

                double score = computeScore(tmp);

                if (score > bestScore) {
                    bestScore = score;
                    bestR = r;
                    bestS = s;
                }
            }
        }

        return new int[]{bestR, bestS};
    }

    private static void unscramble(Mat src, Mat dst, int r, int s) {
        src.copyTo(dst);
        unscrambleBlock(dst, src, 0, src.rows(), r, s);
    }
}
