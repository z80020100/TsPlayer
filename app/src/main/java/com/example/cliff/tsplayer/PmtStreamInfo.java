package com.example.cliff.tsplayer;

import android.util.Log;

/**
 * Created by CLIFF on 2017/5/11.
 */

public class PmtStreamInfo {
    public static final String TAG = "Cliff";

    public int stream_type;    // 8bits
    public int reserved_1;     // 3 bits
    public int elementary_pid; // 13 bits
    public int reserved_2;     // 4 bits
    public int es_info_length; // 12 bits

    public PmtStreamInfo(){
        stream_type = 0;
        reserved_1 = 0;
        elementary_pid = 0;
        reserved_2 = 0;
        es_info_length = 0;
    }

    public void printPmtStreamInfo(){
        Log.i(TAG, "########## PMT Stream  ###########");
        Log.i(TAG, String.format("stream_type    = 0x%02X", stream_type));
        Log.i(TAG, String.format("reserved_1     = %d", reserved_1));
        Log.i(TAG, String.format("elementary_pid = %d", elementary_pid));
        Log.i(TAG, String.format("reserved_2     = %d", reserved_2));
        Log.i(TAG, String.format("es_info_length = %d", es_info_length));
        Log.i(TAG, "##################################");
    }

    public static void PmtStreamInfoCopy(PmtStreamInfo source, PmtStreamInfo target){
        target.stream_type = source.stream_type;
        target.reserved_1 = source.reserved_1;
        target.elementary_pid = source.elementary_pid;
        target.reserved_2 = source.reserved_2;
        target.es_info_length = source.es_info_length;
    }
}
