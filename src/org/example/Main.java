package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Image Scramble Viewer");

        // Deux zones d’affichage
        ImageView left = new ImageView();
        ImageView right = new ImageView();

        left.setFitWidth(640);
        right.setFitWidth(640);
        left.setPreserveRatio(true);
        right.setPreserveRatio(true);

        HBox root = new HBox(10, left, right);
        Scene scene = new Scene(root, 1300, 600);
        stage.setScene(scene);
        stage.show();

        // Charge l’image
        Mat img = Imgcodecs.imread("src/images/test.jpg");
        if (img.empty()) {
            System.err.println("❌ Impossible de charger l'image !");
            return;
        }

        // Image originale → JavaFX
        Image fxOriginal = MatToImage.matToImage(img);
        left.setImage(fxOriginal);

        // Scramble → JavaFX
        Mat scrambled = VideoScrambler.scramble(img);
        Image fxScrambled = MatToImage.matToImage(scrambled);
        right.setImage(fxScrambled);
    }

    public static void main(String[] args) {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("OpenCV chargé !");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("❌ OpenCV non trouvé.");
        }
        launch();
    }
}
