package org.example;

import static org.example.PearsonCorrelation.pearson;

public class EvaluationScoreImage {

    private static final double INVALID_SCORE = Double.NEGATIVE_INFINITY;

    public static double evaluateKeyScore(byte[][] dstRows, int[] sampleIndices, double scoreMeilleurCle) {
        // Somme des valeurs de Pearson
        double totalPearson = 0.0;
        int nbPaire = 0;
        // Nombre de paire qu'on prévoit d'examiner
        int nbPaireTotal = sampleIndices.length;
        for (int idx : sampleIndices) {
            // Calcule la corrélation entre la ligne et celle d'en dessous
            double p = pearson(dstRows[idx], dstRows[idx + 1]);
            if (p > -1) {
                totalPearson += p;
            }
            nbPaire++;
            int nbPaireRestante = nbPaireTotal - nbPaire;
            // Anticipation du meilleur score de la clé testé
            double scoreCle = totalPearson + nbPaireRestante * 1.0;
            // Si cette clé est inférieur au score de la meilleur clé on test la suivante
            if (scoreCle <= scoreMeilleurCle) {
                return INVALID_SCORE;
            }
        }
        return totalPearson;
    }
}
