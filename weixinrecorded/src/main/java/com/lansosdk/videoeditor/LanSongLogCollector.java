package com.lansosdk.videoeditor;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * android logcat收集器
 *
 *
 * 杭州蓝松科技有限公司
 * www.lansongtech.com
 */
public class LanSongLogCollector implements Runnable {
    private static final String TAG = "LanSongLogCollector";
    private Process process;

    private Context context;
    private List<ProcessInfo> processInfoList;

    private boolean isRunning;

    public LanSongLogCollector(Context ctx) {
        context = ctx;
    }

    public synchronized boolean start() {
        if (!isRunning) {
            Thread thread = new Thread(this);
            thread.start();
            waitUntilReady();
        }
        return isRunning;
    }

    @Override
    public void run() {

        try {
            isRunning=false;
            runEntry();
        } catch (Exception e) {
            e.printStackTrace();
            notifyReady();
        }
    }

    public synchronized boolean isRunning() {
        return this.isRunning;
    }

    public synchronized String stop() {
        if (process != null) {
            process.destroy();
        }
        if (processInfoList != null) {
            String packName = context.getPackageName();
            String myUser = getAppUser(packName, processInfoList);

            for (ProcessInfo processInfo : processInfoList) {
                if (processInfo.name.toLowerCase().equals("logcat")
                        && processInfo.user.equals(myUser)) {
                    android.os.Process.killProcess(Integer
                            .parseInt(processInfo.pid));
                    //recordLogServiceLog("kill another logcat process success,the process info is:"
                    //      + processInfo);
                    isRunning = false;
                }
            }
        }
        if (logFilePath != null) {
            String ret = readFile();
            LanSongFileUtil.deleteFile(logFilePath);
            logFilePath = null;
            isRunning = false;
            return ret;
        }
        return null;
    }

    public void runEntry() throws Exception {

            if(LanSongFileUtil.fileExist(logFilePath)){
                LanSongFileUtil.deleteFile(logFilePath);
                logFilePath=null;
            }

            //1.清除日志缓存
            clearLogCache();

            //杀死应用程序已开启的Logcat进程防止多个进程写入一个日志文件
            List<String> orgProcessList = getAllProcess();

            processInfoList = getProcessInfoList(orgProcessList);
            killLogcatProc(processInfoList);

            //开启日志收集进程
            createLogCollector();
            isRunning = true;
            notifyReady();
    }
    private final Object mLock = new Object();
    private volatile boolean mReady = false;
    private void waitUntilReady() {
        synchronized (mLock) {
            mReady = false;
            try {
                mLock.wait(3 * 000);
            } catch (InterruptedException ie) { /* not expected */
            }
        }
    }

    private void notifyReady() {
        synchronized (mLock) {
            mReady = true;
            mLock.notify(); // signal waitUntilReady()
        }
    }

    /**
     * 每次记录日志之前先清除日志的缓存, 不然会在两个日志文件中记录重复的日志
     */
    private void clearLogCache() {
        Process proc = null;
        List<String> commandList = new ArrayList<String>();
        commandList.add("logcat");
        commandList.add("-c");
        try {
            proc = Runtime.getRuntime().exec(commandList.toArray(new String[commandList.size()]));
            StreamConsumer errorGobbler = new StreamConsumer(proc.getErrorStream());
            StreamConsumer outputGobbler = new StreamConsumer(proc.getInputStream());

            errorGobbler.start();
            outputGobbler.start();
            if (proc.waitFor() != 0) {
                Log.e(TAG, " clearLogCache proc.waitFor() != 0");
            }
        } catch (Exception e) {
            Log.e(TAG, "clearLogCache failed", e);
        } finally {
            try {
                proc.destroy();
            } catch (Exception e) {
                Log.e(TAG, "clearLogCache failed", e);
            }
        }
    }

    private void killLogcatProc(List<ProcessInfo> allProcList) {
        if (process != null) {
            process.destroy();
        }
        String packName = context.getPackageName();
        String myUser = getAppUser(packName, allProcList);

        for (ProcessInfo processInfo : allProcList) {
            if (processInfo.name.toLowerCase().equals("logcat")
                    && processInfo.user.equals(myUser)) {
                android.os.Process.killProcess(Integer
                        .parseInt(processInfo.pid));
                //recordLogServiceLog("kill another logcat process success,the process info is:"
                //      + processInfo);
            }
        }
        if (logFilePath != null) {
            LanSongFileUtil.deleteFile(logFilePath);
            logFilePath = null;
        }
    }

    /**
     * 获取本程序的用户名称
     *
     * @param packName
     * @param allProcList
     * @return
     */
    private String getAppUser(String packName, List<ProcessInfo> allProcList) {
        for (ProcessInfo processInfo : allProcList) {
            if (processInfo.name.equals(packName)) {
                return processInfo.user;
            }
        }
        return null;
    }

    /**
     * 根据ps命令得到的内容获取PID，User，name等信息
     *
     * @param orgProcessList
     * @return
     */
    private List<ProcessInfo> getProcessInfoList(List<String> orgProcessList) {
        List<ProcessInfo> procInfoList = new ArrayList<ProcessInfo>();
        for (int i = 1; i < orgProcessList.size(); i++) {
            String processInfo = orgProcessList.get(i);
            String[] proStr = processInfo.split(" ");
            // USER PID PPID VSIZE RSS WCHAN PC NAME
            // root 1 0 416 300 c00d4b28 0000cd5c S /init
            List<String> orgInfo = new ArrayList<String>();
            for (String str : proStr) {
                if (!"".equals(str)) {
                    orgInfo.add(str);
                }
            }
            if (orgInfo.size() == 9) {
                ProcessInfo pInfo = new ProcessInfo();
                pInfo.user = orgInfo.get(0);
                pInfo.pid = orgInfo.get(1);
                pInfo.ppid = orgInfo.get(2);
                pInfo.name = orgInfo.get(8);
                procInfoList.add(pInfo);
            }
        }
        return procInfoList;
    }

    /**
     * 运行PS命令得到进程信息
     *
     * @return USER PID PPID VSIZE RSS WCHAN PC NAME
     * root 1 0 416 300 c00d4b28 0000cd5c S /init
     */
    private List<String> getAllProcess() {
        List<String> orgProcList = new ArrayList<String>();
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec("ps");
            StreamConsumer errorConsumer = new StreamConsumer(proc
                    .getErrorStream());

            StreamConsumer outputConsumer = new StreamConsumer(proc
                    .getInputStream(), orgProcList);

            errorConsumer.start();
            outputConsumer.start();
            if (proc.waitFor() != 0) {
                Log.e(TAG, "getAllProcess proc.waitFor() != 0");
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllProcess failed", e);
        } finally {
            try {
                proc.destroy();
            } catch (Exception e) {
                Log.e(TAG, "getAllProcess failed", e);
            }
        }
        return orgProcList;
    }

    String logFilePath;

    /**
     * 开始收集日志信息
     */
    public void createLogCollector()  throws Exception {
        List<String> commandList = new ArrayList<String>();
        commandList.add("logcat");
        commandList.add("-f");
        logFilePath = LanSongFileUtil.createFileInBox("log");
        Log.i(TAG, "createLogCollector: LSTODO file:" + logFilePath);

        commandList.add(logFilePath);

        commandList.add("-v");
        commandList.add("time");
//        commandList.add("*:w");

        commandList.add("*:E");// 过滤所有的错误信息

        // 过滤指定TAG的信息
//        commandList.add("LanSoJni:E");
        // commandList.add("*:S");


        process = Runtime.getRuntime().exec(commandList.toArray(new String[commandList.size()]));

    }


    /**
     * 拷贝文件
     *
     * @return
     */
    private String readFile() {
        String str2 = "";
        try {
            StringBuilder stringBuilder = new StringBuilder();

            FileReader fr = new FileReader(logFilePath);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);


            str2 = localBufferedReader.readLine();
            while (str2 != null) {
                Log.e(TAG, "readFile: str2:"+str2 );

                stringBuilder.append(str2);
                stringBuilder.append("\r\n");

                str2 = localBufferedReader.readLine();
            }
            localBufferedReader.close();
            return stringBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    class ProcessInfo {
        public String user;
        public String pid;
        public String ppid;
        public String name;

        @Override
        public String toString() {
            String str = "user=" + user + " pid=" + pid + " ppid=" + ppid
                    + " name=" + name;
            return str;
        }
    }

    class StreamConsumer extends Thread {
        InputStream is;
        List<String> list;

        StreamConsumer(InputStream is) {
            this.is = is;
        }

        StreamConsumer(InputStream is, List<String> list) {
            this.is = is;
            this.list = list;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String line = null;
                while ((line = br.readLine()) != null && line.contains(context.getPackageName())) {
                    if (list != null) {
                        list.add(line);
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (process != null) {
            process.destroy();
        }
    }
}