package com.example.cliff.tsplayer;

import android.util.Log;

/**
 * Created by CLIFF on 2017/5/10.
 */

public class ProgramMapTable {
    public static final String TAG = "Cliff";

    public int table_id;                 // 8 bits
    public int section_syntax_indicator; // 1 bits
    public int zero;                     // 1 bits
    public int reserved_1;               // 2 bits
    public int section_length;           // 12 bits
    public int program_number;           // 16 bits
    public int reserved_2;               // 2 bits
    public int version_number;           // 5 bits
    public int current_next_indicator;   // 1 bits
    public int section_number;           // 8 bits
    public int last_section_number;      // 8 bits
    public int reserved_3;               // 3 bits
    public int pcr_pid;                  // 13 bits
    public int reserved_4;               // 4 bits
    public int program_info_length;      // 12 bits
    public int crc_32;                   // 32 bits
    public int unread_size; // auxiliary data to count stream info

    public ProgramMapTable(){
        table_id = 0;
        section_syntax_indicator = 0;
        zero = 0;
        reserved_1 = 0;
        section_length = 0;
        program_number = 0;
        reserved_2 = 0;
        version_number = 0;
        current_next_indicator = 0;
        section_number = 0;
        last_section_number = 0;
        reserved_3 = 0;
        pcr_pid = 0;
        reserved_4 = 0;
        program_info_length = 0;
        crc_32 = 0;
        unread_size = 0;
    }

    public void printPmt(){
        Log.i(TAG, "############ PMT Table Header ###########");
        Log.i(TAG, String.format("table_id                 = %d", table_id));
        Log.i(TAG, String.format("section_syntax_indicator = %d", section_syntax_indicator));
        Log.i(TAG, String.format("zero                     = %d", zero));
        Log.i(TAG, String.format("reserved_1               = %d", reserved_1));
        Log.i(TAG, String.format("section_length           = %d", section_length));
        Log.i(TAG, String.format("program_number           = %d", program_number));
        Log.i(TAG, String.format("reserved_2               = %d", reserved_2));
        Log.i(TAG, String.format("version_number           = %d", version_number));
        Log.i(TAG, String.format("current_next_indicator   = %d", current_next_indicator));
        Log.i(TAG, String.format("section_number           = %d", section_number));
        Log.i(TAG, String.format("last_section_number      = %d", last_section_number));
        Log.i(TAG, String.format("reserved_3               = %d", reserved_3));
        Log.i(TAG, String.format("pcr_pid                  = %d", pcr_pid));
        Log.i(TAG, String.format("reserved_4               = %d", reserved_4));
        Log.i(TAG, String.format("program_info_length      = %d", program_info_length));
        Log.i(TAG, "########## PMT Table Header End #########");
    }
}
