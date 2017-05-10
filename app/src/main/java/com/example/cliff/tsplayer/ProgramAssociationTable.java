package com.example.cliff.tsplayer;

import android.util.Log;

/**
 * Created by CLIFF on 2017/5/10.
 */

public class ProgramAssociationTable {
    public static final String TAG = "Cliff";

    public int table_id;                 // 8 bits
    public int section_syntax_indicator; // 1 bit, shall be set to '1'
    public int zero;                     // 1 bit
    public int reserved_1;               // 1 bit
    public int section_length;           // 12 bits
    public int transport_stream_id;      // 16 bits
    public  int reserved_2;              // 2 bit
    public int version_number;           // 5 bits
    public int current_next_indicator;   // 1 bit
    public int section_number;           // 8 bits
    public  int last_section_number;     // 8 bits
    ProgramInfo[] program_info_array;    // the length of this array is channel_number, each the size of element is 4 bytes
    public int crc_32;                   // 32 bits
    public  int channel_number;          // N in ISO13818-1 Table 2-25, N = (section_length-9)/4

    public ProgramAssociationTable(){
        table_id = 0;
        section_syntax_indicator = 0;
        zero = 0;
        reserved_1 = 0;
        section_length = 0;
        transport_stream_id = 0;
        reserved_2 = 0;
        version_number = 0;
        current_next_indicator = 0;
        section_number = 0;
        last_section_number = 0;
        crc_32 = 0;
        channel_number = 0;
    }

    public void allocateProgramInfoArray(){
        program_info_array = new ProgramInfo[channel_number];
        for(int i = 0; i < channel_number; i++){
            program_info_array[i] = new ProgramInfo();
        }
    }

    public void printPat(){
        Log.i(TAG, "################## PAT Data ###################");
        Log.i(TAG, String.format("Table ID                 = %d", table_id));
        Log.i(TAG, String.format("Section syntax indicator = %d", section_syntax_indicator));
        Log.i(TAG, String.format("Zero                     = %d", zero));
        Log.i(TAG, String.format("Reserved 1               = %d", reserved_1));
        Log.i(TAG, String.format("Section length           = %d", section_length));
        Log.i(TAG, String.format("Transport stream ID      = %d", transport_stream_id));
        Log.i(TAG, String.format("Reserved 2               = %d", reserved_2));
        Log.i(TAG, String.format("version number           = %d", version_number));
        Log.i(TAG, String.format("Current next indicator   = %d", current_next_indicator));
        Log.i(TAG, String.format("Section number           = %d", section_number));
        Log.i(TAG, String.format("Last section number      = %d", last_section_number));
        Log.i(TAG, String.format("Channel number           = %d", channel_number));

        for(int i = 0; i < channel_number; i++){
            Log.i(TAG, String.format("########## Channel %d ##########", i));
            Log.i(TAG, String.format("    Program number       = %d", program_info_array[i].program_number));
            Log.i(TAG, String.format("    Reserved             = %d", program_info_array[i].reserved));
            if(program_info_array[i].program_number == 0){
                Log.i(TAG, String.format("    Network PID      = %d", program_info_array[i].program_map_pid));
            }
            else{
                Log.i(TAG, String.format("    Program map PID      = %d", program_info_array[i].program_map_pid));
            }
            Log.i(TAG, "###############################");
        }

        Log.i(TAG, String.format("CRC32                    = 0x%08X", crc_32));
        Log.i(TAG, "###############################################");
    }

    public class ProgramInfo{
        public int program_number;
        public int reserved;
        public int program_map_pid;      // if program_number is 0, it becomes network_pid

        public ProgramInfo(){
            program_number = 0;
            reserved = 0;
            program_map_pid = 0;
        }
    }
}
