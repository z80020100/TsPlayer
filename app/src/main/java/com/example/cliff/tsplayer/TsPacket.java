package com.example.cliff.tsplayer;

import android.util.Log;

/**
 * Created by CLIFF on 2017/5/9.
 */

public class TsPacket{
    public static final String TAG = "Cliff";
    public static final int MAX_TS_PACKET_LENGTH = 208;

    public TsPacketInfo packet_info;
    public TsHeaderInfo header_info;
    public byte[] ts_data;

    TsPacket(){
        packet_info = new TsPacketInfo();
        header_info = new TsHeaderInfo();
        ts_data = new byte[MAX_TS_PACKET_LENGTH];
    }


    public class TsPacketInfo{
        public int packet_size;
        public int adaptation_size;
        public int payload_size; // N in ISO13818-1 Table 2-2, N = 184 - adaptation_size
        public int packet_number;
        public int current_bit; // auxiliary data to read bit value

        TsPacketInfo(){
            packet_size = 188; // TODO: support other TS packet size
            adaptation_size = 0;
            payload_size = 0;
            packet_number = 0;
            current_bit = 0;
        }
    }

    public class TsHeaderInfo{
        public int sync_byte;                    // 8 bits
        public int transport_error_indicator;    // 1 bit
        public int payload_unit_start_indicator; // 1 bit
        public int transport_priority;           // 1 bit
        public int pid;                          // 13 bits
        public int transport_scrambling_control; // 2 bits
        public int adaptation_field_control;     // 2 bits
        public int continuity_counter;           // 4 bits

        TsHeaderInfo(){
            sync_byte = 0;
            transport_error_indicator = 0;
            payload_unit_start_indicator = 0;
            transport_priority = 0;
            pid = 0;
            transport_scrambling_control = 0;
            adaptation_field_control = 0;
            continuity_counter = 0;
        }
    }

    private int ReadBit(TsPacket TsPacketObj) // 讀出currentBit所在位置的bit value, currentBit = 0 for the first bit
    {
        int nIndex = TsPacketObj.packet_info.current_bit / 8;
        int nOffset = TsPacketObj.packet_info.current_bit % 8 + 1;
        TsPacketObj.packet_info.current_bit++;
        //Log.i(TAG, "nIndex = " + nIndex + ", nOffect = " + nOffset);
        return (TsPacketObj.ts_data[nIndex] >> (8-nOffset)) & 0x01;
    }

    private int ReadBits(TsPacket rawParameterObj, int n) // 從currentBit所在位置的bit讀出後n個bit之值
    {
        int r = 0;
        int i;
        for (i = 0; i < n; i++)
        {
            r |= ( ReadBit(rawParameterObj) << ( n - i - 1 ) );
        }
        return r;
    }

    public void printRawData(){
        // Print TS packet raw data
        StringBuilder dataRowBuilder = new StringBuilder("");
        Log.i(TAG, String.format("################ Packet %05d #################", this.packet_info.packet_number));
        for(int i = 0; i < this.packet_info.packet_size; i++){
            dataRowBuilder.append(String.format("%02X ", this.ts_data[i]));
            if((i+1)%16 == 0){
                dataRowBuilder.append("\n");
            }
        }
        Log.i(TAG, dataRowBuilder.toString());
        Log.i(TAG, String.format("###############################################"));
    }


}
