package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;

/**
 * VideoScramble - Application JavaFX pour chiffrement/déchiffrement vidéo
 * Modes d'utilisation :
 * 1. Chiffrement : java Main -e input.mp4 output.mp4 r s
 * 2. Déchiffrement avec clé : java Main -d input.mp4 output.mp4 r s
 * 3. Déchiffrement sans clé (brute force) : java Main -c input.mp4 output.mp4
 * 4. Chiffrement avec clé embarquée : java Main -ee input.mp4 output.mp4 r s
 * 5. Déchiffrement avec clé embarquée : java Main -de input.mp4 output.mp4
 */
public class Main extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    // Paramètres en ligne de commande
    private static String mode = null;
    private static String inputPath = null;
    private static String outputPath = null;
    private static int r = 42;
    private static int s = 17;
    private static boolean embedKey = false;
    private static boolean extractKey = false;
    private static boolean crackKey = false;

    // Interface JavaFX
    private ImageView leftImageView;
    private ImageView rightImageView;
    private Label statusLabel;
    private Label keyLabel;
    private ProgressBar progressBar;
    private Label fpsLabel;

    // Variables de traitement vidéo
    private volatile boolean stopProcessing = false;
    private Thread processingThread;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VideoScramble - Chiffrement Vidéo");

        // Layout principal
        BorderPane root = new BorderPane();

        // Zone du haut : informations et contrôles
        VBox topBox = createTopPanel();
        root.setTop(topBox);

        // Zone centrale : affichage des vidéos
        HBox videoBox = createVideoPanel();
        root.setCenter(videoBox);

        // Zone du bas : boutons de contrôle
        HBox bottomBox = createBottomPanel(primaryStage);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 1400, 700);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> cleanup());
        primaryStage.show();

        // Lancer le traitement automatiquement
        if (inputPath != null) {
            startProcessing();
        }
    }

    private VBox createTopPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f0f0f0;");

        statusLabel = new Label("Mode: " + getModeDescription());
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        keyLabel = new Label("Clé (r, s): (" + r + ", " + s + ")");
        keyLabel.setStyle("-fx-font-size: 14px;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);

        fpsLabel = new Label("FPS: --");

        box.getChildren().addAll(statusLabel, keyLabel, progressBar, fpsLabel);
        return box;
    }

    private HBox createVideoPanel() {
        HBox box = new HBox(20);
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER);

        // Vidéo gauche (originale)
        VBox leftBox = new VBox(5);
        leftBox.setAlignment(Pos.CENTER);
        Label leftLabel = new Label("Vidéo d'entrée");
        leftLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        leftImageView = new ImageView();
        leftImageView.setFitWidth(640);
        leftImageView.setFitHeight(480);
        leftImageView.setPreserveRatio(true);
        leftBox.getChildren().addAll(leftLabel, leftImageView);

        // Vidéo droite (traitée)
        VBox rightBox = new VBox(5);
        rightBox.setAlignment(Pos.CENTER);
        Label rightLabel = new Label("Vidéo de sortie");
        rightLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        rightImageView = new ImageView();
        rightImageView.setFitWidth(640);
        rightImageView.setFitHeight(480);
        rightImageView.setPreserveRatio(true);
        rightBox.getChildren().addAll(rightLabel, rightImageView);

        box.getChildren().addAll(leftBox, rightBox);
        return box;
    }

    private HBox createBottomPanel(Stage stage) {
        HBox box = new HBox(10);
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER);

        Button stopBtn = new Button("Arrêter");
        stopBtn.setOnAction(e -> stopProcessing = true);

        Button closeBtn = new Button("Fermer");
        closeBtn.setOnAction(e -> {
            cleanup();
            stage.close();
        });

        box.getChildren().addAll(stopBtn, closeBtn);
        return box;
    }

    private void startProcessing() {
        processingThread = new Thread(() -> {
            try {
                processVideo();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur : " + e.getMessage());
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur de traitement");
                    alert.setHeaderText("Une erreur s'est produite");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        });
        processingThread.setDaemon(true);
        processingThread.start();
    }

    private void processVideo() {
        VideoCapture capture = new VideoCapture(inputPath);
        if (!capture.isOpened()) {
            throw new RuntimeException("Impossible d'ouvrir la vidéo : " + inputPath);
        }

        // Récupération des propriétés de la vidéo
        int frameWidth = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int frameHeight = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        int totalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);

        Platform.runLater(() -> {
            fpsLabel.setText("FPS: " + String.format("%.1f", fps) + " | Frames: " + totalFrames);
        });

        // Création du VideoWriter
        VideoWriter writer = null;
        if (outputPath != null && !crackKey) {
            int fourcc = VideoWriter.fourcc('m', 'p', '4', 'v');
            writer = new VideoWriter(outputPath, fourcc, fps, new Size(frameWidth, frameHeight));

            if (!writer.isOpened()) {
                capture.release();
                throw new RuntimeException("Impossible de créer la vidéo de sortie : " + outputPath);
            }
        }

        // Traitement frame par frame
        Mat frame = new Mat();
        int frameCount = 0;
        long startTime = System.currentTimeMillis();

        // Pour le crack de clé, utiliser la première image non-noire
        if (crackKey) {
            Mat firstValidFrame = findFirstNonBlackFrame(capture);
            if (firstValidFrame != null) {
                Platform.runLater(() -> statusLabel.setText("Recherche de la clé par force brute..."));
                int[] crackedKey = TrouveCleBruteForce.bruteForce(firstValidFrame);
                r = crackedKey[0];
                s = crackedKey[1];
                Platform.runLater(() -> {
                    keyLabel.setText("Clé trouvée (r, s): (" + r + ", " + s + ")");
                    statusLabel.setText("Clé crackée ! Déchiffrement en cours...");
                });
                firstValidFrame.release();
            }
            // Réinitialiser la capture
            capture.release();
            capture = new VideoCapture(inputPath);
        }

        while (!stopProcessing && capture.read(frame) && !frame.empty()) {
            frameCount++;

            // Déterminer la clé à utiliser
            int currentR = r;
            int currentS = s;

            if (extractKey) {
                // Extraction de la clé embarquée
                int[] embeddedKey = KeyEmbedder.extractKeyFromPixel(frame);
                currentR = embeddedKey[0];
                currentS = embeddedKey[1];
                if (frameCount == 1) {
                    int finalR = currentR;
                    int finalS = currentS;
                    Platform.runLater(() -> {
                        keyLabel.setText("Clé extraite (r, s): (" + finalR + ", " + finalS + ")");
                    });
                }
            }

            // Traitement de la frame
            Mat processed;
            if (mode.startsWith("-e")) {
                // Chiffrement
                processed = PermutationLignes.scrambleFrame(frame, currentR, currentS);
                if (embedKey) {
                    KeyEmbedder.embedKeyInPixel(processed, currentR, currentS);
                }
            } else {
                // Déchiffrement
                processed = PermutationLignes.unscrambleFrame(frame, currentR, currentS);
            }

            // Écriture de la frame traitée
            if (writer != null) {
                writer.write(processed);
            }

            // Affichage dans l'interface (toutes les 3 frames pour performance)
            if (frameCount % 3 == 0) {
                Mat frameToShow = frame.clone();
                Mat processedToShow = processed.clone();
                int currentFrame = frameCount;

                Platform.runLater(() -> {
                    leftImageView.setImage(OpenCVUtils.matToImage(frameToShow));
                    rightImageView.setImage(OpenCVUtils.matToImage(processedToShow));
                    progressBar.setProgress((double) currentFrame / totalFrames);
                    frameToShow.release();
                    processedToShow.release();
                });
            }

            processed.release();

            // Calcul et affichage du FPS
            if (frameCount % 30 == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                double currentFps = (frameCount * 1000.0) / elapsed;
                int finalFrameCount1 = frameCount;
                Platform.runLater(() -> {
                    fpsLabel.setText(String.format("FPS: %.1f | Frame: %d/%d", currentFps, finalFrameCount1, totalFrames));
                });
            }
        }

        // Libération des ressources
        frame.release();
        capture.release();
        if (writer != null) {
            writer.release();
        }

        int finalFrameCount = frameCount;
        Platform.runLater(() -> {
            statusLabel.setText("Traitement terminé ! " + finalFrameCount + " frames traitées.");
            progressBar.setProgress(1.0);
        });
    }

    private Mat findFirstNonBlackFrame(VideoCapture capture) {
        Mat frame = new Mat();
        int count = 0;
        while (count < 100 && capture.read(frame) && !frame.empty()) {
            // Calculer la moyenne des pixels
            double sum = 0;
            byte[] data = new byte[(int) frame.total() * frame.channels()];
            frame.get(0, 0, data);
            for (byte b : data) {
                sum += (b & 0xFF);
            }
            double avg = sum / data.length;

            // Si moyenne > 10, considérer comme non-noir
            if (avg > 10) {
                return frame.clone();
            }
            count++;
        }
        return null;
    }

    private void cleanup() {
        stopProcessing = true;
        if (processingThread != null && processingThread.isAlive()) {
            try {
                processingThread.join(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String getModeDescription() {
        if (mode == null) return "Non défini";
        return switch (mode) {
            case "-e" -> "Chiffrement";
            case "-d" -> "Déchiffrement";
            case "-c" -> "Crack de clé (brute force)";
            case "-ee" -> "Chiffrement avec clé embarquée";
            case "-de" -> "Déchiffrement avec clé embarquée";
            default -> "Inconnu";
        };
    }

    public static void main(String[] args) {
        // Analyse des arguments
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        mode = args[0];
        inputPath = args[1];

        if (!new File(inputPath).exists()) {
            System.err.println("Erreur : Le fichier d'entrée n'existe pas : " + inputPath);
            System.exit(1);
        }

        switch (mode) {
            case "-e": // Chiffrement
            case "-d": // Déchiffrement
                if (args.length < 5) {
                    System.err.println("Erreur : r et s requis pour ce mode");
                    printUsage();
                    System.exit(1);
                }
                outputPath = args[2];
                r = Integer.parseInt(args[3]);
                s = Integer.parseInt(args[4]);
                break;

            case "-ee": // Chiffrement avec clé embarquée
                if (args.length < 5) {
                    System.err.println("Erreur : r et s requis pour l'embarquement");
                    printUsage();
                    System.exit(1);
                }
                outputPath = args[2];
                r = Integer.parseInt(args[3]);
                s = Integer.parseInt(args[4]);
                embedKey = true;
                mode = "-e";
                break;

            case "-de": // Déchiffrement avec clé embarquée
                if (args.length < 3) {
                    System.err.println("Erreur : chemin de sortie requis");
                    printUsage();
                    System.exit(1);
                }
                outputPath = args[2];
                extractKey = true;
                mode = "-d";
                break;

            case "-c": // Crack de clé
                if (args.length >= 3) {
                    outputPath = args[2];
                }
                crackKey = true;
                mode = "-d";
                break;

            default:
                System.err.println("Mode inconnu : " + mode);
                printUsage();
                System.exit(1);
        }

        // Validation des paramètres
        if (r < 0 || r > 255) {
            System.err.println("Erreur : r doit être entre 0 et 255");
            System.exit(1);
        }
        if (s < 0 || s > 127) {
            System.err.println("Erreur : s doit être entre 0 et 127");
            System.exit(1);
        }

        // Lancement de l'interface JavaFX
        launch(args);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  Chiffrement:");
        System.out.println("    java Main -e <input.mp4> <output.mp4> <r> <s>");
        System.out.println("  Déchiffrement:");
        System.out.println("    java Main -d <input.mp4> <output.mp4> <r> <s>");
        System.out.println("  Crack de clé (brute force):");
        System.out.println("    java Main -c <input.mp4> [output.mp4]");
        System.out.println("  Chiffrement avec clé embarquée:");
        System.out.println("    java Main -ee <input.mp4> <output.mp4> <r> <s>");
        System.out.println("  Déchiffrement avec clé embarquée:");
        System.out.println("    java Main -de <input.mp4> <output.mp4>");
        System.out.println("\nParamètres:");
        System.out.println("  r : offset (0-255)");
        System.out.println("  s : step (0-127)");
    }
}
