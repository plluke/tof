package com.example.tof;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.nio.ShortBuffer;

public class DepthFrameAvailableListener implements ImageReader.OnImageAvailableListener {
    private static final String TAG = DepthFrameAvailableListener.class.getSimpleName();

    public static int WIDTH = 240;
    public static int HEIGHT = 180;

    private static float RANGE_MIN = 200.0f;
    private static float RANGE_MAX = 1600.0f;
    private static float CONFIDENCE_FILTER = 0.1f;

    private DepthFrameVisualizer depthFrameVisualizer;
    private int[] rawMask;
    private int[] noiseReduceMask;
    private int[] averagedMask;
    private int[] averagedMaskP2;
    private int[] blurredAverage;

    public DepthFrameAvailableListener(DepthFrameVisualizer depthFrameVisualizer) {
        this.depthFrameVisualizer = depthFrameVisualizer;

        int size = WIDTH * HEIGHT;
        rawMask = new int[size];
        noiseReduceMask = new int[size];
        averagedMask = new int[size];
        averagedMaskP2 = new int[size];
        blurredAverage = new int[size];
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        try {
            Image image = reader.acquireNextImage();
            if (image != null && image.getFormat() == ImageFormat.DEPTH16) {
                processImage(image);
                publishRawData();
                publishNoiseReduction();
                publishMovingAverage();
                publishBlurredMovingAverage();
            }
            image.close();
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to acquireNextImage: " + e.getMessage());
        }
    }

    private void publishRawData() {
        if (depthFrameVisualizer != null) {
            Bitmap bitmap = convertToRGBBitmap(rawMask);
            depthFrameVisualizer.onRawDataAvailable(bitmap);
            bitmap.recycle();
        }
    }

    private void publishNoiseReduction() {
        if (depthFrameVisualizer != null) {
            Bitmap bitmap = convertToRGBBitmap(noiseReduceMask);
            depthFrameVisualizer.onNoiseReductionAvailable(bitmap);
            bitmap.recycle();
        }
    }

    private void publishMovingAverage() {
        if (depthFrameVisualizer != null) {
            Bitmap bitmap = convertToRGBBitmap(averagedMask);
            depthFrameVisualizer.onMovingAverageAvailable(bitmap);
            bitmap.recycle();
        }
    }

    private void publishBlurredMovingAverage() {
        if (depthFrameVisualizer != null) {
            Bitmap bitmap = convertToRGBBitmap(blurredAverage);
            depthFrameVisualizer.onBlurredMovingAverageAvailable(bitmap);
            bitmap.recycle();
        }
    }

    private void processImage(Image image) {
        ShortBuffer shortDepthBuffer = image.getPlanes()[0].getBuffer().asShortBuffer();
        int[] mask = new int[WIDTH * HEIGHT];
        int[] noiseReducedMask = new int[WIDTH * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = y * WIDTH + x;
                short depthSample = shortDepthBuffer.get(index);
                int newValue = extractRange(depthSample, CONFIDENCE_FILTER);
                // Store value in the rawMask for visualization
                rawMask[index] = newValue;

                int p1Value = averagedMask[index];
                int p2Value = averagedMaskP2[index];
                int avgValue = (newValue + p1Value + p2Value) / 3;
                if (p1Value < 0 || p2Value < 0 || newValue < 0) {
                    Log.d("TAG", "WHAT");
                }
                // Store the new moving average temporarily
                mask[index] = avgValue;
            }
        }
        // Produce a noise reduced version of the raw mask for visualization
        System.arraycopy(rawMask, 0, noiseReducedMask, 0, rawMask.length);
        noiseReduceMask = FastBlur.boxBlur(noiseReducedMask, WIDTH, HEIGHT, 1);

        // Remember the last two frames for moving average
        averagedMaskP2 = averagedMask;
        averagedMask = mask;

        // Produce a blurred version of the latest moving average result
        System.arraycopy(averagedMask, 0, blurredAverage, 0, averagedMask.length);
        blurredAverage = FastBlur.boxBlur(blurredAverage, WIDTH, HEIGHT, 1);
    }

    private int extractRange(short sample, float confidenceFilter) {
        int depthRange = (short) (sample & 0x1FFF);
        int depthConfidence = (short) ((sample >> 13) & 0x7);
        float depthPercentage = depthConfidence == 0 ? 1.f : (depthConfidence - 1) / 7.f;
        if (depthPercentage > confidenceFilter) {
            return normalizeRange(depthRange);
        } else {
            return 0;
        }
    }

    private int normalizeRange(int range) {
        float normalized = (float)range - RANGE_MIN;
        // Clamp to min/max
        normalized = Math.max(RANGE_MIN, normalized);
        normalized = Math.min(RANGE_MAX, normalized);
        // Normalize to 0 to 255
        normalized = normalized - RANGE_MIN;
        normalized = normalized / (RANGE_MAX - RANGE_MIN) * 255;
        return (int)normalized;
    }

    private Bitmap convertToRGBBitmap(int[] mask) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_4444);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = y * WIDTH + x;
                bitmap.setPixel(x, y, Color.argb(255, 0, mask[index],0));
            }
        }
        return bitmap;
    }
}