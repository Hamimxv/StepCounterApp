package com.example.stepcounter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int SENSOR_PERMISSION_CODE = 1;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private TextView stepCountTextView;
    private TextView goalTextView;
    private Chronometer chronometer;
    private ProgressBar progressBar;
    private long elapsedTime = 0;
    private int stepCount = 0;
    private int goalDistance = 50; // in kilometers

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check and request permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, SENSOR_PERMISSION_CODE);
        } else {
            initializeSensors();
        }

        stepCountTextView = findViewById(R.id.stepCountTextView);
        goalTextView = findViewById(R.id.goalTextView);
        chronometer = findViewById(R.id.chronometer);
        progressBar = findViewById(R.id.progressBar);

        // Reset step count and elapsed time
        resetStepCount();

        // Start the chronometer
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
        if (stepSensor == null) {
            Toast.makeText(this, "Step counting sensor is not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Calculate the step count based on the initial value and the current event value
            if (stepCount == 0) {
                stepCount = (int) event.values[0];
            } else {
                int stepIncrement = (int) (event.values[0] - stepCount);
                stepCount += stepIncrement;
            }

            // Update step count and progress
            stepCountTextView.setText(String.valueOf(stepCount));
            updateProgress();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void updateProgress() {
        // Calculate elapsed time in seconds
        long currentTime = SystemClock.elapsedRealtime();
        long elapsedTimeSeconds = (currentTime - chronometer.getBase()) / 1000;

        // Update goal progress
        float distanceCovered = stepCount * 0.762f; // Assuming average step length of 0.762 meters
        int progress = (int) ((distanceCovered / goalDistance) * 100);
        progressBar.setProgress(progress);
        goalTextView.setText(String.format("%.2f/%d km", distanceCovered, goalDistance));

        // Update chronometer text
        String progressText = String.format("%02d:%02d", elapsedTimeSeconds / 60, elapsedTimeSeconds % 60);
        chronometer.setText(progressText);
    }

    private void resetStepCount() {
        stepCount = 0;
        stepCountTextView.setText(String.valueOf(stepCount));
        progressBar.setProgress(0);
        goalTextView.setText(String.format("0/%d km", goalDistance));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SENSOR_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeSensors();
            } else {
                Toast.makeText(this, "Permission denied, step counting will not work", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
