package com.example.cliff.tsplayer;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.example.cliff.tsplayer.Constant.SEPARATE_PES_BY_PES_LENGTH;
import static com.example.cliff.tsplayer.Constant.SEPARATE_PES_BY_PES_START_CODE;

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
    AdaptationField adaptation_field_data;
    PesHeader pes_header_buf;

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

    private int readBytes, ts_unread_size;
    private int ret;

    public ReadFileRunnable(String inputPath, TsPacket packet, PsiPointer psi_pointer_data, ProgramAssociationTable pat, ProgramMapTable pmt, PmtStreamInfo stream_info_h264, PmtStreamInfo stream_info_aac){
        this.inputPath = inputPath;
        this.packet = packet;
        this.psi_pointer_data = psi_pointer_data;
        this.pat = pat;
        this.pmt = pmt;
        this.stream_info_h264 = stream_info_h264;
        this.stream_info_aac = stream_info_aac;
        this.adaptation_field_data = new AdaptationField();
        this.pes_header_buf = new PesHeader();
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
                packet.readAdaptationField(adaptation_field_data);
                adaptation_field_data.printAdaptationField();
                packet.packet_info.payload_size = 184 - adaptation_field_data.adaptation_field_length - 1; // 1: minus sizeof(adaptation_field_length)
                Log.i(TAG, String.format("TS packet payload size = %d", packet.packet_info.payload_size));
                Log.i(TAG, String.format("Skip: stuffing data %d bytes", adaptation_field_data.adaptation_field_length - 1));
                packet.tsPacketSkipReadByte(adaptation_field_data.adaptation_field_length - 1); // adaptation_field_length = sizeof(adaptation parameter) + sizeof(stuffing_data)
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
                    break; // Once find PMT, stop detect PMT in this packet because one packet only can include one PMT
                }
            }

            // skip process PCR
            if(detect_pcr_pid == 1 && packet.header_info.pid == pmt.pcr_pid){
                Log.e(TAG, "Skip: PCR Packet");
            }

            // process PES packet with H.264 stream
            if(detect_h264_stream == 1 && packet.header_info.pid == stream_info_h264.elementary_pid){
                Log.i(TAG, "H.264 Packet");

                if(detect_pes_start == 1){

                }
                else{
                    if(packet.ReadBits(packet, 24) == Constant.PES_PACKET_START_CODE &&
                            packet.header_info.payload_unit_start_indicator == 1){
                        Log.i(TAG, "Found PES start code!");
                        detect_pes_start = 1;
                        ret = packet.readPesHeader(pes_header_buf);
                        if(ret == 0){
                            pes_header_buf.printPesHeader();
                            // skip the optional fields and any stuffing bytes contained in this PES packet header, include PTS, DTS
                            packet.tsPacketSkipReadByte(pes_header_buf.pes_header_data_length);
                            // unread bytes for TS packet = the size of packet - current bit / 8
                            ts_unread_size = packet.packet_info.packet_size - packet.packet_info.current_bit/8;
                            Log.i(TAG, String.format("Unread size for TS packet = %d", ts_unread_size));

                            if(pes_header_buf.pes_packet_length != 0){
                                pes_separate_way = SEPARATE_PES_BY_PES_LENGTH;
                                Log.i(TAG, String.format("Separate PES by PES packet length = %d", pes_header_buf.pes_packet_length));
                            }
                            else{
                                pes_separate_way = SEPARATE_PES_BY_PES_START_CODE;
                            }
                        }
                        else{
                            detect_pes_start = 0;
                            Log.e(TAG, "Skip Unsupported PES");
                        }

                    }
                }

            }

        }
        else{
            Log.e(TAG, "Read TS packet error!");
        }
    }
}
