package com.example.cliff.tsplayer;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Cliff";

    String inputPath;
    TsPacket packet;
    ReadFileRunnable readFileWork;
    HandlerThread handlerThread = new HandlerThread("HandlerThread");
    Handler handler;

    int ret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        packet = new TsPacket();
        inputPath = Environment.getExternalStorageDirectory().getPath() + "/Mi.ts";

        readFileWork = new ReadFileRunnable(inputPath, packet);
        ret = readFileWork.openFile();
        if(ret >= 0){
            handler.post(readFileWork);
        }

    }
}
