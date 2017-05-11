package com.example.cliff.tsplayer;

import android.util.Log;

/**
 * Created by CLIFF on 2017/5/11.
 */

public class AdaptationField {
    public static final String TAG = "Cliff";

    public int adaptation_field_length;              // 8 bits
    public int discontinuity_indicator;              // 1 bit
    public int random_access_indicator;              // 1 bit
    public int elementary_stream_priority_indicator; // 1 bit
    public int pcr_flag;                             // 1 bit
    public int opcr_flag;                            // 1 bit
    public int splicing_point_flag;                  // 1 bit
    public int transport_private_data_flag;          // 1 bit
    public int adaptation_field_extension_flag;      // 1 bit

    public AdaptationField(){
        adaptation_field_length = 0;
        discontinuity_indicator = 0;
        random_access_indicator = 0;
        elementary_stream_priority_indicator = 0;
        pcr_flag = 0;
        opcr_flag = 0;
        splicing_point_flag = 0;
        transport_private_data_flag = 0;
        adaptation_field_extension_flag = 0;
    }

    void printAdaptationField(){
        Log.i(TAG, String.format("adaptation_field_length              = %d", adaptation_field_length));
        Log.i(TAG, String.format("discontinuity_indicator              = %d", discontinuity_indicator));
        Log.i(TAG, String.format("random_access_indicator              = %d", random_access_indicator));
        Log.i(TAG, String.format("elementary_stream_priority_indicator = %d", elementary_stream_priority_indicator));
        Log.i(TAG, String.format("pcr_flag                             = %d", pcr_flag));
        Log.i(TAG, String.format("opcr_flag                            = %d", opcr_flag));
        Log.i(TAG, String.format("splicing_point_flag                  = %d", splicing_point_flag));
        Log.i(TAG, String.format("transport_private_data_flag          = %d", transport_private_data_flag));
        Log.i(TAG, String.format("adaptation_field_extension_flag      = %d", adaptation_field_extension_flag));
    }
}
