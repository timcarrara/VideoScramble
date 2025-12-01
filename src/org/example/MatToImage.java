package org.example;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class MatToImage {

    public static Image matToImage(Mat frame) {
        if (frame == null || frame.empty()) return null;
        return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
    }

    public static BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        BufferedImage img;

        if (channels == 1) {
            img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            mat.get(0, 0, data);
        } else {
            img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            mat.get(0, 0, data);

            // BGR → RGB
            for (int i = 0; i < data.length; i += 3) {
                byte tmp = data[i];
                data[i] = data[i + 2];
                data[i + 2] = tmp;
            }
        }

        return img;
    }
}
