package org.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Main vidéo : lecture, scramble et déchiffrement frame par frame en temps réel.
 */
public class MainVideo extends Application {

    static { System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME); }

    private VideoCapture captureOriginal;
    private VideoCapture captureScrambled;
    private VideoCapture captureDecrypted;

    private ImageView viewOriginal;
    private ImageView viewScrambled;
    private ImageView viewDecrypted;

    private int r = 50;
    private int s = 20;
    private double fps = 30.0;

    @Override
    public void start(Stage stage) {

        String path = "src/videos/video.mp4";

        // Ouvrir 3 captures pour les 3 affichages
        captureOriginal = new VideoCapture(path);
        captureScrambled = new VideoCapture(path);
        captureDecrypted = new VideoCapture(path);

        if (!captureOriginal.isOpened()) {
            System.err.println("Impossible de charger la vidéo : " + path);
            return;
        }

        fps = captureOriginal.get(Videoio.CAP_PROP_FPS);
        if (fps <= 0) fps = 30.0;

        System.out.println("Vidéo chargée : " + captureOriginal.get(Videoio.CAP_PROP_FRAME_COUNT) + " frames @ " + fps + " fps");

        // Interface
        viewOriginal = new ImageView();
        viewScrambled = new ImageView();
        viewDecrypted = new ImageView();

        // Calculer la largeur pour chaque vidéo (1/3 de la largeur totale)
        double videoWidth = 450;
        viewOriginal.setFitWidth(videoWidth); viewOriginal.setPreserveRatio(true);
        viewScrambled.setFitWidth(videoWidth); viewScrambled.setPreserveRatio(true);
        viewDecrypted.setFitWidth(videoWidth); viewDecrypted.setPreserveRatio(true);

        Text t1 = new Text("Input image");
        Text t2 = new Text("Scrambled image — key=" + (r * 128 + s));
        Text t3 = new Text("Unscrambled image — key=" + (r * 128 + s));

        t1.setStyle("-fx-font-size: 14px; -fx-fill: white;");
        t2.setStyle("-fx-font-size: 14px; -fx-fill: white;");
        t3.setStyle("-fx-font-size: 14px; -fx-fill: white;");

        VBox box1 = new VBox(10, t1, viewOriginal);
        VBox box2 = new VBox(10, t2, viewScrambled);
        VBox box3 = new VBox(10, t3, viewDecrypted);

        box1.setStyle("-fx-alignment: center;");
        box2.setStyle("-fx-alignment: center;");
        box3.setStyle("-fx-alignment: center;");

        HBox root = new HBox(15, box1, box2, box3);
        root.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 20;");

        Scene scene = new Scene(root, 1400, 650);
        stage.setScene(scene);
        stage.setTitle("Video Scramble / Decrypt en temps réel");
        stage.setOnCloseRequest(e -> cleanup());
        stage.show();

        // Lancer la lecture
        playVideo();
    }

    private void playVideo() {
        final long frameTimeNanos = (long) (1_000_000_000.0 / fps);

        AnimationTimer timer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= frameTimeNanos) {

                    Mat frameOriginal = new Mat();
                    Mat frameScrambled = new Mat();
                    Mat frameDecrypted = new Mat();

                    // Lire les frames
                    boolean hasFrame = captureOriginal.read(frameOriginal);
                    captureScrambled.read(frameScrambled);
                    captureDecrypted.read(frameDecrypted);

                    if (hasFrame && !frameOriginal.empty()) {
                        // Afficher l'originale
                        viewOriginal.setImage(OpenCVUtils.matToImage(frameOriginal));

                        // Scramble
                        Mat scrambled = FrameScrambler.scramble(frameScrambled, r, s);
                        viewScrambled.setImage(OpenCVUtils.matToImage(scrambled));

                        // Déchiffrer
                        Mat decrypted = FrameScrambler.unscramble(scrambled, r, s);
                        viewDecrypted.setImage(OpenCVUtils.matToImage(decrypted));

                        // Libérer la mémoire
                        frameOriginal.release();
                        frameScrambled.release();
                        frameDecrypted.release();
                        scrambled.release();
                        decrypted.release();

                    } else {
                        // Fin de la vidéo, boucler
                        captureOriginal.set(Videoio.CAP_PROP_POS_FRAMES, 0);
                        captureScrambled.set(Videoio.CAP_PROP_POS_FRAMES, 0);
                        captureDecrypted.set(Videoio.CAP_PROP_POS_FRAMES, 0);
                    }

                    lastUpdate = now;
                }
            }
        };
        timer.start();
    }

    private void cleanup() {
        if (captureOriginal != null) captureOriginal.release();
        if (captureScrambled != null) captureScrambled.release();
        if (captureDecrypted != null) captureDecrypted.release();
    }

    public static void main(String[] args) {
        launch();
    }
}