package com.magnetic.m_nfc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;;
import android.os.Bundle;

import java.util.ArrayList;

public class MnfcValues extends Activity implements SensorEventListener {

    private static final String TAG = "Magnet";
    // Sensors & SensorManager
    private Sensor magnetometer;
    private SensorManager mSensorManager;

    // Storage for Sensor readings
    private float[] mGeomagnetic = null;

//    private TextView mnfcDisplay = null;
//    private TextView axisDisplay = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mnfc_values);

        // Get a reference to the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get a reference to the magnetometer
        magnetometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Exit unless sensor are available
        if (null == magnetometer)
            finish();

        changeToHome();
    }

    private  void changeToHome(){
        final Button switchToHome = findViewById(R.id.back_home_mnfc);
        switchToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register for sensor updates

        mSensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister all sensors
        mSensorManager.unregisterListener(this);

    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent event) {

        TextView mnfcDisplay = findViewById(R.id.mnfc_value_display);
        TextView axisDisplay = findViewById(R.id.axis_value_display);
        ArrayList<Float> magneticFieldValues = new ArrayList<Float>();
        int lowerBoarder = 0;
        int upperBoarder = 0;


        // Acquire magnetometer event data

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            mGeomagnetic = new float[3];
            System.arraycopy(event.values, 0, mGeomagnetic, 0, 3);

        }

        // If we have readings from the sensor then
        // update the display.

        if (mGeomagnetic != null) {

            //Display sensor axis values and total magnetic field strength
            float magneticVectorLength = vectorLength(mGeomagnetic);

            calibrateMnfc(lowerBoarder, upperBoarder);
            axisDisplay.setText(String.format("Axis values: \nX=%.2f mT | Y=%.2f mT | Z=%.2f mT" +
                    "\nTotal magnetic field: %.2f mT", mGeomagnetic[0], mGeomagnetic[1], mGeomagnetic[2], magneticVectorLength));

            //Display M-NFC values:
            int values = bitTranslation(magneticVectorLength, lowerBoarder, upperBoarder);
            mnfcDisplay.setText(String.format("Receiving bit values: %1d", values));
            Log.d(TAG, "mx : "+mGeomagnetic[0]+" my : "+mGeomagnetic[1]+" mz : "+mGeomagnetic[2]);

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // N/A
    }

    //Calculating a vectors length
    private float vectorLength(float[] magneticV){
        //The squareroot of the sum of each squared value
        return  (float) Math.sqrt(magneticV[0]*magneticV[0] + magneticV[1]*magneticV[1] + magneticV[2]*magneticV[2]);
    }

    //Scanns receiving magnetic field strength and returns bit value according to high/low magnetic field strength
    private int bitTranslation(float currentMagneticField, int lowMagneticField, int highMagneticField){

        int bitValue;
        if(currentMagneticField >= (highMagneticField-2) && currentMagneticField <= (highMagneticField+2)){
            bitValue = 1;
        }
        else if(currentMagneticField >= (lowMagneticField-2) && currentMagneticField <= (lowMagneticField+2)){
            bitValue = 0;
        } else{
            bitValue = -1;
        }

        return bitValue;
    }

    //Calculates average of a set of samples from the sensor to determine upper and lower boundaries
    //Returns int value because no decimal number needed for boundaries (at the moment)
    private int getAverageMagneticField(ArrayList<Float> values){

        float sum = 0;
        if(!values.isEmpty()){
            for(Float value : values){
                sum += value;
            }
            return (int) (sum / values.size());
        }
        return (int) sum;
    }

    //TODO: get sensor samples and scan for high and low boundaries to assign to variables
    private void calibrateMnfc(int lower, int upper){


        lower = 0;
        upper = 0;

    }
}
