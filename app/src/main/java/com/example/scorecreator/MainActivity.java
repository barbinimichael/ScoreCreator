package com.example.scorecreator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main activity- application menu
 */
public class MainActivity extends AppCompatActivity {

    // Permission requirements for audio compatibility
    private String perms[] = {"android.permission.RECORD_AUDIO", "android.permission.MODIFY_AUDIO_SETTINGS"};
    final private int permsRequestCode = 123;
    private boolean permissionGranted = false;

    // Note images
    ImageView image;
    ImageView image1;
    ImageView image2;
    ImageView image3;

    // Start button
    Button startButton;
    float originalTextSize;

    // Tag for identifying in debugging
    final String LOG_TAG = "Anim";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing "notes" images, will be used for animation process
        image = (ImageView) findViewById(R.id.note);
        image1 = (ImageView) findViewById(R.id.note1);
        image2 = (ImageView) findViewById(R.id.note2);
        image3 = (ImageView) findViewById(R.id.note3);

        // Initializing start button
        startButton = (Button) (findViewById(R.id.button));
        originalTextSize = startButton.getTextSize();

        // Requesting audio permission
        checkPermission();
        ActivityCompat.requestPermissions(this, perms, permsRequestCode);

        // Animating
        animation(image);
        animation(image1);
        animation(image2);
        animation(image3);

        // Transposing images randomly at time intervals
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                setNote(image);
                setNote(image1);
                setNote(image2);
                setNote(image3);
            }
        }, 0, 10000);
    }

    /**
     * Animation for "notes"
     *
     * @param image ("notes")
     */
    private void animation(ImageView image) {
        Animation animation =
                AnimationUtils.loadAnimation(getApplicationContext(), R.anim.note_float);
        image.startAnimation(animation);
        animation.setRepeatCount(Animation.INFINITE);
    }

    /**
     * Random transformation for "notes"
     *
     * @param image ("notes")
     */
    private void setNote(ImageView image) {

        // In order to get dimensions of the screen- will be used for determining bounds of transformation
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        // Log.d(LOG_TAG, "Screen dimensions: " + width + ", " + height);

        // Getting random point for transformation
        Random rand = new Random();
        float x = rand.nextInt(width - image.getWidth()); // new x coordinate
        float y = rand.nextInt((int) (height)) + height / 2; // new y coordinate

        // Setting new coordinates
        image.setX(x);
        image.setY(y);
        // Log.d(LOG_TAG, "New coordinates " + x + ", " + y);
    }

    /**
     * To move to the record activity- MusicRecord2
     *
     * @param view
     */
    public void record(View view) {
        checkPermission();
        if (permissionGranted) {
            // Resetting button
            startButton.setText("Start");

            Intent intent = new Intent(this, MusicRecord2.class);
            startActivity(intent);
        } else {
            // Notifying user that not functional without audio permission
            startButton.setTextSize(originalTextSize / 8);
            startButton.setText("Must aquire audio permission");
        }
    }

    /**
     * Checking for if device has already granted audio permission
     */
    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) +
                ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, perms, permsRequestCode);
            permissionGranted = false;
        } else {
            permissionGranted = true;
        }
    }

    public void setPermissionGranted() {
        startButton.setTextSize(originalTextSize);
        startButton.setText("Start");
    }

}
