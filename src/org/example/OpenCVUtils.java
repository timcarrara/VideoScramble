/**
 * Utilitaires de conversion OpenCV vers JavaFX
 * Auteurs : BONNIN Simon, CARRARA Tim
 * Groupe  : S5 - A2
 * Date    : Décembre 2025
 * Description : Cette classe fournit des méthodes utilitaires pour convertir
 * des images OpenCV (Mat) vers des objets Image JavaFX, en gérant correctement
 * les conversions d'espaces colorimétriques.
 */

package org.example;

import javafx.scene.image.*;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

/**
 * Classe utilitaire pour la conversion d'images entre OpenCV et JavaFX.
 * Cette classe fournit des méthodes pour convertir des objets Mat d'OpenCV
 * en objets Image de JavaFX, ce qui permet d'afficher des images traitées
 * avec OpenCV dans une interface graphique JavaFX.

 * Espace colorimétrique : OpenCV utilise par défaut l'ordre BGR
 * (Blue-Green-Red) pour stocker les images en couleur, tandis que JavaFX utilise
 * l'ordre RGB (Red-Green-Blue). Cette classe gère automatiquement la conversion
 * entre ces deux espaces colorimétriques pour garantir un affichage correct des couleurs.
 *
 * @author BONNIN Simon, CARRARA Tim
 * @version 1.0
 */

public class OpenCVUtils {

    /**
     * Convertit une image OpenCV Mat en objet Image JavaFX.
     * Cette méthode effectue les opérations suivantes :
     * Conversion de l'espace colorimétrique BGR (OpenCV) vers RGB (JavaFX)
     * Extraction des données de pixels sous forme de tableau de bytes
     * Création d'une WritableImage JavaFX
     * Copie des pixels dans l'image JavaFX avec le format ByteRgb approprié
     *
     * @param mat   L'objet Mat d'OpenCV à convertir.
     * @return Une Image JavaFX prête à être affichée dans un ImageView,
     */
    public static Image matToImage(Mat mat) {
        try {
            // Conversion BGR → RGB (CORRECTION FILTRE BLEU)
            Mat rgb = new Mat();
            Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);

            int width = rgb.width();
            int height = rgb.height();
            int channels = rgb.channels();

            byte[] pixels = new byte[width * height * channels];
            rgb.get(0, 0, pixels);

            WritableImage wi = new WritableImage(width, height);
            PixelWriter pw = wi.getPixelWriter();

            // ★ IMPORTANT : PixelFormat de type ByteBuffer sinon JavaFX refuse setPixels
            PixelFormat<ByteBuffer> format = PixelFormat.getByteRgbInstance();

            pw.setPixels(
                    0, 0,
                    width, height,
                    format,
                    pixels, 0,
                    width * channels
            );

            return wi;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
