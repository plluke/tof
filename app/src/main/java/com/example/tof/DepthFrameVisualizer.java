package com.example.tof;

import android.graphics.Bitmap;

public interface DepthFrameVisualizer {
    void onRawDataAvailable(Bitmap bitmap);
    void onNoiseReductionAvailable(Bitmap bitmap);
    void onMovingAverageAvailable(Bitmap bitmap);
    void onBlurredMovingAverageAvailable(Bitmap bitmap);
}
