package com.github.loneyz.libapplogutils;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import android.app.FragmentTransaction;

public class LogViewActivity extends Activity {
    public static final String TAG = LogViewActivity.class.getSimpleName();
    public static final String EXTRA_MSG = "szMSG";
    LogViewFragment mLogViewFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logview);
        mLogViewFragment = new LogViewFragment();
        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.add(R.id.activitylogviewFrameLayout1, mLogViewFragment, LogViewFragment.TAG);
        tx.commit();
    }

} 
