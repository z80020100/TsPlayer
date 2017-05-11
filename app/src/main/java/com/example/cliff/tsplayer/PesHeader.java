package com.example.cliff.tsplayer;

import android.util.Log;

/**
 * Created by CLIFF on 2017/5/11.
 */

public class PesHeader {
    public static final String TAG = "Cliff";

    // skip packet_start_code_prefix
    public int stream_id;                 // 8 bits
    public int pes_packet_length;         // 16 bits
    // pes_packet_length = sizeof(stream_id) + sizeof(pes_packet_length) + pes_header_data_length + sizeof(pes_data)
    // exclude stream_id = enum PES_STREAM_ID above, the PES packet has the following field
    public int binary_10;                 // 2 bits
    public int pes_scrambling_control;    // 2 bits
    public int pes_priority;              // 1 bit
    public int data_alignment_indicator;  // 1 bit
    public int copyright;                 // 1 bit
    public int original_or_copy;          // 1 bit
    public int pts_dts_flags;             // 2 bits
    public int escr_flag;                 // 1 bit
    public int es_rate_flag;              // 1 bit
    public int dsm_trick_mode_flag;       // 1 bit
    public int additional_copy_info_flag; // 1 bit
    public int pes_crc_flag;              // 1 bit
    public int pes_extension_flag;        // 1 bit
    public int pes_header_data_length;    // 8 bit

    public PesHeader(){
        stream_id = 0;
        pes_packet_length = 0;
        binary_10 = 0;
        pes_scrambling_control = 0;
        pes_priority = 0;
        data_alignment_indicator = 0;
        copyright = 0;
        original_or_copy = 0;
        pts_dts_flags = 0;
        escr_flag = 0;
        es_rate_flag = 0;
        dsm_trick_mode_flag = 0;
        additional_copy_info_flag = 0;
        pes_crc_flag = 0;
        pes_extension_flag = 0;
        pes_header_data_length = 0;
    }

    public void printPesHeader(){
        Log.i(TAG, "########### PES Header ###########");
        Log.i(TAG, String.format("stream_id                 = 0x%02X", stream_id));
        Log.i(TAG, String.format("pes_packet_length         = %d", pes_packet_length));
        Log.i(TAG, String.format("binary_10                 = %d", binary_10));
        Log.i(TAG, String.format("pes_scrambling_control    = %d", pes_scrambling_control));
        Log.i(TAG, String.format("pes_priority              = %d", pes_priority));
        Log.i(TAG, String.format("data_alignment_indicator  = %d", data_alignment_indicator));
        Log.i(TAG, String.format("copyright                 = %d", copyright));
        Log.i(TAG, String.format("original_or_copy          = %d", original_or_copy));
        Log.i(TAG, String.format("pts_dts_flags             = %d", pts_dts_flags));
        Log.i(TAG, String.format("escr_flag                 = %d", escr_flag));
        Log.i(TAG, String.format("es_rate_flag              = %d", es_rate_flag));
        Log.i(TAG, String.format("dsm_trick_mode_flag       = %d", dsm_trick_mode_flag));
        Log.i(TAG, String.format("additional_copy_info_flag = %d", additional_copy_info_flag));
        Log.i(TAG, String.format("pes_crc_flag              = %d", pes_crc_flag));
        Log.i(TAG, String.format("pes_extension_flag        = %d", pes_extension_flag));
        Log.i(TAG, String.format("pes_header_data_length    = %d", pes_header_data_length));
        Log.i(TAG, "######### PES Header END #########");
    }
}
