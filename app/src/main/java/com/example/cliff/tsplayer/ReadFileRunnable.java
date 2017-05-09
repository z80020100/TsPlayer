package com.example.cliff.tsplayer;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by CLIFF on 2017/5/9.
 */

public class ReadFileRunnable implements Runnable {
    public static final String TAG = "Cliff";

    private String inputPath;
    private File inputFile;
    private FileInputStream fileInputStram;
    private BufferedInputStream fileInputBuf;

    private TsPacket packet;

    ReadFileRunnable(String inputPath, TsPacket packet){
        this.inputPath = inputPath;
        this.packet = packet;
    }

    public int openFile(){
        Log.i(TAG, "Open file: " + inputPath);
        inputFile = new File(inputPath);
        if(inputFile.exists()){
            try {
                fileInputStram = new FileInputStream(inputFile);
                fileInputBuf = new BufferedInputStream(fileInputStram);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return -1;
            }
        }
        else{
            Log.e(TAG, "Open file failed: " + inputPath);
            return -1;
        }
        return 0;
    }

    public int readPacket(){
        try {
            fileInputBuf.read(packet.ts_data, 0, packet.packet_info.packet_size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        packet.packet_info.packet_number++;
        return 0;
    }

    @Override
    public void run() {
        readPacket();
        packet.printRawData();
    }
}
