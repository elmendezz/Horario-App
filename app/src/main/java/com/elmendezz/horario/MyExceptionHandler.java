package com.elmendezz.horario;

import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final AppCompatActivity myActivity;

    public MyExceptionHandler(AppCompatActivity activity) {
        myActivity = activity;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();

        Log.e("CRASH_REPORT", "\n==================== CRASH DETECTADO ====================\n" + stackTrace);
        Log.e("CRASH_REPORT", "=========================================================");

        // Cierra la app
        System.exit(1);
    }
}