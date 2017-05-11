package com.example.cliff.tsplayer;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Cliff";

    String inputPath;
    ReadFileRunnable readFileWork;
    HandlerThread handlerThread = new HandlerThread("HandlerThread");
    Handler handler;

    int ret;

    // dynamic data buffer
    TsPacket packet;
    PsiPointer psi_pointer_data;

    // permanent
    ProgramAssociationTable pat;
    ProgramMapTable pmt;
    PmtStreamInfo stream_info_h264, stream_info_aac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        inputPath = Environment.getExternalStorageDirectory().getPath() + "/Mi.ts";

        // dynamic data buffer
        packet = new TsPacket();
        psi_pointer_data = new PsiPointer();

        // permanent
        pat = new ProgramAssociationTable();
        pmt = new ProgramMapTable();
        stream_info_h264 = new PmtStreamInfo();
        stream_info_aac = new PmtStreamInfo();

        readFileWork = new ReadFileRunnable(inputPath, packet, psi_pointer_data, pat, pmt, stream_info_h264, stream_info_aac);
        ret = readFileWork.openFile();
        if(ret >= 0){
            handler.post(readFileWork);
            handler.post(readFileWork);
            handler.post(readFileWork);
            handler.post(readFileWork);
        }
    }
}
