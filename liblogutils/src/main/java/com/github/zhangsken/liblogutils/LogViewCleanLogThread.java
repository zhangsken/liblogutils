package com.github.zhangsken.liblogutils;
import android.os.*;
import java.lang.ref.*;
import java.text.SimpleDateFormat;
import android.util.Log;

public class LogViewCleanLogThread extends Thread {
    public static final String TAG = LogViewCleanLogThread.class.getSimpleName();
    
    // 日期类转化成字符串类的工具
    SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss");
    
    WeakReference<LogViewHandler> handlerWeakReference;
    public LogViewCleanLogThread(LogViewHandler handler) {
        handlerWeakReference = new WeakReference<LogViewHandler>(handler);
    }

    void sendMSG(String sz) {
        Message message1 = Message.obtain();
        message1.what = LogViewHandler.WHAT_APPEN_MSG_TEXT;
        message1.obj = sz;
        Handler handler1 = handlerWeakReference.get();
        if (handler1 != null) {
            handler1.sendMessage(message1);
        }
    }

    @Override
    public void run() {
        try {
            Message message1 = Message.obtain();
            message1.what = LogViewHandler.WHAT_CLEAR_ALL_TEXT;
            Handler handler1 = handlerWeakReference.get();
            if (handler1 != null) {
                handler1.sendMessage(message1);
            }
            sendMSG("正在清理旧Log : " + getDateNowString());
            java.lang.Process procClean = Runtime.getRuntime().exec("logcat -c");
            procClean.waitFor();
            sendMSG("清理结束 : " + getDateNowString());
            
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

    }
    
    // 获取当前时间的格式化字符串
    public String getDateNowString() {
        // 读取当前时间
        long nTimeNow = System.currentTimeMillis();
        return mSimpleDateFormat.format(nTimeNow);
    }
}
