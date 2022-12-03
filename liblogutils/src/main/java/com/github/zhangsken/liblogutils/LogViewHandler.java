package com.github.zhangsken.liblogutils;

import java.lang.ref.WeakReference;
import android.os.Handler;
import android.os.Message;

public class LogViewHandler extends Handler {

    public static final int WHAT_APPEN_MSG_TEXT = 0;
    public static final int WHAT_CLEAR_ALL_TEXT = 1;
    
    WeakReference<LogView> viewWeakReference; // 持有外部类的弱引用
    public LogViewHandler(LogView view) {
        viewWeakReference = new WeakReference<LogView>(view);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case WHAT_APPEN_MSG_TEXT: // 处理下载完成消息，更新UI
                {
                    // 显示log数据
                    //
                    LogView logView = viewWeakReference.get();
                    if (logView != null) {
                        logView.appenMessage((String)msg.obj);
                    }
                    break;
                }
        }
    }
}
