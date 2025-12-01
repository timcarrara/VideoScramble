package org.example;

import org.opencv.core.Mat;

public class VideoScrambler {

    public static Mat scramble(Mat input) {
        Mat output = input.clone();
        int height = input.rows();

        for (int i = 0; i < height; i++) {
            int newIndex = (i * 7 + 13) % height; // mélange simple
            input.row(i).copyTo(output.row(newIndex));
        }

        return output;
    }
}
