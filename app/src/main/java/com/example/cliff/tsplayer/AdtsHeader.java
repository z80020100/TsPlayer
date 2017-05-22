package com.example.cliff.tsplayer;

import android.util.Log;

/**
 * Created by CLIFF on 2017/5/22.
 */

public class AdtsHeader {
    public static final String TAG = "Cliff";

    public adts_fixed_header adts_fixed_header_data;
    public adts_variable_header adts_variable_header_data;
    public int read_bit;

    public AdtsHeader(){
        adts_fixed_header_data = new adts_fixed_header();
        read_bit = 0;
    }

    public void read_adts_fixed_header(byte[] aac_pes_payload){
        adts_fixed_header_data.syncword                 = ReadBits(aac_pes_payload, 12); // 12 bits
        adts_fixed_header_data.id                       = ReadBits(aac_pes_payload, 1);  // 1 bit
        adts_fixed_header_data.layer                    = ReadBits(aac_pes_payload, 2);  // 2 bits, always be '00'
        adts_fixed_header_data.protection_absent        = ReadBits(aac_pes_payload, 1);  // 1 bit
        adts_fixed_header_data.profile                  = ReadBits(aac_pes_payload, 2);  // 2 bits
        adts_fixed_header_data.sampling_frequency_index = ReadBits(aac_pes_payload, 4);  // 4 bits
        adts_fixed_header_data.private_bit              = ReadBits(aac_pes_payload, 1);  // 1 bit
        adts_fixed_header_data.channel_configuration    = ReadBits(aac_pes_payload, 3);  // 3 bits
        adts_fixed_header_data.original_copy            = ReadBits(aac_pes_payload, 1);  // 1 bit
        adts_fixed_header_data.home                     = ReadBits(aac_pes_payload, 1);  // 1 bit
    }

    public void print_adts_fixed_header(){
        Log.i(TAG, String.format("syncword                 = 0x%03X", adts_fixed_header_data.syncword));
        Log.i(TAG, String.format("id                       = %d", adts_fixed_header_data.id));
        Log.i(TAG, String.format("layer                    = %d", adts_fixed_header_data.layer));
        Log.i(TAG, String.format("protection_absent        = %d", adts_fixed_header_data.protection_absent));
        Log.i(TAG, String.format("profile                  = %d", adts_fixed_header_data.profile));
        Log.i(TAG, String.format("sampling_frequency_index = %d", adts_fixed_header_data.sampling_frequency_index));
        Log.i(TAG, String.format("private_bit              = %d", adts_fixed_header_data.private_bit));
        Log.i(TAG, String.format("private_bit              = %d", adts_fixed_header_data.private_bit));
        Log.i(TAG, String.format("original_copy            = %d", adts_fixed_header_data.original_copy));
        Log.i(TAG, String.format("home                     = %d", adts_fixed_header_data.home));
    }

    private int ReadBit(byte[] aac_pes_payload) // 讀出currentBit所在位置的bit value, currentBit = 0 for the first bit
    {
        int nIndex = read_bit / 8;
        int nOffset = read_bit % 8 + 1;
        read_bit++;
        //Log.i(TAG, "nIndex = " + nIndex + ", nOffect = " + nOffset);
        return (aac_pes_payload[nIndex] >> (8-nOffset)) & 0x01;
    }

    public int ReadBits(byte[] aac_pes_payload, int n) // 從currentBit所在位置的bit讀出後n個bit之值
    {
        int r = 0;
        int i;
        for (i = 0; i < n; i++)
        {
            r |= ( ReadBit(aac_pes_payload) << ( n - i - 1 ) );
        }
        return r;
    }

    private class adts_fixed_header{
        int syncword;                 // 12 bits
        int id;                       // 1 bit
        int layer;                    // 2 bits, always be '00'
        int protection_absent;        // 1 bit
        int profile;                  // 2 bits
        int sampling_frequency_index; // 4 bits
        int private_bit;              // 1 bit
        int channel_configuration;    // 3 bits
        int original_copy;            // 1 bit
        int home;                     // 1 bit
    }

    private class adts_variable_header{
        int copyright_identification_bit;       // 1 bit
        int copyright_identification_start;     // 1 bit
        int acc_frame_length;                   // 13 bits
        int adts_buffer_fullness;               // 11 bits
        int number_of_raw_data_blocks_in_frame; // 2 bits
    }
}
