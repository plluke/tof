package com.example.tof;

public class FastBlur {

    // Fast Gaussian blur
    public static int[] gaussBlur(int[] input, int width, int height, int radius) {
        int[] output = new int[input.length];
        gaussBlurFast(input, output, width, height, radius);
        return output;
    }

    // This is a fast box blur
    public static int[] boxBlur(int[] input, int width, int height, int radius) {
        int[] output = new int[input.length];
        gaussBlurRough(input, output, width, height, radius);
        return output;
    }


    private static void gaussBlurRough (int[] scl, int[] tcl, int w, int h, int r) {
        int[] bxs = boxesForGauss(r, 1);
        boxBlurFast(scl, tcl, w, h, bxs[0]);
    }

    private static void gaussBlurFast(int[] scl, int[] tcl, int w, int h, int r) {
        int[] bxs = boxesForGauss(r, 3);
        boxBlurFast(scl, tcl, w, h, (bxs[0] - 1) / 2);
        boxBlurFast(tcl, scl, w, h, (bxs[1] - 1) / 2);
        boxBlurFast(scl, tcl, w, h, (bxs[2] - 1) / 2);
    }

    private static int[] boxesForGauss(int sigma, int n) { // standard deviation, number of boxes
        double wIdeal = Math.sqrt((12 * sigma * sigma / n) + 1);  // Ideal averaging filter width
        int wl = (int)Math.floor(wIdeal);
        if(wl % 2==0) {
            wl--;
        }
        int wu = wl+2;

        int mIdeal = (12 * sigma * sigma - n * wl * wl - 4 * n * wl - 3 * n)/(-4 * wl - 4);
        int m = (int)Math.round(mIdeal);
        // int sigmaActual = Math.sqrt( (m*wl*wl + (n-m)*wu*wu - n)/12 );

        int[] sizes = new int[n];
        for(int i=0; i < n; i++) {
            sizes[i] = (i < m ? wl : wu);
        }
        return sizes;
    }

    private static void boxBlurFast(int[] scl, int[] tcl, int w, int h, int r) {
        for (int i = 0; i < scl.length; i++) {
            tcl[i] = scl[i];
        }
        boxBlurHorizontal(tcl, scl, w, h, r);
        boxBlurVertical(scl, tcl, w, h, r);
    }

    // This function is annotated with what each line is supposed to do. This is the horizontal
    // version, with the function beneath just the vertical version of this.
    private static void boxBlurHorizontal(int[] scl, int[] tcl, int w, int h, int r) {
        // radius range on either side of a pixel + the pixel itself
        float iarr = 1 / ((float)r + (float)r + 1);

        for(int i = 0; i < h; i++) {
            int ti = i * w; //pixel index; will traverse the width of the image for each loop
            // of the parent "for loop"
            int li = ti; // trailing pixel index
            int ri = ti + r; //pixel index of the furthest reach of the radius

            int fv = scl[ti]; // first pixel value of the row
            int lv = scl[ti + w - 1]; // last pixel value in the row
            // create a "value accumulator" - we will be calculating the average of pixels
            // surrounding each one - is faster to add newest value, remove oldest, and
            // then average. This initial value is for pixels outside image bounds
            int val = (r + 1) * fv;

            // for length of radius, accumulate the total value of all pixels from current pixel
            // index and record it into the target channel first pixel
            for(int j = 0; j < r; j++) {
                val += scl[ti + j];
            }
            // for the next $boxRadius pixels in the row, record pixel value of average of all
            // pixels within the radius and save average into target channel
            for(int j = 0 ; j <= r; j++) {
                val += scl[ri++] - fv;
                tcl[ti++] = round(val, iarr);
            }
            // now that we've completely removed the overflow pixels from the value accumulator,
            // continue on, adding new values, removing old ones, and averaging the
            // accumulated value
            for(int j = r + 1; j < w - r; j++) {
                val += scl[ri++] - scl[li++];
                tcl[ti++] = round(val, iarr);
            }
            // finish off the row of pixels, duplicating the edge pixel instead of going out of image bounds
            for(int j = w - r; j < w ; j++) {
                val += lv - scl[li++];
                tcl[ti++] = round(val, iarr);
            }
        }
    }

    private static void boxBlurVertical(int[] scl, int[] tcl, int w, int h, int r) {
        float iarr = 1 / ((float)r + (float)r + 1);
        for(int i = 0; i < w; i++) {
            int ti = i;
            int li = ti;
            int ri = ti + r * w;
            int fv = scl[ti];
            int lv = scl[ti + w * (h - 1)];
            int val = (r + 1) * fv;
            for(int j = 0; j < r; j++) {
                val += scl[ti + j * w];
            }
            for(int j = 0  ; j <= r; j++) {
                val += scl[ri] - fv;
                tcl[ti] = round(val, iarr);
                ri += w;
                ti += w;
            }
            for(int j = r + 1; j < h - r; j++) {
                val += scl[ri] - scl[li];
                tcl[ti] = round(val, iarr);
                li += w;
                ri += w;
                ti += w;
            }
            for(int j = h - r; j < h; j++) {
                val += lv - scl[li];
                tcl[ti] = round(val, iarr);
                li += w;
                ti += w;
            }
        }
    }

    private static int round(int val, float iarr) {
        return Math.round(val * iarr);
    }
}
