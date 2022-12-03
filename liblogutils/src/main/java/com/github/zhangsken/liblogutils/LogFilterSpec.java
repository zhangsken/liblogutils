package com.github.zhangsken.liblogutils;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import android.content.pm.PackageManager;
import java.io.IOException;
import android.util.Log;
import android.content.Context;

// 日志筛选描述类
//
public class LogFilterSpec {

    public static final String TAG = "LogFilterSpec";



    // 日志筛选级别
    public static enum LogLevel { V, D, I, W, E, A, F, S };

    private LogLevel mLogLevel;
    private String mszTAG;

    public void setLogLevel(LogLevel logLevel) {
        this.mLogLevel = logLevel;
    }

    public LogLevel getLogLevel() {
        return mLogLevel;
    }

    public void setTAG(String szTAG) {
        this.mszTAG = szTAG;
    }

    public String getTAG() {
        return mszTAG;
    }

    public LogFilterSpec(LogLevel logLevel, String szTAG) {
        this.mLogLevel = logLevel;
        this.mszTAG = szTAG;
    }

    public String getFilterSpecString() {
        return mszTAG + ":" + mLogLevel.name();
    }

    // 筛选类数组内容标准化
    // 1.减去空字符标签的数据项
    //
    /*static void standardizeListLogFilterSpec(ArrayList<LogFilterSpec> listLogFilterSpec) {

     // 减去空字符标签的数据项
     for (int i = listLogFilterSpec.size() - 1; i > 0; i--) {
     if(listLogFilterSpec.get(i).mszTAG.trim().equals("")) {
     listLogFilterSpec.remove(i);
     Log.d(TAG, "OK1");
     }
     }
     }*/

    // 建立实时logcat命令
    //
    public static String buildRealTimeLogcatString(ArrayList<LogFilterSpec> listLogFilterSpec, LogFilterSpec.LogLevel levelDefault) {
        levelDefault = (levelDefault == null)? LogFilterSpec.LogLevel.V : levelDefault;
        
        //standardizeListLogFilterSpec(listLogFilterSpec);
        // << 调试数据 >>
        // 取得最后100条数据，并且继续捕获数据。
        //szResult = "logcat -T 100 -v time -s " + TAG + ":D";
        //szResult = "logcat -T 100 -v time *:S " + TAG + ":D";
        // 取得最后100条数据，并且然后结束logcat。
        //szResult = "logcat -t 100 -s " + TAG + ":D";
        //szResult = "logcat -T 100 -v time -s " + TAG + ":D";
        //szResult = "logcat -v time -s Test:I,LogViewFragment:I";
        //szResult = "logcat -v time -s Test:D";
        //szResult = "logcat -t '2021-12-30 17:10:03.879' -v time -s *:S";
        //szResult = "logcat -v time -s Test:D ";
        //szResult = "logcat -T 100 -v time -s Test:D";

        // -T 0 ：读取历史数据
        String szResult = "logcat -v time";
        // -T 0 ：不读取历史数据
        //String szResult = "logcat -T 0 -v time";


        int nCountValidTAG = 0;
        if ((listLogFilterSpec != null)
            &&(listLogFilterSpec.size() > 0)) {
            for (int i = 0; i < listLogFilterSpec.size(); i++) {
                if (!listLogFilterSpec.get(i).mszTAG.trim().equals("")) {
                    szResult += " " + listLogFilterSpec.get(i).getFilterSpecString();
                    nCountValidTAG++;
                }
            }
        } 
        if (nCountValidTAG > 0) {
            szResult += " *:" + LogLevel.S.name();
        } else {
            szResult += " *:" + ((levelDefault == null) ?levelDefault.name(): LogFilterSpec.LogLevel.V.name());
        }
        return szResult;
    }

    // 检查szCheck标签的类是否已经在数组里
    //
    static boolean checkTagClassExist(ArrayList<LogFilterSpec> listLogFilterSpec, String szCheck) {
        for (int i = 0; i < listLogFilterSpec.size(); i++) {
            if (listLogFilterSpec.get(i).mszTAG.equals(szCheck)) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<LogFilterSpec> buildLogFilterSpecArrayList(Context context, String szSpec, LogLevel logLevel) {
        ArrayList<LogFilterSpec> listResult = new ArrayList<LogFilterSpec>();
        String[] szList = szSpec.split(",");
        for (int i = 0; i < szList.length; i++) {
            listResult.add(new LogFilterSpec(logLevel, szList[i]));
        }

        // 附加功能 : 遍举应用内的类名进行筛选 (可选部分)
        /*if(listResult.size() == 0) {
         // 加入 APP 内所有 ClassSimpleName 为 TAG 的日志筛选类
         try {
         Set<String> setString = ClassUtils.getFileNameByPackageName(context.getApplicationContext(), context.getApplicationContext().getPackageName());
         Iterator it = setString.iterator();
         while (it.hasNext()) {
         Object obj = it.next();
         if (obj instanceof String) {
         String szTemp = ((String)obj).replaceAll(".*[\\.]+", "");
         szTemp = szTemp.replaceAll("\\$+.*", "");
         if (!checkTagClassExist(listResult, szTemp)) {
         listResult.add(new LogFilterSpec(logLevel, szTemp));
         }

         }
         }

         } catch (PackageManager.NameNotFoundException e) {
         Log.d(TAG, "LogView PackageManager.NameNotFoundException : " + e.getMessage());
         } catch (IOException e) {
         Log.d(TAG, "LogView IOException : " + e.getMessage());
         } catch (InterruptedException e) {
         Log.d(TAG, "LogView InterruptedException : " + e.getMessage());
         }
         }*/

        //standardizeListLogFilterSpec(listResult);

        return listResult;
    }
}
