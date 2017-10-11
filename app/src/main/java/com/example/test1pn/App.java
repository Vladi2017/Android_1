package com.example.test1pn;

import android.app.Application;

/**
 * Created by Vladi on 10/5/17.
 */

public class App extends Application {
    static final String TAG1 = "App";
    static java.io.BufferedWriter bufW1;
    private StackTraceElement[] arrSTE;
    private Throwable causedBy;
    private StackTraceElement[] causedByArrSTE;
    @Override
    public void onCreate() {
        super.onCreate();
        java.io.File logFile = new java.io.File("sdcard/test1pn_log.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (java.io.IOException e) {
                android.util.Log.i("App.onCreate", "Vl3.at logFile.createNewFile(): " + e.toString());
            }
        }
        logFile.setWritable(/*writable*/true, /*ownerOnly*/false);
        try {
            bufW1 = new java.io.BufferedWriter(
                    new java.io.FileWriter(logFile, /*append*/true));
        } catch (java.io.IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Setup handler for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable e) {
                String textSTEs = "";
                // e.printStackTrace(); // not all Android versions will print
                // the stack trace automatically
//                android.util.Log.i(TAG1, "V. from my uncaughtException(..): " + e.toString());
                textSTEs += ("\n" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        .format(new java.util.Date()) + "\n");
                textSTEs += TAG1 + ", Vl1. from my defaultUncaughtExceptionHandler(..):" + e.toString()
                        + ", Vl1.Throwable detail message: " + e.getMessage() + "\n";
                arrSTE = e.getStackTrace();
//                for (StackTraceElement ste : arrSTE)
//                    Vsupport1.textFileLog(ste.toString(), bufW1);
                for (StackTraceElement ste : arrSTE) textSTEs += (ste.toString() + "\n");
                textSTEs += "\nVl. caused by:\n";
                causedBy = e.getCause();
                causedByArrSTE = causedBy.getStackTrace();
                for (StackTraceElement ste : causedByArrSTE) textSTEs += (ste.toString() + "\n");
                Vsupport1.textFileLog(textSTEs, bufW1);
                try {
//                    bufW1.flush();
                    bufW1.close();
                } catch (java.io.IOException e1) {
                    android.util.Log.i("my_uncaughtException", "Vl2.at java.io.BufferedWriter operations: " + e.toString());
                }
//				System.exit(1); // kill off the crashed app
            }
        });
        Vsupport1.textFileLog(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date()) + "\nVl4. App class, setDefaultUncaughtExceptionHandler done.\n", bufW1);
    }
}
