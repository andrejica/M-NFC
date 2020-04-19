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
    private static final String TAG_MAGNET = "Magnet";
    private static final String TAG_MAIN = "MainActivity";

    //private CalibrationServiceHandlerThread handlerThread = new CalibrationServiceHandlerThread();

    // Sensors & SensorManager
    private Sensor magnetometer;
    private SensorManager mSensorManager;

    // Storage for Sensor readings
    private float[] mGeomagnetic = null;
    private boolean isHighBit = false;
    private boolean calibration = false;
    private boolean finishedCalibration = false;
    private boolean scanTransmission = false;
    private int lowerBoarder = 0;
    private int upperBoarder = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mnfc_values);

        //handlerThread.start();

        // Get a reference to the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get a reference to the magnetometer
        magnetometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Exit unless sensor are available
        if (null == magnetometer) {
            finish();
        }

        changeToHome();
        //calibrateMnfc();
    }

    //Switch to Home menu and quit Handlerthread.
    private  void changeToHome(){
        final Button switchToHome = findViewById(R.id.back_home_mnfc);
        switchToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                //handlerThread.quit();
            }
        });
    }

    //Starts a service to calibrate the phone for transmission
    public void calibrateMnfc(View view){
        calibration = true;
//        final Button calibrate = findViewById(R.id.calibrate);
//        calibrate.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Message msg = Message.obtain();
//                msg.what = 1;
//                msg.arg1 = 0;
//                msg.obj = "calibrate Magnet";
//                handlerThread.getHandler().post(new CalibrationServiceHandlerThread());
//            }
//        });
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

        TextView mnfcDisplay = findViewById(R.id.mnfc_bit_display);
        TextView axisDisplay = findViewById(R.id.axis_value_display);
        TextView magneticFieldDisplay = findViewById(R.id.magnetic_field_average);
        TextView messageDisplay = findViewById(R.id.show_text);
        ArrayList<Float> magneticFieldValues = new ArrayList<Float>();
        ArrayList<Integer> bytePackets = new ArrayList<Integer>();
        int bitValue = 0;
        int currentBoarder = 0;


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
            axisDisplay.setText(String.format(R.string.axis_field_description + "\nX=%.2f mT | Y=%.2f mT | Z=%.2f mT" +
                    "\nTotal magnetic field: %.2f mT", mGeomagnetic[0], mGeomagnetic[1], mGeomagnetic[2], magneticVectorLength));

            //Calibrate the Bit boarders to distinguish if the measured bit value
            //is a 1 or a 0
            //TODO: Debug calibration part, only adds 1 values to ArrayList...
            if(calibration) {
                magneticFieldValues.add(magneticVectorLength);
                calibrateBitBoarder(magneticFieldValues, messageDisplay);
                Log.d(TAG_MAIN, String.valueOf(magneticFieldValues.size()));
            }

            //Assigns M-NFC values according to set boundaries from calibration process.
            if(scanTransmission) {
                bitValue = bitTranslation(magneticVectorLength, lowerBoarder, upperBoarder);
                bytePackets.add(bitValue);
            }
            mnfcDisplay.setText(String.format(R.string.bit_value_description +"%1d", bitValue));

            //Log.d(TAG_MAGNET, "mx : "+mGeomagnetic[0]+" my : "+mGeomagnetic[1]+" mz : "+mGeomagnetic[2]);

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
    //Returns -1 as error if couldn't recognise bit
    private int bitTranslation(float currentMagneticField, int lowBoarder, int highBoarder){

        int bitValue;
        if(currentMagneticField >= (highBoarder-2) && currentMagneticField <= (highBoarder+2)){
            bitValue = 1;
        }
        else if(currentMagneticField >= (lowBoarder-2) && currentMagneticField <= (lowBoarder+2)){
            bitValue = 0;
        } else{
            bitValue = -1;
        }

        return bitValue;
    }

    //Checks values from ArrayList and sets upper and lower boarder
    //little error check if the values are correct set
    //comparing lower and upper boarder to each other
    @SuppressLint("SetTextI18n")
    private void calibrateBitBoarder(ArrayList<Float> values, TextView view){

        if(finishedCalibration){
            //Stop the calibration if values are set and clears ArrayList
            values.clear();
            isHighBit = false;
            calibration = false;
            finishedCalibration = false;
            view.setText(R.string.message_field_description + "\nCalibration done.");
        } else if(!isHighBit && values.size()> 50) {
            view.setText(R.string.message_field_description + "\nCalibrate lower boarder...");
            values.remove(0);
            lowerBoarder = calculateAverageMagneticField(values);
            //Checking if lowerBoarder is set.
            //Otherwise let assign lowerBoarder again
            isHighBit = ((upperBoarder - lowerBoarder) < 0);
        } else if (isHighBit && values.size() > 50){
            view.setText(R.string.message_field_description + "\nCalibrate upper boarder...");
            values.remove(0);
            upperBoarder = calculateAverageMagneticField(values);
            finishedCalibration = ((upperBoarder - lowerBoarder) > 3);
        }
    }

    //Calculates average of a set of samples from the sensor to determine upper and lower boundaries
    //Returns int value because no decimal number needed for boundaries (at the moment)
    //Returns 0 if empty
    private int calculateAverageMagneticField(ArrayList<Float> values){

        float sum = 0;
        if(!values.isEmpty()){
            for(Float value : values){
                sum += value;
            }
            return (int) (sum / values.size());
        }
        return (int) sum;
    }


}
