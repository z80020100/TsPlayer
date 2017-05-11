package com.example.cliff.tsplayer;

import android.util.Log;

/**
 * Created by CLIFF on 2017/5/11.
 */

public class PesPayload {
    public static final String TAG = "Cliff";

    public int pes_payload_length;
    byte[]     pes_data;
    public int copied_byte;

    public PesPayload(int pes_payload_length){
        this. pes_payload_length = pes_payload_length;
        copied_byte = 0;
        pes_data = new byte[pes_payload_length];
    }

    public void printRawData(){
        // Print TS packet raw data
        StringBuilder dataRowBuilder = new StringBuilder("");
        Log.i(TAG, String.format("###############################################"));
        for(int i = 0; i < this.copied_byte; i++){
            dataRowBuilder.append(String.format("%02X ", this.pes_data[i]));
            if((i+1)%16 == 0){
                dataRowBuilder.append("\n");
            }
        }
        Log.i(TAG, dataRowBuilder.toString());
        Log.i(TAG, String.format("###############################################"));
    }
}
