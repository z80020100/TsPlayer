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

    // dynamic data buffer
    TsPacket packet;
    PsiPointer psi_pointer_data;
    PmtStreamInfo stream_info_buf;

    // permanent
    ProgramAssociationTable pat;
    ProgramMapTable pmt;
    PmtStreamInfo stream_info_h264, stream_info_aac;

    // flag
    int detect_h264_stream = 0;
    int detect_aac_stream = 0;
    int detect_pcr_pid = 0;
    int detect_pes_start = 0;
    int pes_separate_way;

    private int readBytes;

    public ReadFileRunnable(String inputPath, TsPacket packet, PsiPointer psi_pointer_data, ProgramAssociationTable pat, ProgramMapTable pmt, PmtStreamInfo stream_info_h264, PmtStreamInfo stream_info_aac){
        this.inputPath = inputPath;
        this.packet = packet;
        this.psi_pointer_data = psi_pointer_data;
        this.pat = pat;
        this.pmt = pmt;
        this.stream_info_h264 = stream_info_h264;
        this.stream_info_aac = stream_info_aac;
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
        packet.packet_info.current_bit = 0;
        try {
            readBytes = fileInputBuf.read(packet.ts_data, 0, packet.packet_info.packet_size);
            if(readBytes != packet.packet_info.packet_size){
                return -1;
            }
            packet.packet_info.packet_number++;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void run() {
        if(readPacket() != -1){
            //packet.printRawData();
            packet.readHeaderInfo();
            packet.printHeaderInfo();

            if(packet.header_info.adaptation_field_control == 2 ||
               packet.header_info.adaptation_field_control == 3) {
                Log.e(TAG, "TODO: parse adaptation field");
            }
            else{
                packet.packet_info.payload_size = 184;
            }

            // Parse pointer_field for PAT
            if(packet.header_info.pid == Constant.PID_PAT){
                Log.i(TAG, "PSI type: PAT");
                if(packet.header_info.payload_unit_start_indicator == 1){
                    Log.i(TAG, "Info: parse pointer_field for PSI section (PAT)");
                    packet.readPsiPointer(psi_pointer_data);
                    psi_pointer_data.printPsiPointer();
                }
                else{
                    Log.i(TAG, "Info: PSI section (PAT) without pointer_field");
                }

                // Parse PAT
                packet.readPat(pat);
                pat.printPat();
            }

            // Detect and parse PMT
            if(pat.channel_number > 1){
                Log.e(TAG, "The number of channel > 1");
            }
            else if(pat.channel_number < 1){
                Log.e(TAG, "The number of channel < 1");
            }

            for(int i = 0; i < pat.channel_number; i++){
                if(packet.header_info.pid == pat.program_info_array[i].program_map_pid){
                    Log.i(TAG, String.format("Detect PMT, PID = %d", packet.header_info.pid));
                    if(packet.header_info.payload_unit_start_indicator == 1){
                        Log.i(TAG, "Info: parse pointer_field for PSI section (PMT)");
                        packet.readPsiPointer(psi_pointer_data);
                        psi_pointer_data.printPsiPointer();
                    }
                    else{
                        Log.i(TAG, "Info: PSI section (PMT) without pointer_field");
                    }

                    packet.readPmt(pmt);
                    detect_pcr_pid = 1;
                    pmt.printPmt();
                    if(pmt.program_info_length > 0){
                        // TODO: parse descriptor
                        Log.e(TAG, "TODO: parse descriptor");
                        Log.e(TAG, "Skip parse descriptor");
                        packet.tsPacketSkipReadByte(pmt.program_info_length);
                    }

                    // read stream info from PMT
                    while(pmt.unread_size >= 9) // 9: stream_info physical data size (40 bits) + CRC32 (32 bits)
                    {
                        // reset StreamInfoBuf
                        stream_info_buf = new PmtStreamInfo();
                        packet.readPmtStreamInfo(stream_info_buf, pmt);
                        stream_info_buf.printPmtStreamInfo();
                        if(stream_info_buf.es_info_length > 0){
                            // TODO: parse descriptor, use skip as workaround
                            Log.e(TAG, "Skip parse descriptor");
                            packet.tsPacketSkipReadByte(stream_info_buf.es_info_length);
                            pmt.unread_size = pmt.unread_size - stream_info_buf.es_info_length;
                        }

                        // store stream info for H264
                        if(stream_info_buf.stream_type == Constant.STREAM_TYPE_VIDEO_H264){
                            PmtStreamInfo.PmtStreamInfoCopy(stream_info_buf, stream_info_h264);
                            Log.i(TAG, String.format("Detect Video Stream: H.264, PID = %d", stream_info_h264.elementary_pid));
                            detect_h264_stream = 1;
                        }

                        // store stream info for AAC
                        if(stream_info_buf.stream_type == Constant.STREAM_TYPE_VIDEO_AAC){
                            PmtStreamInfo.PmtStreamInfoCopy(stream_info_buf, stream_info_aac);
                            Log.i(TAG, String.format("Detect Audio Stream: AAC, PID = %d", stream_info_aac.elementary_pid));
                            detect_aac_stream = 1;
                        }
                    }
                    if(pmt.unread_size == 4){
                        packet.readPmtCrc32(pmt);
                    }
                    else{
                        Log.e(TAG, String.format("pmt.unread_size = %d is incorrect, parse PMT failed...", pmt.unread_size));
                    }

                }
            }

        }
        else{
            Log.e(TAG, "Read TS packet error!");
        }
    }
}
