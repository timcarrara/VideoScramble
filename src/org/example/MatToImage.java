package org.example;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class MatToImage {

    public static Image matToImage(Mat frame) {
        if (frame == null || frame.empty()) return null;
        return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
    }

    public static BufferedImage matToBufferedImage(Mat mat) {
        if (mat.channels() == 1) {
            BufferedImage img = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_BYTE_GRAY);
            byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            mat.get(0, 0, data);
            return img;
        } else {
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);
            BufferedImage img = new BufferedImage(rgbMat.cols(), rgbMat.rows(), BufferedImage.TYPE_3BYTE_BGR);
            byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            rgbMat.get(0, 0, data);
            return img;
        }
    }
}
