package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

/**
 * Main d'exemple : lecture image, scramble, brute-force multithread, affichage.
 */
public class Main extends Application {

    static { System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME); }

    @Override
    public void start(Stage stage) {

        String path = "src/images/image.jpg";
        Mat img = Imgcodecs.imread(path);

        if (img.empty()) {
            System.err.println("Impossible de charger l'image : " + path);
            return;
        }

        // clé connue (pour vérifier)
        int r = 50;
        int s = 20;

        // chiffrement
        Mat scrambled = FrameScrambler.scramble(img, r, s);
        System.out.println("Image chiffrée.");

        // brute-force multithread (retourne la meilleure clé)
        long t0 = System.currentTimeMillis();
        int[] found = FrameScrambler.bruteForceTurbo(scrambled);
        long t1 = System.currentTimeMillis();
        System.out.println("Brute-force optimisé terminé en " + (t1 - t0) + " ms. clé trouvée : r=" + found[0] + " s=" + found[1]);

        // Déchiffrer avec la clé trouvée
        Mat brute = FrameScrambler.unscramble(scrambled, found[0], found[1]);

        // Affichage JavaFX (conversion BGR -> RGB sans filtre bleu)
        ImageView v1 = new ImageView(matToFX(img));
        ImageView v2 = new ImageView(matToFX(scrambled));
        ImageView v3 = new ImageView(matToFX(brute));

        v1.setFitWidth(300); v1.setPreserveRatio(true);
        v2.setFitWidth(300); v2.setPreserveRatio(true);
        v3.setFitWidth(300); v3.setPreserveRatio(true);

        HBox root = new HBox(10, v1, v2, v3);
        stage.setScene(new Scene(root));
        stage.setTitle("Scramble / Brute-force decrypt (multithread)");
        stage.show();
    }

    // Convertit Mat (BGR) → WritableImage (RGB)
    private Image matToFX(Mat mat) {
        Mat rgb = new Mat();
        Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);

        int width = rgb.cols();
        int height = rgb.rows();
        int channels = rgb.channels();

        byte[] buffer = new byte[width * height * channels];
        rgb.get(0, 0, buffer);

        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        PixelFormat<ByteBuffer> pf = PixelFormat.getByteRgbInstance();

        pw.setPixels(0, 0, width, height, pf, buffer, 0, width * channels);
        return image;
    }

    public static void main(String[] args) {
        launch();
    }
}
