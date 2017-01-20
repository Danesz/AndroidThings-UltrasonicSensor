package com.danieldallos.androidthings.ultrasonicsensor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivityNoCallback extends AppCompatActivity {


    private static final String ECHO_PIN_NAME = "BCM20";
    private static final String TRIGGER_PIN_NAME = "BCM21";

    private static final String TAG = "ultrasonicsensor";

    private Gpio mEcho;
    private Gpio mTrigger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize PeripheralManagerService
        PeripheralManagerService service = new PeripheralManagerService();

        //List all available GPIOs
        Log.d(TAG, "Available GPIOs: " + service.getGpioList());

        try {
            // Create GPIO connection.
            mEcho = service.openGpio(ECHO_PIN_NAME);
            // Configure as an input.
            mEcho.setDirection(Gpio.DIRECTION_IN);
            // Enable edge trigger events.
            mEcho.setEdgeTriggerType(Gpio.EDGE_BOTH);
            // Set Active type to HIGH, then the HIGH events will be considered as TRUE
            mEcho.setActiveType(Gpio.ACTIVE_HIGH);

        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

        try {
            // Create GPIO connection.
            mTrigger = service.openGpio(TRIGGER_PIN_NAME);

            // Configure as an output with default LOW (false) value.
            mTrigger.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }


        new Thread(){
            @Override
            public void run() {
                try {
                    while (true) {
                        readDistanceSync();
                        Thread.sleep(300);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();



    }

    long time1, time2;
    int keepBusy;

    protected void readDistanceSync() throws IOException, InterruptedException {
        // Just to be sure, set the trigger first to false
        mTrigger.setValue(false);
        Thread.sleep(0,2000);

        // Hold the trigger pin HIGH for at least 10 us
        mTrigger.setValue(true);
        Thread.sleep(0,10000); //10 microsec

        // Reset the trigger pin
        mTrigger.setValue(false);

        // Wait for pulse on echo pin
        while (mEcho.getValue() == false) {
            //long t1 = System.nanoTime();
            //Log.d(TAG, "Echo has not arrived...");

            // keep the while loop busy
            keepBusy = 0;

            //long t2 = System.nanoTime();
            //Log.d(TAG, "diff 1: " + (t2-t1));
        }
        time1 = System.nanoTime();
        Log.i(TAG, "Echo ARRIVED!");

        // Wait for the end of the pulse on the ECHO pin
        while (mEcho.getValue() == true) {
            //long t1 = System.nanoTime();
            //Log.d(TAG, "Echo is still coming...");

            // keep the while loop busy
            keepBusy = 1;

            //long t2 = System.nanoTime();
            //Log.d(TAG, "diff 2: " + (t2-t1));
        }
        time2 = System.nanoTime();
        Log.i(TAG, "Echo ENDED!");

        // Measure how long the echo pin was held high (pulse width)
        long pulseWidth = time2 - time1;

        // Calculate distance in centimeters. The constants
        // are coming from the datasheet, and calculated from the assumed speed
        // of sound in air at sea level (~340 m/s).
        double distance = (pulseWidth / 1000.0 ) / 58.23 ; //cm

        // or we could calculate it with the speed of the sound:
        //double distance = (pulseWidth / 1000000000.0) * 340.0 / 2.0 * 100.0;

        Log.i(TAG, "distance: " + distance + " cm");

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close the resource
        if (mEcho != null) {
            try {
                mEcho.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }

        // Close the resource
        if (mTrigger != null) {
            try {
                mTrigger.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    }
}
