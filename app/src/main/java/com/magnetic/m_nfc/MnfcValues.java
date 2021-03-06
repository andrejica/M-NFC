package com.magnetic.m_nfc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class MnfcValues extends Activity implements SensorEventListener {
    private static final String TAG_MAGNET = "Magnet";
    private static final String TAG_MAIN = "MainActivity";
    public static final int MAX_CALIBRATION_VALUES = 50;

    private TextView messageDisplay;
    private TextView set_val;

    private Ndef reader;

    // Sensors & SensorManager
    private Sensor magnetometer;
    private SensorManager mSensorManager;

    // Storage for Sensor readings
    private float[] mGeomagnetic = null;
    private ArrayList<Float> magneticFieldValues = new ArrayList<>();
    private BitSet messageAsBits = new BitSet();
    private int sumOfBits = 0;
    private boolean isHighBit = false;
    private boolean calibration = false;
    private boolean finishedCalibration = false;
    private boolean scanTransmission = false;
    private int counter = 0;
    private int lowerBorder = 0;
    private int upperBorder = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mnfc_values);
        messageDisplay = findViewById(R.id.show_text);
        set_val = findViewById(R.id.set_values);

        // Get a reference to the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get a reference to the magnetometer
        magnetometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Exit unless sensor are available
        if (null == magnetometer) {
            generateTextForToast("Error: Magnetometer is not available!!");
            finish();
        }

        changeToHome();
    }

    //region Buttons

    //Switch to Home menu and quit Handlerthread.
    private  void changeToHome(){
        final Button switchToHome = findViewById(R.id.back_home_mnfc);
        switchToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateTextForToast("M-NFC going to sleep...");
                finish();
            }
        });
    }

    //Starts a service to calibrate the phone for transmission
    public void calibrateMnfc(View view){
        calibration = true;
        messageDisplay.setText(getDisplayTitle("mes") + " \nStarting calibration!");
    }

    public void startScanning(View view){
        scanTransmission = true;
        messageDisplay.setText(getDisplayTitle("mes") + " \nStarting scan process!");
    }

    //endregion

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
        int bitValue;


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
            axisDisplay.setText(String.format(getDisplayTitle("axis")
                    + "\n\n\tX=%.2f mT | Y=%.2f mT | Z=%.2f mT", mGeomagnetic[0], mGeomagnetic[1], mGeomagnetic[2]));
            magneticFieldDisplay.setText(String.format(getDisplayTitle("str") + "\n\n\t%.2f mT", magneticVectorLength));

            //Calibrate the Bit borders to distinguish if the measured bit value
            //is a 1 or a 0
            if(calibration) {
                magneticFieldValues.add(magneticVectorLength);
                calibrateBitBorder(magneticFieldValues, messageDisplay);
                Log.d(TAG_MAIN, String.valueOf(magneticFieldValues.size()));
            }

            //Assigns M-NFC values according to set boundaries from calibration process.
            if(scanTransmission) {
                bitValue = bitTranslation(magneticVectorLength, lowerBorder, upperBorder);
                magneticFieldValues.add(magneticVectorLength);
                mnfcDisplay.setText(String.format(getDisplayTitle("byte") +"\n\n\t%1d", bitValue));
                scanAndTranslateMnfc(bitValue, magneticFieldValues, messageAsBits);
            }
            //Log.d(TAG_MAGNET, "mx : "+mGeomagnetic[0]+" my : "+mGeomagnetic[1]+" mz : "+mGeomagnetic[2]);

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // N/A
    }

    //Scans receiving magnetic field strength and returns bit value according to high/low magnetic field strength
    //Returns -1 as error if couldn't recognise bit
    private int bitTranslation(float currentMagneticField, int lowBorder, int highBorder){

        int bitVal;
        if(currentMagneticField >= (highBorder-2) && currentMagneticField <= (highBorder+2)){
            bitVal = 1;
        }
        else if(currentMagneticField >= (lowBorder-2) && currentMagneticField <= (lowBorder+2)){
            bitVal = 0;
        }
        else{
            bitVal = -1;
        }

        return bitVal;
    }

    //Checks values from ArrayList and sets upper and lower boarder
    //little error check if the values are correct set
    //comparing lower and upper boarder to each other
    @SuppressLint("SetTextI18n")
    private void calibrateBitBorder(ArrayList<Float> values, TextView view){

        if(!isHighBit){
            view.setText(getDisplayTitle("mes") + "\n\tCalibrate lower border..." + showCalibrationInPercent(values));
        } else {
            view.setText(getDisplayTitle("mes") + "\n\tCalibrate upper border..." + showCalibrationInPercent(values));
        }

        int averageNow = calculateAverageMagneticField(values);
        if(finishedCalibration && upperBorder != 0 && lowerBorder != 0){
            //Stop the calibration if values are set and clears ArrayList
            values.clear();
            isHighBit = false;
            calibration = false;
            finishedCalibration = false;
            view.setText(getDisplayTitle("mes") + "\n");
            view.setText(String.format(getDisplayTitle("mes") + " Calibration done. \nValues for bit recognition " +
                    "\n\t\tUpper border -> %2d mT \n\t\tLower border -> %2d mT", upperBorder, lowerBorder));
            set_val.setText(String.format(getDisplayTitle("set") +
                    " \n\tBit value 0 -> %1d mT \n\tBit value 1 -> %1d mT", lowerBorder, upperBorder));
        } else if(!isHighBit && values.size()> 50) {
            values.remove(0);
            lowerBorder = averageNow;
            //Checking if lowerBoarder is set.
            //Otherwise let assign lowerBoarder again
            if(Math.abs(upperBorder - lowerBorder) > 0) {
                isHighBit = true;
                values.clear();
                view.setText(getDisplayTitle("mes") + "\n\tCalibration lower border done!");
            }
        } else if (isHighBit && (averageNow > 0) && values.size() > 50){
            values.remove(0);
            upperBorder = calculateAverageMagneticField(values);
            finishedCalibration = (Math.abs(upperBorder - lowerBorder) > 4);
        }
    }

    /*
    checks value of the registered Bit at the moment.
    and adds the result to an overall evaluation to a variable.
    if enough magnetic-field values are checked it then stores the
    estimated Bit value from the variable "sumOfBits" into a list as a logical "1" or "0".
    Which later it translates the message back.
    */
    private void scanAndTranslateMnfc(int bitVal, ArrayList<Float> magneticList, BitSet message){

        //Adds up to 5 values into "sumOfBits"
        if(bitVal == 1){
            sumOfBits++;
        } else if(bitVal == 0){
            sumOfBits--;
        }

        //Positive "sumOfBits" leads to a logical 1
        //Negative "sumOfBits" is a logical 0
        if(magneticList.size() >= 10){

            if(sumOfBits > 0){
                message.set(counter, true);
            } else if(sumOfBits < 0){
                message.set(counter, false);
            }

            magneticList.clear();
            sumOfBits = 0;
            counter++;
            Log.d("BitSet Value", String.valueOf(message.get(counter)));
        }

        //Translates message from Bits into string Text
        //and displays on the App screen
        if(counter >= 64){
            byte[] result = message.toByteArray();
            String receivedMessage = new String(result, StandardCharsets.UTF_8);

            messageDisplay.setText(getDisplayTitle("mes") +
                    "\nMessage received!" + "\n\tYour message: " + receivedMessage +
                    "\n\t>>received via M-NFC<<");
            counter = 0;
            scanTransmission = false;

        }
    }


    // region helper methods

    //Calculating a vectors length
    private float vectorLength(float[] magneticV){
        //The squareroot of the sum of each squared value
        return  (float) Math.sqrt((magneticV[0]*magneticV[0])/9 + (magneticV[1]*magneticV[1])/9 +
                (magneticV[2]*magneticV[2])/9);
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

    //picking string value for Textfield title text
    private String getDisplayTitle(String title){

        String t = "";
        switch (title){
            case "axis":
                t = getString(R.string.axis_field_description);
                break;
            case "str":
                t = getString(R.string.magnetic_values_field_description);
                break;
            case "byte":
                t = getString(R.string.bit_value_description);
                break;
            case  "mes":
                t = getString(R.string.message_field_description);
                break;
            case "set":
                t = getString(R.string.set_value);
                break;
        }
        return t;
    }

    //not necessary to make
    private void dummyNdefMessage(ArrayList<ArrayList<Integer>> message){

        //Flags for the record header. Describes the NDEFrecord (1 byte)
        //Structure: TNF(3 Bit)+ Flags
        ArrayList<Integer> tnfAndFlags = new ArrayList<Integer>();
        List<Integer> tnfNFlags = Arrays.asList(0,0,1, 0, 0, 0, 1, 1);
        tnfAndFlags.addAll(tnfNFlags);

        //Type length (1 byte)

    }

    //Displays a Toast message with text as input
    private void generateTextForToast(String text){
        Context context = getApplicationContext();
        CharSequence message = text;
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    //Showing calibration progress in percent
    private String showCalibrationInPercent(ArrayList<Float> values){
        float percent = (float) values.size() / MAX_CALIBRATION_VALUES;
        if(percent > 1){
            percent = 1;
        }
        return String.format("\n\tprogress: %.1f", (percent*100)) + "%";
    }

    // endregion
}
