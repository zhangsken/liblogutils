package com.github.loneyz.libapplogutils;

import android.view.View;
import android.content.Context;
import android.util.AttributeSet;
import android.content.res.TypedArray;
import android.util.Log;
import android.os.Handler;
import java.lang.ref.WeakReference;
import android.os.Message;
import android.widget.TextView;
import android.widget.LinearLayout;
import java.util.ArrayList;
import android.widget.ScrollView;
import java.util.Set;
import java.util.Iterator;
import android.content.pm.PackageManager.NameNotFoundException;
import java.io.IOException;
import android.content.pm.PackageManager;
import android.widget.Toast;
import java.security.cert.Extension;

public class LogView extends LinearLayout {
    public static final String TAG = "LogView";

    ArrayList<LogFilterSpec> mlistLogFilterSpec;
    TextView mtvMessage;
    ScrollView msvLog;
    LogViewHandler mHandler;
    LogRTCatchThread mRealTimeLoadLogThread = null;
    LogFilterSpec.LogLevel mLogLevelDefault;
    /**
     * 在java代码里new的时候会用到
     * @param context
     */
    public LogView(Context context) {
        super(context);
    }

    /**
     * 在xml布局文件中使用时自动调用
     * @param context
     */
    public LogView(Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            mtvMessage = new TextView(context);
            msvLog = new ScrollView(context);
            mHandler = new LogViewHandler(this);
            msvLog.addView(mtvMessage);
            addView(msvLog);

            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LogView);
            String szTagListString = typedArray.getString(R.styleable.LogView_tagList);
            String szLevelDefault = typedArray.getString(R.styleable.LogView_levelDefault);
            LogFilterSpec.LogLevel logLevelDefault = LogFilterSpec.LogLevel.valueOf(szLevelDefault);
            mLogLevelDefault = (logLevelDefault == null) ? LogFilterSpec.LogLevel.V : logLevelDefault;
            //Toast.makeText(context, logLevel.toString(), Toast.LENGTH_SHORT).show();
            if (szTagListString == null) {
                mlistLogFilterSpec = new ArrayList<LogFilterSpec>();
            } else {
                mlistLogFilterSpec = LogFilterSpec.buildLogFilterSpecArrayList(context, szTagListString, logLevelDefault);
            }


            mtvMessage.setTextColor(typedArray.getColor(R.styleable.LogView_textColor, 0xFF000000));
            mtvMessage.setTextIsSelectable(typedArray.getBoolean(R.styleable.LogView_textIsSelectable, false));
            typedArray.recycle();

            mtvMessage.setBackground(context.getDrawable(R.drawable.blank10x10));
            msvLog.setBackground(context.getDrawable(R.drawable.blank10x10));
        } catch (Exception e) {
            Log.e(TAG, "[" + LogView.class.getName() + "] [LogView(Context context, AttributeSet attrs)] Exception : " + e.getMessage());
        }
    }

    /**
     * 不会自动调用，如果有默认style时，在第二个构造函数中调用
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public LogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    /**
     * 只有在API版本>21时才会用到
     * 不会自动调用，如果有默认style时，在第二个构造函数中调用
     * @param context
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    public LogView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void appenMessage(String sz) {
        mtvMessage.append(sz + "\n");
        scrollMessage();
    }

    public void startLog() {
        if (mRealTimeLoadLogThread == null) { 
            mRealTimeLoadLogThread = new LogRTCatchThread(mHandler, mlistLogFilterSpec, mLogLevelDefault);
            mRealTimeLoadLogThread.start();
            String szLogcatString = LogFilterSpec.buildRealTimeLogcatString(mlistLogFilterSpec, null);
            Log.i(TAG, "startLog() Logcat string is : " + szLogcatString);

        } else {
            Log.d(TAG, "Start log failed.");
        }

    }

    public boolean updateLogFilterSpecLevel(String szTAGName, LogFilterSpec.LogLevel logLevel) {
        LogFilterSpec logFilterSpec = getLogFilterSpecByTAGName(szTAGName);
        if (logFilterSpec != null) {
            for (int i = 0; i < mlistLogFilterSpec.size(); i++) {
                if (mlistLogFilterSpec.get(i).getTAG().equals(logFilterSpec.getTAG())) {
                    LogFilterSpec.LogLevel levelOld = mlistLogFilterSpec.get(i).getLogLevel();
                    mlistLogFilterSpec.get(i).setLogLevel(logLevel);
                    Log.d("LogView", "Target logLevel ("+logLevel+") Modifies : " + mlistLogFilterSpec.get(i).getTAG() +":"+levelOld+"->"+mlistLogFilterSpec.get(i).getLogLevel());
                    return true;
                }
            }
        }

        return false;
    }

    LogFilterSpec getLogFilterSpecByTAGName(String szTAGName) {
        for (int i = 0; i < mlistLogFilterSpec.size(); i++) {
            if (mlistLogFilterSpec.get(i).getTAG().equals(szTAGName)) {
                return mlistLogFilterSpec.get(i);
            }
        }
        return null;
    }

    public boolean addLogFilterSpec(LogFilterSpec logFilterSpec) {
        for (int i = 0; i < mlistLogFilterSpec.size(); i++) {
            if (mlistLogFilterSpec.get(i).getTAG().equals(logFilterSpec.getTAG())) {
                Log.i(TAG, "addLogFilterSpec(LogFilterSpec logFilterSpec) TAG Exist --> " + logFilterSpec.getTAG() + ":" + logFilterSpec.getLogLevel().name());
                return false;
            }
        }
        return mlistLogFilterSpec.add(logFilterSpec);
    }

    public void cleanLog() {
        mtvMessage.setText("");
        LogViewCleanLogThread logViewCleanLogThread = new LogViewCleanLogThread(mHandler);
        logViewCleanLogThread.start();

    }

    void scrollMessage() {
        final ScrollView sv = msvLog;
        sv.post(new Runnable() {
                @Override
                public void run() {
                    sv.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
    }


}
