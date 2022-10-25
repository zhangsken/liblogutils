package com.github.loneyz.applogutils;
import android.util.Log;

public class TestClassA {
    public static final String TAG = TestClassA.class.getSimpleName();

    public static void writeTestTAG() {
        
        Log.v(TAG, "V");
        Log.d(TAG, "D");
        Log.i(TAG, "I");
        Log.w(TAG, "W");
        Log.e(TAG, "E");
    }
    
}
