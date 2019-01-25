package com.example.scorecreator;

import android.graphics.Canvas;

/**
 * Created by micha on 1/6/2018.
 */

interface WaveformRenderer {
    void render(Canvas canvas, byte[] waveform);
}
