package org.example;

import javafx.scene.image.*;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

public class OpenCVUtils {

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
