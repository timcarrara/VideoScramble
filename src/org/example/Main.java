package org.example;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import java.nio.ByteBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;

public class Main extends Application {

    static { System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME); }

    @Override
    public void start(Stage stage) {

        // Charge l'image
        Mat img = Imgcodecs.imread("src/images/image.jpg");

        if (img.empty()) {
            System.out.println("Impossible de charger l'image !");
            return;
        }

        int r = 50;
        int s = 20;

        // Chiffre
        Mat scrambled = FrameScrambler.scramble(img, r, s);

        // Déchiffre par brute-force
        int[] key = FrameScrambler.bruteForce(scrambled);
        int foundR = key[0];
        int foundS = key[1];

        Mat brute = FrameScrambler.unscramble(scrambled, foundR, foundS);

        // Affichage JavaFX
        ImageView v1 = new ImageView(matToFX(img));
        ImageView v2 = new ImageView(matToFX(scrambled));
        ImageView v3 = new ImageView(matToFX(brute));

        v1.setFitWidth(300); v1.setPreserveRatio(true);
        v2.setFitWidth(300); v2.setPreserveRatio(true);
        v3.setFitWidth(300); v3.setPreserveRatio(true);

        HBox root = new HBox(10, v1, v2, v3);
        stage.setScene(new Scene(root));
        stage.setTitle("Scramble / Brute-force decrypt");
        stage.show();
    }

    // Convertit Mat (BGR) → WritableImage (RGB)
    private Image matToFX(Mat mat) {
        Mat rgb = new Mat();
        org.opencv.imgproc.Imgproc.cvtColor(mat, rgb, org.opencv.imgproc.Imgproc.COLOR_BGR2RGB);

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
