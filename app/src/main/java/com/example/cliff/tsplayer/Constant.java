package com.example.cliff.tsplayer;

/**
 * Created by CLIFF on 2017/5/10.
 */

public class Constant {
    public static final int PES_PACKET_START_CODE = 0x000001;

    // PSI (Program specific information)
    public static final int PID_PAT = 0x0000;

    // Stream Type
    public static final int STREAM_TYPE_VIDEO_H264 = 0x1B;
    public static final int STREAM_TYPE_VIDEO_AAC = 0x0F;

    // PES Stream ID Black List
    public static final int PROGRAM_STREAM_MAP = 0xBC;
    public static final int PADDING_STREAM = 0xBE;
    public static final int PRIVATE_STREAM_2 = 0xBF;
    public static final int ECM = 0xF0;
    public static final int EMM = 0xF1;
    public static final int PROGRAM_STREAM_DIRECTORY = 0xFF;
    public static final int DSMCC_STREAM = 0xF2;
    public static final int ITU_T_REC_H2221_TYPE_E_STREAM = 0xF8;

    // PES separate way
    public static final int SEPARATE_PES_BY_PES_LENGTH = 1;
    public static final int SEPARATE_PES_BY_PES_START_CODE = 2;
}
