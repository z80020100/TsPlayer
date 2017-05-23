package com.example.cliff.tsplayer;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Cliff";

    String inputPath;
    ReadFileRunnable readFileWork;
    HandlerThread handlerThread;
    Handler handler;

    int ret;

    // dynamic data buffer
    TsPacket packet;
    PsiPointer psi_pointer_data;

    // permanent
    ProgramAssociationTable pat;
    ProgramMapTable pmt;
    PmtStreamInfo stream_info_h264, stream_info_aac;
    SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        handlerThread = new HandlerThread("ReadHandlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        inputPath = Environment.getExternalStorageDirectory().getPath() + "/DocCam.ts";

        // dynamic data buffer
        packet = new TsPacket();
        psi_pointer_data = new PsiPointer();

        // permanent
        pat = new ProgramAssociationTable();
        pmt = new ProgramMapTable();
        stream_info_h264 = new PmtStreamInfo();
        stream_info_aac = new PmtStreamInfo();

        //readFileWork = new ReadFileRunnable(inputPath, packet, psi_pointer_data, pat, pmt, stream_info_h264, stream_info_aac);
        //ret = readFileWork.openFile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.play_dump) {
            if(readFileWork != null){
                readFileWork.stop();
            }
            inputPath = Environment.getExternalStorageDirectory().getPath() + "/Download/output.ts";
            pat = new ProgramAssociationTable();
            pmt = new ProgramMapTable();
            stream_info_h264 = new PmtStreamInfo();
            stream_info_aac = new PmtStreamInfo();

            readFileWork = new ReadFileRunnable(inputPath, packet, psi_pointer_data, pat, pmt, stream_info_h264, stream_info_aac);
            ret = readFileWork.openFile();
            if(ret == 0){
                readFileWork.setSurfaceView(mSurfaceView);
                handler.post(readFileWork);
            }
            else{
                Toast.makeText(this, "Open file: " + inputPath + "failed!", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if(id == R.id.play_doccam){
            if(readFileWork != null){
                readFileWork.stop();
            }
            inputPath = Environment.getExternalStorageDirectory().getPath() + "/DocCam.ts";
            pat = new ProgramAssociationTable();
            pmt = new ProgramMapTable();
            stream_info_h264 = new PmtStreamInfo();
            stream_info_aac = new PmtStreamInfo();

            readFileWork = new ReadFileRunnable(inputPath, packet, psi_pointer_data, pat, pmt, stream_info_h264, stream_info_aac);
            ret = readFileWork.openFile();
            if(ret == 0){
                readFileWork.setSurfaceView(mSurfaceView);
                handler.post(readFileWork);
            }
            else{
                Toast.makeText(this, "Open file: " + inputPath + "failed!", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if(id == R.id.play_mi){
            if(readFileWork != null){
                readFileWork.stop();
            }
            inputPath = Environment.getExternalStorageDirectory().getPath() + "/Mi_audio.ts";
            pat = new ProgramAssociationTable();
            pmt = new ProgramMapTable();
            stream_info_h264 = new PmtStreamInfo();
            stream_info_aac = new PmtStreamInfo();

            readFileWork = new ReadFileRunnable(inputPath, packet, psi_pointer_data, pat, pmt, stream_info_h264, stream_info_aac);
            ret = readFileWork.openFile();
            if(ret == 0){
                readFileWork.setSurfaceView(mSurfaceView);
                handler.post(readFileWork);
            }
            else{
                Toast.makeText(this, "Open file: " + inputPath + "failed!", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if(id == R.id.play_acer){
            if(readFileWork != null){
                readFileWork.stop();
            }
            inputPath = Environment.getExternalStorageDirectory().getPath() + "/Acer.ts";
            pat = new ProgramAssociationTable();
            pmt = new ProgramMapTable();
            stream_info_h264 = new PmtStreamInfo();
            stream_info_aac = new PmtStreamInfo();

            readFileWork = new ReadFileRunnable(inputPath, packet, psi_pointer_data, pat, pmt, stream_info_h264, stream_info_aac);
            ret = readFileWork.openFile();
            if(ret == 0){
                readFileWork.setSurfaceView(mSurfaceView);
                handler.post(readFileWork);
            }
            else{
                Toast.makeText(this, "Open file: " + inputPath + "failed!", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
