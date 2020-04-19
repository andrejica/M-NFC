package com.magnetic.m_nfc;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.HandlerThread;

public class CalibrationServiceHandlerThread extends HandlerThread{
    private static final String TAG = "HandlerThread";

    public static final int CALIBRATION_TASK = 1;

    private Handler handler;

    public CalibrationServiceHandlerThread() {
        super("calibrationThread", Process.THREAD_PRIORITY_BACKGROUND);
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what){
                    case CALIBRATION_TASK:

                        break;
                }
            }
        };
    }

    public Handler getHandler(){
        return handler;
    }
}
