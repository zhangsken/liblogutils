package com.github.zhangsken.liblogutils;

/*
 * Powerby : ZhanGSKen(ZhangShaojian2018@163.com)
 * re. :
 *
 * Log类型数据
 * -- V : Verbose (明细);
 * -- D : Debug (调试);
 * -- I : Info (信息);
 * -- W : Warn (警告);
 * -- E : Error (错误);
 * -- A : Assert (断言);
 * -- F : Fatal (严重错误);
 * -- S : Silent(Super all output) (最高的优先级, 可能不会记载东西);
 */

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.github.zhangsken.liblogutils.R;
import com.github.zhangsken.liblogutils.ClassUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import android.app.Fragment;
import android.widget.Toolbar;
import android.app.Activity;
import android.widget.CheckBox;
import android.widget.AdapterView;
import android.widget.CompoundButton;


public class LogViewFragment extends Fragment {

    public static final String TAG = LogViewFragment.class.getSimpleName();

    // 用户设置数据保存路径
    static String mszConfigDataPath;
    static final String SZ_ALL = "[ALL]";
    View mView;
    //Toolbar mToolbar;
    TextView mtvLogView;
    MenuItem  mmiSyncLog;
    Button mbtnTestLog;
    Button mbtnAddTAGFilter;
    Button mbtnSubTAGFilter;
    ScrollView msvLog;
    Handler mHandler;
    RealTimeLoadSettingLogThread mRealTimeLoadSettingLogThread;
    RealTimeLoadAllLogThread mRealTimeLoadAllLogThread;
    Thread mCleanLogThread;
    Spinner mSpinnerAllTAG;
    ArrayList<String> mArrayListStringAllTAG;
    ArrayAdapter<String> mAdapterAllTAG;
    Spinner mSpinnerSelectedTAG;
    ArrayList<String> mArrayListStringSelectedTAG;
    ArrayAdapter<String> mAdapterSelectedTAG;
    // 首次加载窗口读取到的所有历史log数据
    ArrayList<String> mArrayListStringLastLogTemp = new ArrayList<String>();
    // 要显示的最后log条数
    int mnLastLogTemp_ShowNumber = 100;
    // 日期类转化成字符串类的工具
    SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss");
    ConfigDataUtils mConfigDataUtils;
    
    TextView tvTitle;
    Button mbtnCleanLog;
    Button mbtnDefaultClassTAG;
    CheckBox cbIsSyncLog;
    static String[] szarrayLogViewLevel = {"V", "D", "I", "W", "E","A", "F", "S"};
    Spinner mSpinnerLogViewLevel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mszConfigDataPath = getActivity().getExternalFilesDir(TAG) + "ConfigData.dat";
        mConfigDataUtils = ConfigDataUtils.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_logview, container, false);

        //mToolbar = mView.findViewById(R.id.fragmentlogviewToolbar1);
        //mToolbar.inflateMenu(R.menu.toolbar_logview);
        //mToolbar.setTitle(TAG);
        tvTitle = mView.findViewById(R.id.fragmentlogviewTextView2);
        tvTitle.setText(TAG);
        
        mbtnCleanLog = mView.findViewById(R.id.fragmentlogviewButton4);
        mbtnCleanLog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startCleanLog();
                }
            });
        
        //
        cbIsSyncLog = mView.findViewById(R.id.fragmentlogviewCheckBox1);
        boolean isSyncLog = mConfigDataUtils.getIsSyncLog();
        cbIsSyncLog.setChecked(isSyncLog);
        cbIsSyncLog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){ 
                @Override 
                public void onCheckedChanged(CompoundButton buttonView, 
                                             boolean isChecked) { 
                    // TODO Auto-generated method stub 
                    mConfigDataUtils.setIsSyncLogAndSave(isChecked);
                    cbIsSyncLog.setChecked(isChecked);
                    restartSyncThreadByConfig();
                } 
            }); 
        
            
        // 设置调试级别菜单
        mSpinnerLogViewLevel = mView.findViewById(R.id.fragmentlogviewSpinner3);
        //声明一个下拉列表的数组适配器
        ArrayAdapter<String> starAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,szarrayLogViewLevel);
        //设置数组适配器的布局样式
        //starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        //设置下拉框的标题，不设置就没有难看的标题了
        mSpinnerLogViewLevel.setPrompt("请选择行星");
        //设置下拉框的数组适配器
        mSpinnerLogViewLevel.setAdapter(starAdapter);
        int nLogLevel = mConfigDataUtils.getLogLevel();
        //设置下拉框默认的显示第一项
        mSpinnerLogViewLevel.setSelection(nLogLevel);
        //给下拉框设置选择监听器，一旦用户选中某一项，就触发监听器的onItemSelected方法
        mSpinnerLogViewLevel.setOnItemSelectedListener(new SpinnerLogViewLevelSelectedListener());
        
        
        mbtnDefaultClassTAG = mView.findViewById(R.id.fragmentlogviewButton5);
        mbtnDefaultClassTAG.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setArrayListStringDefaultClassTAG();
                }
            });
        
        //getActivity().setActionBar(mToolbar);

        mbtnTestLog = mView.findViewById(R.id.fragmentlogviewButton3);
        //使用匿名内部类
        mbtnTestLog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "TestLog v");
                    Log.d(TAG, "TestLog d");
                    Log.i(TAG, "TestLog i");
                    Log.w(TAG, "TestLog w");
                    Log.e(TAG, "TestLog e");
                }
            });
        mbtnAddTAGFilter = mView.findViewById(R.id.fragmentlogviewButton1);
        //使用匿名内部类
        mbtnAddTAGFilter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addTAGFilter();
                }
            });
        mbtnSubTAGFilter = mView.findViewById(R.id.fragmentlogviewButton2);
        //使用匿名内部类
        mbtnSubTAGFilter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteTAGFilter();
                }
            });

        // 初始化窗口元素成员
        mtvLogView = mView.findViewById(R.id.fragmentlogviewTextView3);
        msvLog = mView.findViewById(R.id.fragmentlogviewScrollView1);
        //mspinnerTypeFilter = findViewById(R.id.activitylogviewSpinner1);
        mSpinnerAllTAG = mView.findViewById(R.id.fragmentlogviewSpinner1);
        mSpinnerSelectedTAG = mView.findViewById(R.id.fragmentlogviewSpinner2);

        // Log用户标签列表数据
        mArrayListStringSelectedTAG = mConfigDataUtils.mConfigData.mArrayListStringTAG;
        mAdapterSelectedTAG = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, mArrayListStringSelectedTAG);
        mAdapterSelectedTAG.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerSelectedTAG.setAdapter(mAdapterSelectedTAG);

        // Log所有标签列表数据
        mArrayListStringAllTAG = new ArrayList<String>();
        mArrayListStringAllTAG.add(SZ_ALL);
        mAdapterAllTAG = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, mArrayListStringAllTAG);
        mAdapterAllTAG.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerAllTAG.setAdapter(mAdapterAllTAG);

        mHandler = new MyHandler(this);

        // 按配置启动log
        startSyncThreadByConfig();
        return mView;
    }
    class SpinnerLogViewLevelSelectedListener implements AdapterView.OnItemSelectedListener{

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            //Toast.makeText(MainActivity.this,"您选择的是："+starArray[i],Toast.LENGTH_SHORT).show();
            mConfigDataUtils.setLogLevelAndSave(mSpinnerLogViewLevel.getSelectedItemPosition());
            //Log.v(TAG, "mi.getItemId() is " + Integer.toString(mi.getItemId()));
            //mi.setChecked(true);
            restartSyncThreadByConfig();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }
    
    

    void startCleanLog() {
        stopSyncThread();
        mCleanLogThread = new CleanLogThread(mHandler);
        mCleanLogThread.start();
    }

    // 按照配置启动mRealTimeLoadLogThread
    // 与mRealTimeLoadLogTAGThread进程
    // 如果进程已经启动则直接退出
    //
    void startSyncThreadByConfig() {
        Log.d(TAG, "call startSyncThreadByConfig()");
        boolean isRealTimeLoadLog = mConfigDataUtils.getIsSyncLog();
        if (isRealTimeLoadLog) {
            if (mRealTimeLoadSettingLogThread == null) {
                mRealTimeLoadSettingLogThread = new RealTimeLoadSettingLogThread(mHandler);
                mRealTimeLoadSettingLogThread.start();
            }

            if (mRealTimeLoadAllLogThread == null) {
                mRealTimeLoadAllLogThread = new RealTimeLoadAllLogThread(mHandler);
                mRealTimeLoadAllLogThread.start();
            }
        }
    }

    void restartSyncThreadByConfig() {
        Log.d(TAG, "call restartSyncThreadByConfig()");

        stopSyncThread();

        startSyncThreadByConfig();
    }

    void stopSyncThread() {
        Log.d(TAG, "call stopSyncThread()");

        if (mRealTimeLoadSettingLogThread != null) {
            mRealTimeLoadSettingLogThread.exit = true;
            mRealTimeLoadSettingLogThread = null;
        }

        if (mRealTimeLoadAllLogThread != null) {
            mRealTimeLoadAllLogThread.exit = true;
            mRealTimeLoadAllLogThread = null;
        }
    }

   

    // 添加所有应用包的基本类标签，不包括基本类中的匿名类
    //
    void setArrayListStringDefaultClassTAG() {
        try {
            Set<String> setString = ClassUtils.getFileNameByPackageName(getActivity().getApplicationContext(), getActivity().getApplicationContext().getPackageName());
            Log.d(TAG, "setString.size() is " + Integer.toString(setString.size()));
            ArrayList<String> arrayListString = new ArrayList<String>();

            Iterator it = setString.iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof String) {
                    Log.d(TAG, "obj is " + obj);
                    String szTemp = ((String)obj).replaceAll(".*[\\.]+", "");
                    szTemp = szTemp.replaceAll("\\$+.*", "");
                    Log.d(TAG, "szTemp is " + szTemp);
                    mConfigDataUtils.addTAGNoRepeat(szTemp);
                }
            }
            Log.d(TAG, "mConfigData.mArrayListStringTAG.size() is " + Integer.toString(mConfigDataUtils.mConfigData.mArrayListStringTAG.size()));
            mConfigDataUtils.saveConfigData();
            mAdapterSelectedTAG.notifyDataSetChanged();
            restartSyncThreadByConfig();
        } catch (InterruptedException e) {} catch (PackageManager.NameNotFoundException e) {} catch (IOException e) {}

    }

    void addTAGFilter() {
        String szSelectTAGFilter = mSpinnerAllTAG.getSelectedItem().toString();
        if (szSelectTAGFilter.equals(SZ_ALL)) {
            mConfigDataUtils.mConfigData.mArrayListStringTAG.clear();
        } else {
            mConfigDataUtils.addTAGNoRepeat(szSelectTAGFilter);
        }
        mConfigDataUtils.saveConfigData();
        mAdapterSelectedTAG.notifyDataSetChanged();
        restartSyncThreadByConfig();
    }

    void deleteTAGFilter() {
        String szSelectTAGFilter = mSpinnerAllTAG.getSelectedItem().toString();
        if (szSelectTAGFilter.equals(SZ_ALL)) {
            mConfigDataUtils.mConfigData.mArrayListStringTAG.clear();
        } else {
            mConfigDataUtils.deleteTAG(szSelectTAGFilter);
        }
        mConfigDataUtils.saveConfigData();
        mAdapterSelectedTAG.notifyDataSetChanged();
        restartSyncThreadByConfig();
    }

    /*public void onKeepSyncLog(MenuItem menuItem) {
        boolean isChecked = menuItem.isChecked();

        mConfigDataUtils.setIsSyncLogAndSave(!isChecked);

        menuItem.setChecked(!isChecked);

        restartSyncThreadByConfig();
    }*/

    static class ConfigData implements Serializable {
        public ArrayList<String> mArrayListStringTAG;
        public int mnLogLevel;
        public boolean mIsSyncLog;
        public ConfigData() {
            mArrayListStringTAG = new ArrayList<String>();
            mnLogLevel = 0;
            mIsSyncLog = false;
        }
    }
    static class ConfigDataUtils {
        volatile static ConfigDataUtils _mConfigDataUtils;
        ConfigData mConfigData;

        private ConfigDataUtils() {
        }

        public static synchronized ConfigDataUtils getInstance() {
            if (_mConfigDataUtils == null) {
                _mConfigDataUtils = new ConfigDataUtils();
                _mConfigDataUtils.loadConfigData();
            }
            return _mConfigDataUtils;
        }

        void loadConfigData() {
            ConfigData configData = null;
            try {
                ObjectInput input = new ObjectInputStream(new FileInputStream(mszConfigDataPath));
                configData = (ConfigData)input.readObject();
                input.close();
            } catch (ClassNotFoundException e) {
                Log.d(TAG, "loadConfigData() ClassNotFoundException : " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "loadConfigData() IOException : " + e.getMessage());
            }
            if (configData == null) {
                configData = new ConfigData();
            }

            mConfigData = configData;
        }

        public void saveConfigData() {
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(mszConfigDataPath));
                objectOutputStream.writeObject(mConfigData);
                objectOutputStream.close();
            } catch (IOException e) {
                Log.d(TAG, "saveConfigData() IOException :" + e.getMessage());
            }
        }

        public void setArrayListStringTAGAndSave(ArrayList<String> arrayListStringTAG) {
            mConfigData.mArrayListStringTAG.clear();
            for (int i = 0; i < arrayListStringTAG.size(); i++) {
                mConfigData.mArrayListStringTAG.add(arrayListStringTAG.get(i));
            }
            saveConfigData();
        }

        public void addTAGNoRepeat(String szTAG) {
            for (int i = 0; i < mConfigData.mArrayListStringTAG.size(); i++) {
                if (mConfigData.mArrayListStringTAG.get(i).equals(szTAG)) {
                    return;
                }
            }
            mConfigData.mArrayListStringTAG.add(szTAG);
        }

        public void deleteTAG(String szTAG) {
            for (int i = 0; i < mConfigData.mArrayListStringTAG.size(); i++) {
                if (mConfigData.mArrayListStringTAG.get(i).equals(szTAG)) {
                    mConfigData.mArrayListStringTAG.remove(i);
                    return;
                }
            }
        }

        public void setIsSyncLogAndSave(boolean isSyncLog) {
            mConfigData.mIsSyncLog = isSyncLog;
            Log.d(TAG, "mConfigData.mIsSyncLog is " + Boolean.toString(mConfigData.mIsSyncLog));
            saveConfigData();
        }

        public boolean getIsSyncLog() {
            return mConfigData.mIsSyncLog;
        }

        public void setLogLevelAndSave(int nLogLevel) {
            mConfigData.mnLogLevel = nLogLevel;
            saveConfigData();
        }

        public int getLogLevel() {
            return mConfigData.mnLogLevel;
        }

        public String getLogLevel_LocatFlag() {
            return ":" + szarrayLogViewLevel[mConfigData.mnLogLevel];
        }

        public String getLocatString() {
            String szResult = "logcat -v time";
            String szLogLevel_LocatFlag = getLogLevel_LocatFlag();
            //Log.d(TAG, "szLogLevel_LocatFlag is "+szLogLevel_LocatFlag);

            ArrayList<String> arrayListStringTAG = new ArrayList<String>();
            for (int i = 0; i < mConfigData.mArrayListStringTAG.size(); i++) {
                arrayListStringTAG.add(mConfigData.mArrayListStringTAG.get(i) + szLogLevel_LocatFlag);
            }
            if (arrayListStringTAG.size() > 0) {
                szResult += " -s " + StringUtils.join(arrayListStringTAG, ",");
            } else if (!szLogLevel_LocatFlag.equals("")) {
                szResult += " -s  *" + szLogLevel_LocatFlag;
            }

            Log.d(TAG, "szResult is " + szResult);
            //此串正解
            //szResult = "logcat -v time -s Test1:I,LogViewFragment:I";
            //szResult = "logcat -v time -s LogViewFragment:V";
            return szResult;
        }
    }


    @Override
    public void onDestroy() {
        stopSyncThread();

        if (mHandler != null) {
            mHandler.removeMessages(MyHandler.MSG_APPEN_LOG_TEXT);
            mHandler.removeMessages(MyHandler.MSG_APPEN_LOGBUFFER_TEXT);
            mHandler.removeMessages(MyHandler.MSG_APPEN_LOGTAG_TEXT);
            mHandler.removeMessages(MyHandler.MSG_APPEN_MSG_TEXT);
            mHandler.removeMessages(MyHandler.MSG_CLEAR_ALL_TEXT);
            mHandler.removeMessages(MyHandler.MSG_SHOW_LOGBUFFER_TEXT);
        }

        super.onDestroy();
    }

// 显示提示信息
    void showMSG(String sz) {
// 显示数据
        mtvLogView.append(sz + "\n");
        scrollTextView();
    }

    void scrollTextView() {
        final ScrollView sv = msvLog;
        sv.post(new Runnable() {
                @Override
                public void run() {
                    sv.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
    }

// 拾取log标签。
//
    public void addLogTAG(String sz) {
// 分组字符串
        String[] szlist = sz.split(" ");

        if (szlist.length > 2 && !szlist[2].trim().equals("") && szlist[2].contains("/")) {
// 添加未有的Log标签数据
            boolean isExistTAGFilter = false;
            String szTAG = "";
            if (szlist[2].indexOf("(") != -1) {
                szTAG = szlist[2].substring(2, szlist[2].indexOf("("));
            } else if (szlist[2].indexOf(" ") != -1) {
                szTAG = szlist[2].substring(2, szlist[2].indexOf(" "));
            } else {
                szTAG = szlist[2].substring(2, szlist[2].length());
            }
            for (int i = 1; i < mArrayListStringAllTAG.size(); i++) {
                if (szTAG.compareTo(mArrayListStringAllTAG.get(i)) == 0) {
                    isExistTAGFilter = true;
                    break;
                }
            }
            if (isExistTAGFilter == false) {
                mArrayListStringAllTAG.add(szTAG);
                Collections.sort(mArrayListStringAllTAG, new SortChineseName());
                mAdapterAllTAG.notifyDataSetChanged();
            }
        }
    }

// 显示最后mn_lastlogtemp_show_count条log数据
//
    void showLastLogTempToTextView() {
// mszlist_lastlogtemp没数据就退出
        if (mArrayListStringLastLogTemp == null) {
            return;
        }
// 显示数据
        int nAll = mArrayListStringLastLogTemp.size();
        if (nAll > 0) {
            int nStart = ((nAll - mnLastLogTemp_ShowNumber) < 0) ?0: (nAll - mnLastLogTemp_ShowNumber);
            for (int i=nStart;i < nAll;i++) {
                mtvLogView.append("\n" + mArrayListStringLastLogTemp.get(i));
            }
        }
// 清mszlist_lastlogtemp，减少内存占用
        mArrayListStringLastLogTemp.clear();


// 滚屏
        scrollTextView();
    }

    void showLogToTextView(String sz) {
// 显示数据
        mtvLogView.append("\n" + sz);
// 滚屏
        scrollTextView();
    }

    void appenToLogBuffer(String sz) {
// 如果mszlist_lastlogtemp有启用，
// 存储log数据到mszlist_lastlogtemp。
        if (mArrayListStringLastLogTemp != null) {
            mArrayListStringLastLogTemp.add(sz);
        }
    }

    class SortChineseName implements Comparator<String> {
        public SortChineseName() {
        }
        Collator cmp = Collator.getInstance(java.util.Locale.CHINA);
        @Override
        public int compare(String o1, String o2) {
            if (cmp.compare(o1, o2) > 0) {
                return 1;
            } else if (cmp.compare(o1, o2) < 0) {
                return -1;
            }
            return 0;
        }
    }

    class MyHandler extends Handler {

        public static final int MSG_APPEN_LOG_TEXT = 0;
        public static final int MSG_APPEN_LOGBUFFER_TEXT = 1;
        public static final int MSG_SHOW_LOGBUFFER_TEXT = 3;
        public static final int MSG_APPEN_LOGTAG_TEXT = 4;
        public static final int MSG_APPEN_MSG_TEXT = 5;
        public static final int MSG_CLEAR_ALL_TEXT = 6;

        WeakReference<LogViewFragment> fragmentWeakReference; // 持有外部类的弱引用
        public MyHandler(LogViewFragment fragment) {
            fragmentWeakReference = new WeakReference<LogViewFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_APPEN_LOG_TEXT: // 处理下载完成消息，更新UI
                    {
                        // 显示log数据
                        //
                        LogViewFragment handlerAppLogFragment = fragmentWeakReference.get();
                        if (handlerAppLogFragment != null) {
                            handlerAppLogFragment.showLogToTextView((String)msg.obj);
                        }
                        break;
                    }
                case MSG_APPEN_LOGBUFFER_TEXT: // 处理下载完成消息，更新UI
                    {
                        // 显示log数据
                        //
                        LogViewFragment handlerAppLogFragment = fragmentWeakReference.get();
                        if (handlerAppLogFragment != null) {
                            handlerAppLogFragment.appenToLogBuffer((String)msg.obj);
                        }
                        break;
                    }
                case MSG_APPEN_LOGTAG_TEXT: // 处理下载完成消息，更新UI
                    {
                        // 拾取log标签。
                        //
                        LogViewFragment handlerAppLogFragment = fragmentWeakReference.get();
                        if (handlerAppLogFragment != null) {
                            handlerAppLogFragment.addLogTAG((String)msg.obj);
                        }
                        break;
                    }
                case MSG_APPEN_MSG_TEXT: // 处理下载完成消息，更新UI
                    {
                        LogViewFragment handlerAppLogFragment = fragmentWeakReference.get();
                        if (handlerAppLogFragment != null) {
                            handlerAppLogFragment.showMSG((String)msg.obj);
                        }
                        break;
                    }
                case MSG_CLEAR_ALL_TEXT:
                    {
                        LogViewFragment handlerAppLogFragment = fragmentWeakReference.get();
                        if (handlerAppLogFragment != null) {
                            handlerAppLogFragment.mtvLogView.setText("");
                            //handlerAppLogFragment.mtvLog.setText("正在准备 : " + getDateNowString());
                        }
                        break;
                    }
                case MSG_SHOW_LOGBUFFER_TEXT:
                    {
                        LogViewFragment handlerAppLogFragment = fragmentWeakReference.get();
                        if (handlerAppLogFragment != null) {
                            handlerAppLogFragment.showLastLogTempToTextView();
                        }
                        break;
                    }
            }
        }
    }

// 实时读取log，并记录新加的标签
    class RealTimeLoadAllLogThread extends Thread { // 将Thread声明为static，非静态内部类或者匿名类默认持有外部类对象的引用，容易造成内存泄露
        //volatile修饰符用来保证其它线程读取的总是该变量的最新的值
        public volatile boolean exit = false;

        WeakReference<Handler> handlerWeakReference;
        public RealTimeLoadAllLogThread(Handler handler) { // 采用弱引用的方式持有Activity中Handler对象的引用，避免内存泄露
            handlerWeakReference = new WeakReference<Handler>(handler);
        }

        // 拾取log标签
        //
        void sendLogTAG(String sz) {
            Message message1 = Message.obtain();
            message1.what = MyHandler.MSG_APPEN_LOGTAG_TEXT;
            message1.obj = sz;
            Handler handler1 = handlerWeakReference.get();
            if (!exit && handler1 != null) {
                handler1.sendMessage(message1);
            }
        }

        @Override
        public void run() {
            java.lang.Process mLogcatProc = null;
            BufferedReader reader = null;
            try {
                //获取logcat日志信息
                //mLogcatProc = Runtime.getRuntime().exec("logcat " + LogActivity.getTAGFilter() + ":D *:S");
                mLogcatProc = Runtime.getRuntime().exec("logcat -v time *:V");
                reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));
                String szLineNew;
                // 以下代码包函Looper
                Looper.prepare();
                while (!exit && (szLineNew = reader.readLine()) != null) {
                    sendLogTAG(szLineNew);
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


    }

    class RealTimeLoadSettingLogThread extends Thread { // 将Thread声明为static，非静态内部类或者匿名类默认持有外部类对象的引用，容易造成内存泄露
        public final static String TAG = "RealTimeLoadLogThread";

        //volatile修饰符用来保证其它线程读取的总是该变量的最新的值
        public volatile boolean exit = false;
        // 延迟显示控制变量,
        // 用来解决数据量过大的问题
        volatile boolean isOpenLogShow = false;
        int nOpenLogShowDelaySecond = 1;

        WeakReference<Handler> handlerWeakReference;
        public RealTimeLoadSettingLogThread(Handler handler) { // 采用弱引用的方式持有Activity中Handler对象的引用，避免内存泄露
            Log.d(TAG, "call RealTimeLoadLogThread(Handler handler)");
            handlerWeakReference = new WeakReference<Handler>(handler);
        }

        // 显示log数据
        //
        void sendLog(String sz) {
            Message message1 = Message.obtain();
            message1.what = MyHandler.MSG_APPEN_LOG_TEXT;
            message1.obj = sz;
            Handler handler1 = handlerWeakReference.get();
            if (!exit && handler1 != null) {
                handler1.sendMessage(message1);
            }
        }

        // 添加log数据到缓冲区
        //
        void sendLogToBuffer(String sz) {
            Message message1 = Message.obtain();
            message1.what = MyHandler.MSG_APPEN_LOGBUFFER_TEXT;
            message1.obj = sz;
            Handler handler1 = handlerWeakReference.get();
            if (!exit && handler1 != null) {
                handler1.sendMessage(message1);
            }
        }

        void sendMSG(String sz) {
            Message message1 = Message.obtain();
            message1.what = MyHandler.MSG_APPEN_MSG_TEXT;
            message1.obj = sz;
            Handler handler1 = handlerWeakReference.get();
            if (!exit && handler1 != null) {
                handler1.sendMessage(message1);
            }
        }

        void sendShowLastLog() {
            Message message1 = Message.obtain();
            message1.what = MyHandler.MSG_SHOW_LOGBUFFER_TEXT;

            Handler handler1 = handlerWeakReference.get();
            if (!exit && handler1 != null) {
                handler1.sendMessage(message1);
            }
        }

        // 延迟三秒后开启log显示
        class OpenLogShowThread extends Thread {
            @Override
            public void run() {
                try {
                    Thread.sleep(nOpenLogShowDelaySecond * 1000);
                    sendShowLastLog();
                    Thread.sleep(200);
                    sendMSG("\n正在等待新的log数据");
                    isOpenLogShow = true;
                } catch (InterruptedException e) {}
            }
        }

        @Override
        public void run() {
            java.lang.Process mLogcatProc = null;
            BufferedReader reader = null;
            try {
                sendMSG("开始Log : " + getDateNowString());
                sendMSG(Integer.toString(nOpenLogShowDelaySecond) + "秒后开启log显示。");
                // 延迟三秒后开启log显示
                OpenLogShowThread openLogShowThread = new OpenLogShowThread();
                openLogShowThread.start();

                //获取logcat日志信息
                //mLogcatProc = Runtime.getRuntime().exec("logcat " + LogActivity.getTAGFilter() + ":D *:S");
                mLogcatProc = Runtime.getRuntime().exec(mConfigDataUtils.getLocatString());
                reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));
                String szLineNew;
                // 以下代码包函Looper
                Looper.prepare();
                while (!exit && (szLineNew = reader.readLine()) != null) {
                    if (isOpenLogShow) {
                        sendLog(szLineNew);
                    } else {
                        sendLogToBuffer(szLineNew);
                    }
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


    }

    class CleanLogThread extends Thread { // 将Thread声明为static，非静态内部类或者匿名类默认持有外部类对象的引用，容易造成内存泄露
        WeakReference<Handler> handlerWeakReference;
        public CleanLogThread(Handler handler) { // 采用弱引用的方式持有Activity中Handler对象的引用，避免内存泄露
            handlerWeakReference = new WeakReference<Handler>(handler);
        }

        void sendMSG(String sz) {
            Message message1 = Message.obtain();
            message1.what = MyHandler.MSG_APPEN_MSG_TEXT;
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
                message1.what = MyHandler.MSG_CLEAR_ALL_TEXT;
                Handler handler1 = handlerWeakReference.get();
                if (handler1 != null) {
                    handler1.sendMessage(message1);
                }
                sendMSG("正在清理旧Log : " + getDateNowString());
                java.lang.Process procClean = Runtime.getRuntime().exec("logcat -c");
                procClean.waitFor();
                sendMSG("清理结束 : " + getDateNowString());
                startSyncThreadByConfig();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }

        }
    }

    // 获取当前时间的格式化字符串
    public String getDateNowString() {
        // 读取当前时间
        long nTimeNow = System.currentTimeMillis();
        return mSimpleDateFormat.format(nTimeNow);
    }
}


