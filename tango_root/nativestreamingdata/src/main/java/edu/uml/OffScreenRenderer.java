package edu.uml;

import android.util.Log;

/**
 * Created by csrobot on 9/4/14.
 */
public class OffScreenRenderer extends Thread {
    private static final String TAG = "OffScreenRenderer";
    @Override
    public void start() {
        Log.e(TAG, "STARTING");
        State currState = getState();
        Log.e(TAG, "CURRENT THREAD STATE = " + currState.toString());
        if ((currState == State.RUNNABLE || currState == State.NEW || currState == State.TERMINATED))
            super.start();
    }

    @Override
    public void run() {
        dowork();
    }

    public native void offScreenRenderer();
    public native void dowork();
}