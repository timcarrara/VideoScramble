/**
 * Évaluation de score d'image
 * Auteurs : BONNIN Simon, CARRARA Tim
 * Groupe  : S5 - A2
 * Date    : Décembre 2025
 * Description : Cette classe fournit une méthode pour évaluer la qualité
 * d'une clé de déchiffrement en calculant les corrélations de Pearson
 * entre des lignes consécutives d'une image.
 */

package org.example;

import static org.example.PearsonCorrelation.pearson;

/**
 * Classe pour évaluer la qualité d'une clé de déchiffrement d'image.
 * Cette classe utilise le coefficient de corrélation de Pearson pour mesurer
 * la similarité entre lignes consécutives d'une image. Une image correctement
 * déchiffrée présente généralement une forte corrélation entre lignes adjacentes.
 * @author BONNIN Simon, CARRARA Tim
 * @version 1.0
 */

public class EvaluationScoreImage {
    /**
     * Constante représentant un score invalide.
     * Utilisée pour indiquer qu'une clé n'est pas prometteuse.
     */
    private static final double SCORE_INVALIDE = Double.NEGATIVE_INFINITY;

    /**
     * Évalue la qualité d'une clé de déchiffrement en calculant la somme
     * des corrélations de Pearson entre lignes consécutives de l'image.
     * Cette méthode utilise une optimisation : elle arrête le calcul prématurément
     * si le score anticipé ne peut pas dépasser le meilleur score connu.
     *
     * @param lignesImage           Tableau 2D représentant les lignes de l'image déchiffrée
     * @param indicesEchantillon    Indices des lignes à échantillonner pour l'évaluation
     * @param scoreMeilleurCle      Score de la meilleure clé trouvée jusqu'à présent
     * @return La somme des corrélations de Pearson, ou INVALID_SCORE si la clé
     *         n'est pas prometteuse
     */
    public static double evaluateKeyScore(byte[][] lignesImage, int[] indicesEchantillon, double scoreMeilleurCle) {
        // Somme des valeurs de Pearson
        double totalPearson = 0.0;
        int nbPaire = 0;
        // Nombre total de paire qu'on prévoit d'examiner
        int nbPaireTotal = indicesEchantillon.length;
        for (int idx : indicesEchantillon) {
            // Calcule la corrélation entre la ligne et celle d'en dessous
            double p = pearson(lignesImage[idx], lignesImage[idx + 1]);
            // Ajoute au total si la corrélation est valide
            if (p > -1) {
                totalPearson += p;
            }
            nbPaire++;
            int nbPaireRestante = nbPaireTotal - nbPaire;
            // Anticipation du meilleur score de la clé testé
            // en supposant une corrélation parfaite de pour les autres paires
            double scoreCle = totalPearson + nbPaireRestante * 1.0;
            // Si cette clé est inférieur au score de la meilleur clé on test la suivante
            if (scoreCle <= scoreMeilleurCle) {
                return SCORE_INVALIDE;
            }
        }
        return totalPearson;
    }
}
