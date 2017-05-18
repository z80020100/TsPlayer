package com.example.cliff.tsplayer;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.example.cliff.tsplayer.Constant.PES_PACKET_START_CODE;
import static com.example.cliff.tsplayer.Constant.SEPARATE_PES_BY_PES_LENGTH;
import static com.example.cliff.tsplayer.Constant.SEPARATE_PES_BY_PES_START_CODE;
import static com.example.cliff.tsplayer.Constant.SPEC_MAX_PES_LENGTH;

/**
 * Created by CLIFF on 2017/5/9.
 */

public class ReadFileRunnable implements Runnable {
    public static final String TAG = "Cliff";

    private String inputPath;
    private File inputFile;
    private FileInputStream fileInputStram;
    private BufferedInputStream fileInputBuf;

    FileOutputStream fos = null;
    private String DumpVidePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +"/output.264";;

    H264DecoderRunnable decodeNaluWork;
    HandlerThread handlerThread;
    Handler handler;

    // dynamic data buffer
    TsPacket packet;
    PsiPointer psi_pointer_data;
    PmtStreamInfo stream_info_buf;
    AdaptationField adaptation_field_data;
    PesHeader pes_header_buf;
    PesPayload pes_packet;

    byte[] pes_data_buf;

    // permanent
    ProgramAssociationTable pat;
    ProgramMapTable pmt;
    PmtStreamInfo stream_info_h264, stream_info_aac;
    SurfaceView mSurfaceView;

    // flag
    int detect_h264_stream = 0;
    int detect_aac_stream = 0;
    int detect_pcr_pid = 0;
    int detect_pes_start = 0;
    int pes_separate_way;

    private int readBytes, ts_unread_size, pes_payload_size, allocate_pes_size = SPEC_MAX_PES_LENGTH;
    private int ret;
    public boolean loop_ctrl = true;
    private int h264_continuity_cnt = -1;

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


        handlerThread = new HandlerThread("DecodeHandlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public void setSurfaceView(SurfaceView surfaceView){
        mSurfaceView = surfaceView;
        decodeNaluWork = new H264DecoderRunnable(mSurfaceView);
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
        try {
            fos = new FileOutputStream(DumpVidePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int readPacket(){
        packet.packet_info.current_bit = 0;
        try {
            readBytes = fileInputBuf.read(packet.ts_data, 0, packet.packet_info.packet_size);
            if(readBytes == -1){
                return readBytes; // return -1 if the end of the file is reached
            }
            if(readBytes != packet.packet_info.packet_size){
                return -2;
            }
            packet.packet_info.packet_number++;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void run() {
        while(loop_ctrl){
            ret = readPacket();
            if(ret == 0){
                //packet.printRawData();
                packet.readHeaderInfo();
                //packet.printHeaderInfo();

                if(packet.header_info.adaptation_field_control == 2 ||
                        packet.header_info.adaptation_field_control == 3) {
                    packet.readAdaptationField(adaptation_field_data);
                    //adaptation_field_data.printAdaptationField();
                    packet.packet_info.payload_size = 184 - adaptation_field_data.adaptation_field_length - 1; // 1: minus sizeof(adaptation_field_length)
                    //Log.i(TAG, String.format("TS packet payload size = %d", packet.packet_info.payload_size));
                    //Log.i(TAG, String.format("Skip: stuffing data %d bytes", adaptation_field_data.adaptation_field_length - 1));
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
                        //psi_pointer_data.printPsiPointer();
                    }
                    else{
                        Log.i(TAG, "Info: PSI section (PAT) without pointer_field");
                    }

                    // Parse PAT
                    packet.readPat(pat);
                    //pat.printPat();
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
                            //psi_pointer_data.printPsiPointer();
                        }
                        else{
                            Log.i(TAG, "Info: PSI section (PMT) without pointer_field");
                        }

                        packet.readPmt(pmt);
                        detect_pcr_pid = 1;
                        //pmt.printPmt();
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
                            //stream_info_buf.printPmtStreamInfo();
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
                    //Log.e(TAG, "Skip: PCR Packet");
                }

                // process PES packet with H.264 stream
                if(detect_h264_stream == 1 && packet.header_info.pid == stream_info_h264.elementary_pid){
                    //Log.i(TAG, "H.264 Packet");
                    if(h264_continuity_cnt == -1){
                        h264_continuity_cnt = packet.header_info.continuity_counter;
                    }
                    else{
                        if(packet.header_info.continuity_counter == 0 && h264_continuity_cnt == 15){
                            h264_continuity_cnt = packet.header_info.continuity_counter;
                        }
                        else if(packet.header_info.continuity_counter != h264_continuity_cnt +1){
                            Log.e(TAG, String.format("h264_continuity_cnt = %d, packet.header_info.continuity_counter = %d", h264_continuity_cnt, packet.header_info.continuity_counter));
                            Log.e(TAG, "Loss RTP packet!");

                            if(detect_pes_start == 1){
                                Log.e(TAG, "Parse incomplete PES packet...");
                                detect_pes_start = 0;
                                // write data to file or send to decoder
                                decodeNaluWork.setNalu(pes_packet.pes_data);
                                decodeNaluWork.decede();
                                pes_packet.copied_byte = 0;
                                pes_packet.pes_payload_length = 0;
                            }
                            h264_continuity_cnt = packet.header_info.continuity_counter;
                        }
                        else{
                            h264_continuity_cnt = packet.header_info.continuity_counter;
                        }
                    }

                    if(detect_pes_start == 1){
                        if(pes_separate_way == SEPARATE_PES_BY_PES_START_CODE){
                            // TODO: implementation
                            if(packet.ReadBits(packet, 24) == PES_PACKET_START_CODE &&
                                    packet.header_info.payload_unit_start_indicator == 1){
                                //Log.i(TAG, "Found next PES, separate it!");
                                //Log.i(TAG, "Parse one PES packet complete!");

                                // check NALU start code
                                if(pes_packet.pes_data[0] != 0x00 ||
                                    pes_packet.pes_data[1] != 0x00 ||
                                    pes_packet.pes_data[2] != 0x00 ||
                                    pes_packet.pes_data[3] != 0x01){
                                    Log.e(TAG, "Check NALU start code! (0x%08X)");
                                }


                                // write data to file or send to decoder

                                decodeNaluWork.copyNalu(pes_packet.pes_data, pes_packet.copied_byte);
                                //decodeNaluWork.setNalu(pes_packet.pes_data);
                                decodeNaluWork.decede();

                                /*
                                try {
                                    fos.write(pes_packet.pes_data, 0, pes_packet.copied_byte);
                                    fos.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                */

                                pes_packet.copied_byte = 0;

                                ret = packet.readPesHeader(pes_header_buf);
                                if(ret == 0){
                                    //pes_header_buf.printPesHeader();
                                    // skip the optional fields and any stuffing bytes contained in this PES packet header, include PTS, DTS
                                    packet.tsPacketSkipReadByte(pes_header_buf.pes_header_data_length);
                                    // unread bytes for TS packet = the size of packet - current bit / 8
                                    ts_unread_size = packet.packet_info.packet_size - packet.packet_info.current_bit/8;
                                    //Log.i(TAG, String.format("Unread size for TS packet = %d", ts_unread_size));
                                }
                                else{
                                    detect_pes_start = 0;
                                    Log.e(TAG, "Skip Unsupported PES");
                                }
                                //system("PAUSE");
                            }
                        else{
                                //Log.i(TAG, "Seek back 3 bytes");
                                packet.packet_info.current_bit = packet.packet_info.current_bit - 24;
                            }
                        }

                        //Log.i(TAG, "Copy PES fragment");
                        ts_unread_size = packet.packet_info.packet_size - packet.packet_info.current_bit/8;
                        //Log.i(TAG, String.format("Unread size for TS packet = %d", ts_unread_size));
                        if(ts_unread_size + pes_packet.copied_byte > allocate_pes_size){
                            // TODO: implementation
                            Log.e(TAG, String.format("Expected PES packet is %d, over allocated memory size %d", ts_unread_size + pes_packet.copied_byte, allocate_pes_size));
                            allocate_pes_size = allocate_pes_size*2;
                            Log.e(TAG, String.format("Re-allocate PES memory size: %d", allocate_pes_size));
                            pes_data_buf = pes_packet.pes_data;
                            pes_packet.pes_data = new byte[allocate_pes_size];
                            System.arraycopy(pes_data_buf, 0, pes_packet.pes_data, 0, pes_packet.copied_byte);
                            pes_data_buf = null;
                        }
                        int expected_size = pes_packet.copied_byte + ts_unread_size;
                        if(expected_size > pes_packet.pes_payload_length){
                            Log.e(TAG, String.format("expected_size = %d,  pes_packet.pes_payload_length = %d", expected_size, pes_packet.pes_payload_length));
                            Log.e(TAG, "Parse incomplete PES packet...");
                            detect_pes_start = 0;
                            // write data to file or send to decoder
                            decodeNaluWork.setNalu(pes_packet.pes_data);
                            decodeNaluWork.decede();
                            pes_packet.copied_byte = 0;
                            pes_packet.pes_payload_length = 0;
                        }
                        else{
                            Log.i(TAG, String.format("expected_size = %d,  pes_packet.pes_payload_length = %d", expected_size, pes_packet.pes_payload_length));
                            packet.copyPesPayloadFromTs(pes_packet, packet.packet_info.current_bit/8, ts_unread_size);
                        }
                        //pes_packet.printRawData();
                        //Log.i(TAG, String.format("PES payload copied bytes = %d", pes_packet.copied_byte));
                    }
                    else{
                        if(packet.ReadBits(packet, 24) == PES_PACKET_START_CODE &&
                                packet.header_info.payload_unit_start_indicator == 1){
                            //Log.i(TAG, "Found PES start code!");
                            detect_pes_start = 1;
                            ret = packet.readPesHeader(pes_header_buf);
                            if(ret == 0){
                                //pes_header_buf.printPesHeader();
                                // skip the optional fields and any stuffing bytes contained in this PES packet header, include PTS, DTS
                                packet.tsPacketSkipReadByte(pes_header_buf.pes_header_data_length);
                                // unread bytes for TS packet = the size of packet - current bit / 8
                                ts_unread_size = packet.packet_info.packet_size - packet.packet_info.current_bit/8;
                                //Log.i(TAG, String.format("Unread size for TS packet = %d", ts_unread_size));

                                if(pes_header_buf.pes_packet_length != 0){
                                    pes_separate_way = SEPARATE_PES_BY_PES_LENGTH;
                                    //Log.i(TAG, String.format("Separate PES by PES packet length = %d", pes_header_buf.pes_packet_length));
                                    // PES payload size = pes_packet_length - sizeof(stream_id) - sizeof(pes_packet_length) - pes_header_data_length
                                    pes_payload_size = pes_header_buf.pes_packet_length - 3 - pes_header_buf.pes_header_data_length;
                                    pes_packet = new PesPayload(pes_payload_size);
                                    //Log.i(TAG, String.format("PES payload size  = %d", pes_packet.pes_payload_length));
                                    packet.copyPesPayloadFromTs(pes_packet, packet.packet_info.current_bit/8, ts_unread_size);
                                    //pes_packet.printRawData();
                                    //Log.i(TAG, String.format("PES payload copied bytes = %d", pes_packet.copied_byte));
                                }
                                else{
                                    pes_separate_way = SEPARATE_PES_BY_PES_START_CODE;
                                    //Log.i(TAG, String.format("Separate PES by PES start code for PES packet length = %d", pes_header_buf.pes_packet_length));
                                    pes_packet = new PesPayload(allocate_pes_size);
                                    packet.copyPesPayloadFromTs(pes_packet, packet.packet_info.current_bit/8, ts_unread_size);
                                    //Log.i(TAG, String.format("PES payload copied bytes = %d", pes_packet.copied_byte));
                                }
                            }
                            else{
                                detect_pes_start = 0;
                                Log.e(TAG, "Skip Unsupported PES");
                            }
                        }
                    }

                    // Parse PES packet successfully
                    if((pes_packet.copied_byte == pes_packet.pes_payload_length) && (pes_separate_way == SEPARATE_PES_BY_PES_LENGTH)){
                        //Log.i(TAG, "Parse one PES packet complete!");
                        detect_pes_start = 0;

                        // send data to decoder
                        decodeNaluWork.setNalu(pes_packet.pes_data);
                        decodeNaluWork.decede();
                        //handler.post(decodeNaluWork);


                        /*
                        // write to file
                        try {
                            fos.write(pes_packet.pes_data, 0, pes_packet.pes_payload_length);
                            fos.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        */

                        pes_packet.copied_byte = 0;
                        pes_packet.pes_payload_length = 0;
                        //loop_ctrl = false;
                    }
                }

            }
            else if(ret == -2){
                Log.e(TAG, "Read TS packet error!");
                loop_ctrl = false;
                break;
            }
            else if(ret == -1){
                Log.i(TAG, "Read complete");
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                loop_ctrl = false;
                break;
            }
            else{
                Log.e(TAG, "Read TS packet error!");
                loop_ctrl = false;
                break;
            }
        }
        decodeNaluWork.closeMediaDecoder();
    }

    public void stop(){
        loop_ctrl = false;
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
