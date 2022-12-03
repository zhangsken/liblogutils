package com.github.zhangsken.liblogutils;

import android.util.Log;
import android.os.Message;
import java.lang.ref.WeakReference;
import android.os.Handler;
import java.io.BufferedReader;
import android.os.Looper;
import java.io.IOException;
import com.github.zhangsken.liblogutils.LogViewHandler;
import java.text.SimpleDateFormat;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import java.util.logging.Level;

public class LogRTCatchThread extends Thread {
    
    public final static String TAG = "LogRTCatchThread";
    // 日志筛选标签数组
    ArrayList<LogFilterSpec> mlistLogFilterSpec;
    LogFilterSpec.LogLevel mLogLevelDefault;
    // 进程活动控制符号
    volatile boolean _mIsExit = false;
    
    WeakReference<Handler> handlerWeakReference;
    
    public LogRTCatchThread(Handler handler, ArrayList<LogFilterSpec> listLogFilterSpec, LogFilterSpec.LogLevel logLevelDefault) {
        Log.d(TAG, "call RealTimeLoadLogThread(Handler handler)");
        handlerWeakReference = new WeakReference<Handler>(handler);
        mlistLogFilterSpec = new ArrayList<LogFilterSpec>();
        for (int i = 0; i < listLogFilterSpec.size(); i++) {
            mlistLogFilterSpec.add(listLogFilterSpec.get(i));
            Log.d("LogView", "ADD "+listLogFilterSpec.get(i).getTAG()+":"+listLogFilterSpec.get(i).getLogLevel());
        }
        mLogLevelDefault = (logLevelDefault==null)?LogFilterSpec.LogLevel.V:logLevelDefault;
    }

    void sendMSG(String sz) {
        Message message1 = Message.obtain();
        message1.what = LogViewHandler.WHAT_APPEN_MSG_TEXT;
        message1.obj = sz;
        Handler handler1 = handlerWeakReference.get();
        if (!_mIsExit && handler1 != null) {
            handler1.sendMessage(message1);
        }
    }

    @Override
    public void run() {
        java.lang.Process mLogcatProc = null;
        BufferedReader reader = null;
        try {
            Log.i(TAG, " Start At < " + currentTimeString() + " >");
            //捕获实时日志信息
            //
            mLogcatProc = Runtime.getRuntime().exec(LogFilterSpec.buildRealTimeLogcatString(mlistLogFilterSpec, mLogLevelDefault));
            reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));
            String szLineNew;
            // 以下代码包函Looper
            Looper.prepare();
            while (!_mIsExit && (szLineNew = reader.readLine()) != null) {
                sendMSG(szLineNew);
            }
            Looper.loop();

            throw new RuntimeException("AppLogFragment thread loop unexpectedly exited!");
            // 此行以下代码不再执行到了
            // int i = 0; //不执行此行

        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

    }


    // 当前时间的格式化字符串
    //
    public String currentTimeString() {
        // 日期类转化成字符串类的工具
        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.mmm");
        // 返回当前时间格式化字符串
        return mSimpleDateFormat.format(System.currentTimeMillis());
    }
}
