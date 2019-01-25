package com.example.scorecreator;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;


/**
 * MusicRecord2- used for audio processing
 */
public class MusicRecord2 extends AppCompatActivity {
    private static final int RECORDER_SAMPLERATE = 192000;
    // private static final int RECORDER_SAMPLERATE = 8192;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    // Potential buffer sizes (in bytes)
    // Will be able to match device specifications
    int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    // Get buffer size by multiplying following
    int BufferElements2Rec = 128; // want to play 2048 (2K) since 2 bytes we use only 1024
    // int BytesPerElement = 2; // 2 bytes in 16bit format

    // Audio Visualizer
    Visualizer mVisualizer;
    WaveformView waveformView;
    // double[] real = new double[BufferElements2Rec];
    byte[] bData = new byte[BufferElements2Rec];

    // User buttons
    Button start, stop;
    private boolean started = false;

    // Tag for identifying in debugging
    final String LOG_TAG = "Recording";

    // Frequency
    private int samplesperSnip = 5001;
    private int[] freqData = new int[samplesperSnip];
    private int counter = 0;
    private String note = "";
    TextView noteText;
    private String prediction = "Prediction";
    TextView predictionText;
    private boolean madePrediction = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_record2);

        // Buttons
        start = (Button) findViewById(R.id.START_RECORDING);
        stop = (Button) findViewById(R.id.STOP_RECORDING);
        noteText = (TextView) findViewById(R.id.NoteText);
        predictionText = (TextView) findViewById(R.id.PredictionText);
        predictionText.setText(prediction);
        // Visualizer
        waveformView = (WaveformView) findViewById(R.id.waveform_view);

    }

    // Setup visualizer
    private void setupVisualizer() {
        Log.d(LOG_TAG, recorder.getAudioSessionId() + " Session ID");
        mVisualizer = new Visualizer(0);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    public void onWaveFormDataCapture(Visualizer visualizer,
                                                      byte[] bytes, int samplingRate) {

                        /*byte[] waveform = new byte[real.length / 2];
                        for (int i = 0; i < real.length / 2; i++)
                            waveform[i] = (byte) real[i];*/
                        byte[] waveform = new byte[bData.length / 2];

                        byte total = 0; // Getting the average
                        for (int i = 0; i < bData.length / 2; i++)
                            total += bData[i];
                        for(int i = 0; i < bData.length / 2; i++)
                            waveform[i]= bData[i];
                            /*if(bData[i] < 0) waveform[i] = 0;
                            else waveform[i]= bData[i];*/

                        waveformView.updateVisualizer(waveform);

                    }

                    public void onFftDataCapture(Visualizer visualizer,
                                                 byte[] bytes, int samplingRate) {
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, false);
    }

    /**
     * To handle recording process
     *
     * @param view
     */
    public void startRecording(View view) {
        if (!started) {
            started = true;

            Log.d(LOG_TAG, "Started recording");

            // Setup audio capture
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, bufferSize);


            recorder.startRecording();

            setupVisualizer();
            mVisualizer.setEnabled(true);

            isRecording = true;
            recordingThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        processing();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, "AudioRecorder Thread");
            recordingThread.start();
        }
    }

    /**
     * Process sound information
     */
    private void processing() throws IOException {
        Log.d(LOG_TAG, "Processing");

        while (isRecording) {
            // gets the voice output from microphone
            Log.d(LOG_TAG, "Is recording");
            String bDataString = "";
            bData = new byte[BufferElements2Rec];
            recorder.read(bData, 0, BufferElements2Rec);
            for (int i = 0; i < bData.length; i++)
                bDataString += bData[i];

            // FFT Data
            double[] real = new double[bData.length];
            double[] imag = new double[bData.length];
            for (int i = 0; i < real.length; i++) {
                real[i] = bData[i];
                imag[i] = 0;
            }
            transform(real, imag);
            String fftString = "";
            double max = 0;
            int max_bin = 0;
            for (int i = 1; i < real.length / 2; i++) {
                if (Math.sqrt(Math.pow(real[i], 2) + Math.pow(imag[i], 2)) > max) {
                    max = Math.sqrt(Math.pow(real[i], 2) + Math.pow(imag[i], 2));
                    max_bin = i;
                }
                /*if (imag[i] > max)
                    max = imag[i];*/
                fftString += real[i];
            }
            // Log.d(LOG_TAG, max + "");
            // Log.d(LOG_TAG, fftString + "");
            // Log.d(LOG_TAG, real[max_bin] + "");

            double frequency = 2 * max_bin * RECORDER_SAMPLERATE / BufferElements2Rec;

            Log.d(LOG_TAG, frequency + "");
            note = determineNote(frequency);
            // note = determineNote(real[max_bin]);
            Log.d(LOG_TAG, note);

            // 56448 samples saved
            if(counter != samplesperSnip - 1) {
                freqData[counter] = (int)frequency;
                counter++;
                madePrediction = false;
            } else {
                Predict predict = new Predict(freqData, this);
                prediction = predict.predict();
                Log.d(LOG_TAG, "Prediction " + prediction);
                madePrediction = true;
                freqData = new int[samplesperSnip];
                counter = 0;
            }
            Log.d(LOG_TAG, "Counter + " + counter);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    noteText.setText(note);
                    if(madePrediction) predictionText.setText(prediction);
                }
            });

        }
    }


    /**
     * To handle recording coming to a stop
     *
     * @param view
     */
    public void stopRecording(View view) {
        Log.d(LOG_TAG, "Stop Recording");
        started = false;

        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            mVisualizer.setEnabled(false);
        }
    }

    // FFT
    /*
     * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
	 * The vector can have any length. This is a wrapper function.
	 */

    private String determineNote(double freq) {
        // Create table
        double aFour = 440.0;
        int tableLength = 12 * 15;
        double[] table = new double[tableLength];
        for (int i = 0; i < table.length; i++) {
            table[i] = aFour * Math.pow(Math.pow(2.0, 1 / 12.0), i - 57.0);
        }
        double diff = table[table.length - 1];
        int currentNote = 0;
        for (int i = 0; i < table.length; i++) {
            if (Math.abs(freq - table[i]) < diff) {
                diff = Math.abs(freq - table[i]);
                currentNote = i;
            }
        }
        Log.d(LOG_TAG, currentNote + " abc " + diff);

        String note = "";

        String[] noteTable = new String[table.length];
        for (int i = 0; i < noteTable.length; i += 12) {
            noteTable[i] = "C";
            noteTable[i + 1] = "C#";
            noteTable[i + 2] = "D";
            noteTable[i + 3] = "D#";
            noteTable[i + 4] = "E";
            noteTable[i + 5] = "F";
            noteTable[i + 6] = "F#";
            noteTable[i + 7] = "G";
            noteTable[i + 8] = "G#";
            noteTable[i + 9] = "A";
            noteTable[i + 10] = "A#";
            noteTable[i + 11] = "B";
        }
        note = noteTable[currentNote];
        return note;
    }

    public static void transform(double[] real, double[] imag) {
        int n = real.length;
        if (n != imag.length)
            throw new IllegalArgumentException("Mismatched lengths");
        if (n == 0)
            return;
        else if ((n & (n - 1)) == 0)  // Is power of 2
            transformRadix2(real, imag);
        else  // More complicated algorithm for arbitrary sizes
            transformBluestein(real, imag);
    }

    /*
     * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
	 * The vector's length must be a power of 2. Uses the Cooley-Tukey decimation-in-time radix-2 algorithm.
	 */
    public static void transformRadix2(double[] real, double[] imag) {
        // Length variables
        int n = real.length;
        if (n != imag.length)
            throw new IllegalArgumentException("Mismatched lengths");
        int levels = 31 - Integer.numberOfLeadingZeros(n);  // Equal to floor(log2(n))
        if (1 << levels != n)
            throw new IllegalArgumentException("Length is not a power of 2");

        // Trigonometric tables
        double[] cosTable = new double[n / 2];
        double[] sinTable = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            cosTable[i] = Math.cos(2 * Math.PI * i / n);
            sinTable[i] = Math.sin(2 * Math.PI * i / n);
        }

        // Bit-reversed addressing permutation
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - levels);
            if (j > i) {
                double temp = real[i];
                real[i] = real[j];
                real[j] = temp;
                temp = imag[i];
                imag[i] = imag[j];
                imag[j] = temp;
            }
        }

        // Cooley-Tukey decimation-in-time radix-2 FFT
        for (int size = 2; size <= n; size *= 2) {
            int halfsize = size / 2;
            int tablestep = n / size;
            for (int i = 0; i < n; i += size) {
                for (int j = i, k = 0; j < i + halfsize; j++, k += tablestep) {
                    int l = j + halfsize;
                    double tpre = real[l] * cosTable[k] + imag[l] * sinTable[k];
                    double tpim = -real[l] * sinTable[k] + imag[l] * cosTable[k];
                    real[l] = real[j] - tpre;
                    imag[l] = imag[j] - tpim;
                    real[j] += tpre;
                    imag[j] += tpim;
                }
            }
            if (size == n)  // Prevent overflow in 'size *= 2'
                break;
        }
    }


    /*
     * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
     * The vector can have any length. This requires the convolution function, which in turn requires the radix-2 FFT function.
     * Uses Bluestein's chirp z-transform algorithm.
     */
    public static void transformBluestein(double[] real, double[] imag) {
        // Find a power-of-2 convolution length m such that m >= n * 2 + 1
        int n = real.length;
        if (n != imag.length)
            throw new IllegalArgumentException("Mismatched lengths");
        if (n >= 0x20000000)
            throw new IllegalArgumentException("Array too large");
        int m = Integer.highestOneBit(n) * 4;

        // Trignometric tables
        double[] cosTable = new double[n];
        double[] sinTable = new double[n];
        for (int i = 0; i < n; i++) {
            int j = (int) ((long) i * i % (n * 2));  // This is more accurate than j = i * i
            cosTable[i] = Math.cos(Math.PI * j / n);
            sinTable[i] = Math.sin(Math.PI * j / n);
        }

        // Temporary vectors and preprocessing
        double[] areal = new double[m];
        double[] aimag = new double[m];
        for (int i = 0; i < n; i++) {
            areal[i] = real[i] * cosTable[i] + imag[i] * sinTable[i];
            aimag[i] = -real[i] * sinTable[i] + imag[i] * cosTable[i];
        }
        double[] breal = new double[m];
        double[] bimag = new double[m];
        breal[0] = cosTable[0];
        bimag[0] = sinTable[0];
        for (int i = 1; i < n; i++) {
            breal[i] = breal[m - i] = cosTable[i];
            bimag[i] = bimag[m - i] = sinTable[i];
        }

        // Convolution
        double[] creal = new double[m];
        double[] cimag = new double[m];
        convolve(areal, aimag, breal, bimag, creal, cimag);

        // Postprocessing
        for (int i = 0; i < n; i++) {
            real[i] = creal[i] * cosTable[i] + cimag[i] * sinTable[i];
            imag[i] = -creal[i] * sinTable[i] + cimag[i] * cosTable[i];
        }
    }

    /*
     * Computes the circular convolution of the given complex vectors. Each vector's length must be the same.
     */
    public static void convolve(double[] xreal, double[] ximag,
                                double[] yreal, double[] yimag, double[] outreal, double[] outimag) {

        int n = xreal.length;
        if (n != ximag.length || n != yreal.length || n != yimag.length
                || n != outreal.length || n != outimag.length)
            throw new IllegalArgumentException("Mismatched lengths");

        xreal = xreal.clone();
        ximag = ximag.clone();
        yreal = yreal.clone();
        yimag = yimag.clone();
        transform(xreal, ximag);
        transform(yreal, yimag);

        for (int i = 0; i < n; i++) {
            double temp = xreal[i] * yreal[i] - ximag[i] * yimag[i];
            ximag[i] = ximag[i] * yreal[i] + xreal[i] * yimag[i];
            xreal[i] = temp;
        }
        inverseTransform(xreal, ximag);

        for (int i = 0; i < n; i++) {  // Scaling (because this FFT implementation omits it)
            outreal[i] = xreal[i] / n;
            outimag[i] = ximag[i] / n;
        }
    }

    /*
     * Computes the inverse discrete Fourier transform (IDFT) of the given complex vector, storing the result back into the vector.
	 * The vector can have any length. This is a wrapper function. This transform does not perform scaling, so the inverse is not a true inverse.
	 */
    public static void inverseTransform(double[] real, double[] imag) {
        transform(imag, real);
    }

}
