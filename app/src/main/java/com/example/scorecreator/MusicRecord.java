package com.example.scorecreator;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.media.AudioRecord;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

/**
 * Created by micha on 10/23/2017.
 */

public class MusicRecord extends AppCompatActivity {
    AudioRecord recorder;
    final private int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    final private int SAMPLE_RATE = 44100;
    final private int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    final private int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    short[] audioBuffer = new short[bufferSize / 2];

    AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize);
    int numberOfShort = 0;
    int shortsRead = 0;

    boolean mShouldContinue;

    TextView noteText;

    final private String LOG_TAG = "BOI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_record);
        noteText = (TextView) findViewById(R.id.noteView);
        recordMusic();
    }

    public void record() {
        recorder.startRecording();
        while(true) {
            int i = 0;

            (new Handler()).postDelayed(new Runnable() {
                public void run() {
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                    noteText.setText(Integer.toString(numberOfShort));
                }
            }, 5000);
            i++;
            if(i > 10) break;
        }
    }

    public void recordSound() {
        boolean running = true;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
        Log.d(LOG_TAG, "Initialized");
        record.startRecording();
        Log.d(LOG_TAG, "Started Recording");

        record.read(audioBuffer, 0, audioBuffer.length);
        numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
        shortsRead += numberOfShort;
        Log.d(LOG_TAG, "recordSound " + shortsRead);

        Log.d(LOG_TAG, "Lol");
    }

    class getInfo extends Thread {
        public getInfo() {

        }

        public void run() {
            Log.d(LOG_TAG, "" + shortsRead);
        }
    }

    public void recordMusic() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

                bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                short[] audioBuffer = new short[bufferSize / 2];

                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

                if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(LOG_TAG, "Audio Record can't initialize!");
                    return;
                }

                record.startRecording();

                Log.d(LOG_TAG, "Start recording");

                long shortsRead = 0;
                while (mShouldContinue) {
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                    shortsRead += numberOfShort;
                    process(numberOfShort);
                    // Do something with the audioBuffer
                    Log.d(LOG_TAG, "It is recording rn");
                }

                record.stop();
                record.release();

                Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
            }
        }).start();

    }

    private void process(int numberOfShort) {
        noteText.setText(Integer.toString(numberOfShort));
    }

    public void stopMusic(View view) {
        //noteText.setText("Stopped");
        recorder.stop();
        recorder.release();
    }
}



