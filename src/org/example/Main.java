package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
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
 *
 * @author Votre Nom
 * @version 1.0
 */
public class Main extends Application {

    static {
        // Charger explicitement la DLL FFmpeg AVANT OpenCV
        String ffmpegPath = "C:\\Users\\PC\\Documents\\opencv\\build\\java\\x64\\opencv_videoio_ffmpeg4120_64.dll";
        try {
            System.out.println("Chargement de FFmpeg : " + ffmpegPath);
            System.load(ffmpegPath);
            System.out.println("FFmpeg chargé avec succès !");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("ERREUR chargement FFmpeg : " + e.getMessage());
            e.printStackTrace();
        }

        // Puis charger OpenCV
        try {
            System.loadLibrary("opencv_java4120");
            System.out.println("OpenCV chargé avec succès !");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("ERREUR chargement OpenCV : " + e.getMessage());
            e.printStackTrace();
        }

        // Test immédiat
        System.out.println("OpenCV version: " + Core.VERSION);
        System.out.println("Build information:");
        System.out.println(Core.getBuildInformation());
    }

    // Paramètres de traitement
    private String inputPath = null;
    private String outputPath = null;
    private int r = 42;
    private int s = 17;
    private String mode = "-e";
    private boolean embedKey = false;
    private boolean extractKey = false;
    private boolean crackKey = false;

    // Composants d'interface
    private TextField inputField;
    private TextField outputField;
    private Spinner<Integer> rSpinner;
    private Spinner<Integer> sSpinner;
    private ComboBox<String> modeCombo;
    private CheckBox embedKeyCheck;
    private ImageView leftImageView;
    private ImageView rightImageView;
    private Label statusLabel;
    private Label keyLabel;
    private ProgressBar progressBar;
    private Label fpsLabel;
    private Button startBtn;
    private Button stopBtn;

    // Variables de traitement vidéo
    private volatile boolean stopProcessing = false;
    private Thread processingThread;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VideoScramble - Chiffrement Vidéo");

        // Layout principal : gauche = contrôles, droite = vidéos
        HBox root = new HBox(0);

        // Panneau de gauche : contrôles
        VBox leftPanel = createControlPanel(primaryStage);
        leftPanel.setPrefWidth(400);
        leftPanel.setMinWidth(400);

        // Panneau de droite : vidéos
        VBox rightPanel = createVideoPanel();

        root.getChildren().addAll(leftPanel, rightPanel);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setOnCloseRequest(e -> cleanup());
        primaryStage.show();
    }

    private VBox createControlPanel(Stage stage) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 0 2 0 0;");

        // Titre
        Label titleLabel = new Label("VideoScramble");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Separator sep1 = new Separator();

        // Sélection du fichier d'entrée
        Label inputLabel = new Label("Vidéo d'entrée");
        inputLabel.setStyle("-fx-font-weight: bold;");
        inputField = new TextField();
        inputField.setPromptText("Sélectionner une vidéo...");
        Button browseInputBtn = new Button("Parcourir...");
        browseInputBtn.setMaxWidth(Double.MAX_VALUE);
        browseInputBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Sélectionner la vidéo d'entrée");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.m4v", "*.avi", "*.mov"));

            // Dossier par défaut
            File videosDir = new File("src/videos");
            if (videosDir.exists() && videosDir.isDirectory()) {
                fc.setInitialDirectory(videosDir);
            }

            File file = fc.showOpenDialog(stage);
            if (file != null) {
                inputPath = file.getAbsolutePath();
                inputField.setText(file.getName());
            }
        });

        // Sélection du fichier de sortie
        Label outputLabel = new Label("Vidéo de sortie");
        outputLabel.setStyle("-fx-font-weight: bold;");
        outputField = new TextField();
        outputField.setPromptText("Sélectionner un emplacement...");
        Button browseOutputBtn = new Button("Parcourir...");
        browseOutputBtn.setMaxWidth(Double.MAX_VALUE);
        browseOutputBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Enregistrer la vidéo de sortie");
            fc.getExtensionFilters().clear();
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Vidéo AVI non compressée (recommandé)", "*.avi"),
                    new FileChooser.ExtensionFilter("Vidéo MP4 (ne fonctionne PAS avec clé embarquée)", "*.mp4")
            );
            // Suggérer .avi par défaut
            String suggestedName = "output.avi";
            if (inputPath != null) {
                File inputFile = new File(inputPath);
                String baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
                String modePrefix = modeCombo.getValue().startsWith("Chiffrement") ? "_encrypted" : "_decrypted";
                suggestedName = baseName + modePrefix + ".avi";
            }
            fc.setInitialFileName(suggestedName);

            // Dossier par défaut
            File videosDir = new File("src/videos");
            if (videosDir.exists() && videosDir.isDirectory()) {
                fc.setInitialDirectory(videosDir);
            }

            // Nom suggéré basé sur l'entrée
            suggestedName = "output.mp4";
            if (inputPath != null) {
                File inputFile = new File(inputPath);
                String baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
                String extension = inputFile.getName().substring(inputFile.getName().lastIndexOf("."));
                String modePrefix = modeCombo.getValue().startsWith("Chiffrement") ? "_encrypted" : "_decrypted";
                suggestedName = baseName + modePrefix + extension;
            }
            fc.setInitialFileName(suggestedName);

            File file = fc.showSaveDialog(stage);
            if (file != null) {
                outputPath = file.getAbsolutePath();
                // Vérifier que l'extension est valide
                if (!outputPath.toLowerCase().endsWith(".mp4") &&
                        !outputPath.toLowerCase().endsWith(".m4v") &&
                        !outputPath.toLowerCase().endsWith(".avi")) {
                    outputPath += ".mp4";
                }
                outputField.setText(file.getName());
            }
        });

        Separator sep2 = new Separator();

        // Mode de traitement
        Label modeLabel = new Label("Mode de traitement");
        modeLabel.setStyle("-fx-font-weight: bold;");
        modeCombo = new ComboBox<>();
        modeCombo.setMaxWidth(Double.MAX_VALUE);
        modeCombo.getItems().addAll(
                "Chiffrement",
                "Déchiffrement",
                "Chiffrement avec clé embarquée",
                "Déchiffrement avec clé embarquée",
                "Crack de clé (brute force)"
        );
        modeCombo.setValue("Chiffrement");
        modeCombo.setOnAction(e -> updateModeSettings());

        // Paramètres r et s
        Label keyParamsLabel = new Label("Paramètres de clé");
        keyParamsLabel.setStyle("-fx-font-weight: bold;");

        HBox rBox = new HBox(10);
        Label rLabel = new Label("r (offset):");
        rLabel.setPrefWidth(80);
        rSpinner = new Spinner<>(0, 255, 42);
        rSpinner.setEditable(true);
        rSpinner.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rSpinner, Priority.ALWAYS);
        rBox.getChildren().addAll(rLabel, rSpinner);

        HBox sBox = new HBox(10);
        Label sLabel = new Label("s (step):");
        sLabel.setPrefWidth(80);
        sSpinner = new Spinner<>(0, 127, 17);
        sSpinner.setEditable(true);
        sSpinner.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sSpinner, Priority.ALWAYS);
        sBox.getChildren().addAll(sLabel, sSpinner);

        embedKeyCheck = new CheckBox("Embarquer la clé dans la vidéo");

        Separator sep3 = new Separator();

        // Bouton démarrer
        startBtn = new Button("▶ DÉMARRER LE TRAITEMENT");
        startBtn.setStyle("-fx-font-size: 16px; -fx-padding: 15; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(e -> startProcessing());

        // Bouton arrêter
        stopBtn = new Button("⏸ Arrêter");
        stopBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setDisable(true);
        stopBtn.setOnAction(e -> {
            stopProcessing = true;
            statusLabel.setText("Arrêt en cours...");
        });

        // Bouton fermer
        Button closeBtn = new Button("✖ Fermer");
        closeBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> {
            cleanup();
            stage.close();
        });

        Separator sep4 = new Separator();

        // Informations de statut
        statusLabel = new Label("Prêt à traiter");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        statusLabel.setWrapText(true);

        keyLabel = new Label("Clé: (42, 17)");
        keyLabel.setStyle("-fx-font-size: 11px;");

        fpsLabel = new Label("FPS: --");
        fpsLabel.setStyle("-fx-font-size: 11px;");

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(20);

        // Assemblage du panneau
        panel.getChildren().addAll(
                titleLabel,
                sep1,
                inputLabel,
                inputField,
                browseInputBtn,
                outputLabel,
                outputField,
                browseOutputBtn,
                sep2,
                modeLabel,
                modeCombo,
                keyParamsLabel,
                rBox,
                sBox,
                embedKeyCheck,
                sep3,
                startBtn,
                stopBtn,
                closeBtn,
                sep4,
                statusLabel,
                keyLabel,
                fpsLabel,
                progressBar
        );

        return panel;
    }

    private VBox createVideoPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: #2a2a2a;");

        // Vidéo d'entrée
        Label leftLabel = new Label("Vidéo d'entrée");
        leftLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        leftImageView = new ImageView();
        leftImageView.setFitWidth(500);
        leftImageView.setFitHeight(375);
        leftImageView.setPreserveRatio(true);
        leftImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");

        // Vidéo de sortie
        Label rightLabel = new Label("Vidéo de sortie");
        rightLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        rightImageView = new ImageView();
        rightImageView.setFitWidth(500);
        rightImageView.setFitHeight(375);
        rightImageView.setPreserveRatio(true);
        rightImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");

        panel.getChildren().addAll(leftLabel, leftImageView, rightLabel, rightImageView);
        VBox.setVgrow(panel, Priority.ALWAYS);

        return panel;
    }

    private void updateModeSettings() {
        String selectedMode = modeCombo.getValue();
        boolean needsKey = !selectedMode.contains("embarquée") && !selectedMode.contains("Crack");
        rSpinner.setDisable(!needsKey);
        sSpinner.setDisable(!needsKey);
        embedKeyCheck.setDisable(!selectedMode.equals("Chiffrement"));
    }

    private void startProcessing() {
        // Validation
        if (inputPath == null || inputPath.isEmpty()) {
            showError("Erreur", "Veuillez sélectionner une vidéo d'entrée");
            return;
        }
        if (outputPath == null || outputPath.isEmpty()) {
            showError("Erreur", "Veuillez sélectionner un fichier de sortie");
            return;
        }
        if (!new File(inputPath).exists()) {
            showError("Erreur", "Le fichier d'entrée n'existe pas");
            return;
        }

        // Configuration du mode
        String selectedMode = modeCombo.getValue();
        crackKey = selectedMode.contains("Crack");
        extractKey = selectedMode.equals("Déchiffrement avec clé embarquée");
        embedKey = embedKeyCheck.isSelected();

        if (selectedMode.startsWith("Chiffrement")) {
            mode = "-e";
        } else {
            mode = "-d";
        }

        r = rSpinner.getValue();
        s = sSpinner.getValue();

        // Mise à jour de l'interface
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        stopProcessing = false;
        progressBar.setProgress(0);
        statusLabel.setText("Traitement en cours...");
        keyLabel.setText("Clé (r, s): (" + r + ", " + s + ")");

        // Lancement du traitement
        processingThread = new Thread(() -> {
            try {
                processVideo();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur : " + e.getMessage());
                    showError("Erreur de traitement", e.getMessage());
                    resetInterface();
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

        System.out.println("=== INFORMATIONS VIDÉO ===");
        System.out.println("Dimensions : " + frameWidth + "x" + frameHeight);
        System.out.println("FPS : " + fps);
        System.out.println("Total frames : " + totalFrames);
        System.out.println("Mode : " + mode);
        System.out.println("Clé (r, s) : (" + r + ", " + s + ")");
        System.out.println("Embed key : " + embedKey);
        System.out.println("Extract key : " + extractKey);
        System.out.println("==========================");

        Platform.runLater(() -> {
            fpsLabel.setText("FPS: " + String.format("%.1f", fps) + " | Frames: " + totalFrames);
        });

        // Création du VideoWriter
        VideoWriter writer = null;
        if (outputPath != null) {
            int fourcc;

            // Choisir le codec selon l'extension ET le mode
            if (outputPath.toLowerCase().endsWith(".avi") || embedKey || extractKey) {
                // AVI non compressé pour la clé embarquée
                fourcc = 0;
                System.out.println("Utilisation du codec AVI non compressé (requis pour clé embarquée)");
            } else if (outputPath.toLowerCase().endsWith(".mp4")) {
                // MP4 pour les autres cas
                fourcc = VideoWriter.fourcc('m', 'p', '4', 'v');
                System.out.println("Utilisation du codec MP4");
            } else {
                // Par défaut : AVI
                fourcc = 0;
                System.out.println("Utilisation du codec AVI par défaut");
            }

            Size frameSize = new Size(frameWidth, frameHeight);
            writer = new VideoWriter(outputPath, fourcc, fps, frameSize, true);

            if (!writer.isOpened()) {
                capture.release();
                throw new RuntimeException("Impossible de créer la vidéo de sortie : " + outputPath);
            }
            System.out.println("VideoWriter créé avec succès : " + outputPath);
        }


        // Traitement frame par frame
        Mat frame = new Mat();
        int frameCount = 0;
        int framesWritten = 0;
        long startTime = System.currentTimeMillis();

        // Pour le crack de clé
        if (crackKey) {
            Mat firstValidFrame = findFirstNonBlackFrame(capture);
            if (firstValidFrame != null) {
                Platform.runLater(() -> statusLabel.setText("Recherche de la clé par force brute..."));

                try {
                    int[] crackedKey = TrouveCleBruteForce.bruteForce(firstValidFrame);
                    r = crackedKey[0];
                    s = crackedKey[1];
                    System.out.println("Clé crackée : r=" + r + ", s=" + s);
                    Platform.runLater(() -> {
                        keyLabel.setText("Clé trouvée (r, s): (" + r + ", " + s + ")");
                        statusLabel.setText("Clé crackée ! Déchiffrement en cours...");
                    });
                } catch (Exception e) {
                    System.err.println("Erreur lors du crack de clé : " + e.getMessage());
                    e.printStackTrace();
                }

                firstValidFrame.release();
            }
            capture.release();
            capture = new VideoCapture(inputPath);

            // Passer en mode déchiffrement après le crack
            mode = "-d";
        }

        while (!stopProcessing && capture.read(frame) && !frame.empty()) {
            frameCount++;

            int currentR = r;
            int currentS = s;

            // Traitement de la frame
            Mat processed = null;
            try {
                if (mode.startsWith("-e")) {
                    // === CHIFFREMENT ===
                    // 1. Chiffrer la frame
                    processed = PermutationLignes.scrambleFrame(frame, currentR, currentS);

                    // 2. Embarquer la clé APRÈS le chiffrement (dans TOUTES les frames si embedKey activé)
                    if (embedKey) {
                        KeyEmbedder.embedKeyInPixel(processed, currentR, currentS);
                        if (frameCount == 1) {
                            System.out.println("Mode embarquement activé - Clé (r=" + currentR + ", s=" + currentS + ") embarquée dans toutes les frames");
                        }

                        // Test immédiat d'extraction sur la première frame
                        if (frameCount == 1) {
                            int[] testExtract = KeyEmbedder.extractKeyFromPixel(processed);
                            System.out.println("TEST embarquement frame 1 - Clé ré-extraite : r=" + testExtract[0] + ", s=" + testExtract[1]);
                            if (testExtract[0] != currentR || testExtract[1] != currentS) {
                                System.err.println("ERREUR : La clé embarquée ne correspond pas !");
                            }
                        }
                    }
                } else {
                    // === DÉCHIFFREMENT ===
                    // 1. Extraire la clé AVANT de déchiffrer (si mode avec clé embarquée)
                    if (extractKey) {
                        int[] embeddedKey = KeyEmbedder.extractKeyFromPixel(frame);
                        currentR = embeddedKey[0];
                        currentS = embeddedKey[1];

                        if (frameCount == 1) {
                            r = currentR;  // Sauvegarder pour affichage
                            s = currentS;
                            System.out.println("Clé extraite de la frame 1 : r=" + currentR + ", s=" + currentS);
                            int finalR = currentR;
                            int finalS = currentS;
                            Platform.runLater(() -> {
                                keyLabel.setText("Clé extraite (r, s): (" + finalR + ", " + finalS + ")");
                            });
                        }

                        // Debug : vérifier la clé extraite régulièrement
                        if (frameCount <= 5 || frameCount % 100 == 0) {
                            System.out.println("Frame " + frameCount + " - Clé extraite : r=" + currentR + ", s=" + currentS);
                        }

                        // Vérifier si la clé est valide
                        if (!KeyEmbedder.isValidKey(embeddedKey)) {
                            System.err.println("ATTENTION Frame " + frameCount + " : Clé invalide ! r=" + currentR + ", s=" + currentS);
                        }
                    }

                    // 2. Déchiffrer avec la clé (extraite ou fournie)
                    processed = PermutationLignes.unscrambleFrame(frame, currentR, currentS);
                }

                // Vérifier que le traitement a réussi
                if (processed == null || processed.empty()) {
                    System.err.println("ERREUR : Frame traitée est vide à la frame " + frameCount);
                    continue;
                }

                // Vérifier les dimensions
                if (writer != null) {
                    if (processed.rows() == frameHeight && processed.cols() == frameWidth) {
                        writer.write(processed);
                        framesWritten++;
                    } else {
                        System.err.println("ATTENTION : Dimensions incorrectes à la frame " + frameCount +
                                " ! " + processed.cols() + "x" + processed.rows() +
                                " vs " + frameWidth + "x" + frameHeight);
                    }
                }

                // Affichage
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

            } catch (Exception e) {
                System.err.println("Erreur au traitement de la frame " + frameCount + " : " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (processed != null) {
                    processed.release();
                }
            }

            if (frameCount % 30 == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                double currentFps = (frameCount * 1000.0) / elapsed;
                int finalFrameCount1 = frameCount;
                Platform.runLater(() -> {
                    fpsLabel.setText(String.format("FPS: %.1f | Frame: %d/%d", currentFps, finalFrameCount1, totalFrames));
                });
            }
        }

        System.out.println("=== RÉSULTAT ===");
        System.out.println("Frames lues : " + frameCount);
        System.out.println("Frames écrites : " + framesWritten);
        System.out.println("=================");

        // Libération des ressources dans le bon ordre
        frame.release();
        capture.release();

        if (writer != null) {
            System.out.println("Libération du VideoWriter...");
            writer.release();
            System.out.println("VideoWriter libéré.");
        }

        int finalFrameCount = frameCount;
        int finalFramesWritten = framesWritten;
        Platform.runLater(() -> {
            statusLabel.setText("Traitement terminé ! " + finalFrameCount + " frames traitées, " +
                    finalFramesWritten + " frames écrites.");
            progressBar.setProgress(1.0);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Traitement terminé");
            alert.setHeaderText("Succès !");
            alert.setContentText(finalFrameCount + " frames ont été traitées.\n" +
                    finalFramesWritten + " frames écrites.\n" +
                    "Fichier de sortie : " + outputPath);
            alert.setOnHidden(evt -> resetInterface());
            alert.showAndWait();
        });
    }

    private void resetInterface() {
        Platform.runLater(() -> {
            // Réinitialiser les boutons
            startBtn.setDisable(false);
            stopBtn.setDisable(true);

            // Réinitialiser les images
            leftImageView.setImage(null);
            rightImageView.setImage(null);

            // Réinitialiser les champs
            inputField.clear();
            outputField.clear();

            // Réinitialiser les chemins
            inputPath = null;
            outputPath = null;

            // Réinitialiser la progression
            progressBar.setProgress(0);

            // Réinitialiser les labels
            statusLabel.setText("Prêt à traiter");
            keyLabel.setText("Clé: (42, 17)");
            fpsLabel.setText("FPS: --");

            // Réinitialiser les paramètres
            rSpinner.getValueFactory().setValue(42);
            sSpinner.getValueFactory().setValue(17);
            modeCombo.setValue("Chiffrement");
            embedKeyCheck.setSelected(false);

            // Réactiver les contrôles
            updateModeSettings();

            System.out.println("Interface réinitialisée");
        });
    }

    private Mat findFirstNonBlackFrame(VideoCapture capture) {
        Mat frame = new Mat();
        int count = 0;
        while (count < 100 && capture.read(frame) && !frame.empty()) {
            double sum = 0;
            byte[] data = new byte[(int) frame.total() * frame.channels()];
            frame.get(0, 0, data);
            for (byte b : data) {
                sum += (b & 0xFF);
            }
            double avg = sum / data.length;
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
