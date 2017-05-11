package com.example.cliff.tsplayer;

import android.util.Log;

import static com.example.cliff.tsplayer.Constant.DSMCC_STREAM;
import static com.example.cliff.tsplayer.Constant.ECM;
import static com.example.cliff.tsplayer.Constant.EMM;
import static com.example.cliff.tsplayer.Constant.ITU_T_REC_H2221_TYPE_E_STREAM;
import static com.example.cliff.tsplayer.Constant.PADDING_STREAM;
import static com.example.cliff.tsplayer.Constant.PRIVATE_STREAM_2;
import static com.example.cliff.tsplayer.Constant.PROGRAM_STREAM_DIRECTORY;
import static com.example.cliff.tsplayer.Constant.PROGRAM_STREAM_MAP;

/**
 * Created by CLIFF on 2017/5/9.
 */

public class TsPacket{
    public static final String TAG = "Cliff";
    public static final int MAX_TS_PACKET_LENGTH = 208;

    public TsPacketInfo packet_info;
    public TsHeaderInfo header_info;
    public byte[] ts_data;

    public TsPacket(){
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

    public int ReadBits(TsPacket rawParameterObj, int n) // 從currentBit所在位置的bit讀出後n個bit之值
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

    public void readHeaderInfo(){
        header_info.sync_byte                    = ReadBits(this, 8);
        header_info.transport_error_indicator    = ReadBits(this, 1);
        header_info.payload_unit_start_indicator = ReadBits(this, 1);
        header_info.transport_priority           = ReadBits(this, 1);
        header_info.pid                          = ReadBits(this, 13);
        header_info.transport_scrambling_control = ReadBits(this, 2);
        header_info.adaptation_field_control     = ReadBits(this, 2);
        header_info.continuity_counter           = ReadBits(this, 4);
    }

    public void printHeaderInfo(){
        Log.i(TAG, String.format("############ Packet %05d Header ##############", packet_info.packet_number));
        Log.i(TAG, String.format("Sync byte                    = 0x%02X", header_info.sync_byte));
        Log.i(TAG, String.format("Transport error indicator    = %d", header_info.transport_error_indicator));
        Log.i(TAG, String.format("Payload unit start indicator = %d", header_info.payload_unit_start_indicator));
        Log.i(TAG, String.format("Transport priority           = %d", header_info.transport_priority));
        Log.i(TAG, String.format("PID                          = %d", header_info.pid));
        Log.i(TAG, String.format("Transport scrambling control = %d", header_info.transport_scrambling_control));
        Log.i(TAG, String.format("Adaptation field control     = %d", header_info.adaptation_field_control));
        Log.i(TAG, String.format("Continuity counter           = %d", header_info.continuity_counter));

        // Check TS header info
        if(header_info.sync_byte != 0x47){
            Log.e(TAG, "Not TS packet!");
            // TODO: exception handling
        }

        if(header_info.transport_error_indicator == 1){
            Log.e(TAG, "TS packet damaged!");
            // TODO: exception handling
        }

        if(header_info.payload_unit_start_indicator == 1){
            Log.i(TAG, "Carry the first packet of the PES payload or pointer_field for PSI data");
        }
        else{
            Log.i(TAG, "Null packet or PES fragment or PSI section without pointer_field");
        }

        if(header_info.adaptation_field_control == 0){
            Log.i(TAG, "Reserved");
        }
        else if(header_info.adaptation_field_control == 1){
            Log.i(TAG, "Payload only");
        }
        else if(header_info.adaptation_field_control == 2){
            Log.i(TAG, "Adaptation field only");
        }
        else if(header_info.adaptation_field_control == 3){
            Log.i(TAG, "Adaptation field followed by payload");
        }
        else{
            Log.i(TAG, "adaptation_field_control value error!");
        }
        Log.i(TAG, "###############################################");


    }

    public void tsPacketSkipReadByte(int skip_byte){
        int skip_bits = skip_byte*8;
        if(skip_byte > 0){
            Log.e(TAG,"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            Log.i(TAG, String.format("Skip %d byte(s)", skip_byte));
            ReadBits(this, skip_bits);
            Log.i(TAG, String.format("Seek current bit to %d", packet_info.current_bit));
            Log.e(TAG,"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    public void readPsiPointer(PsiPointer psi_pointer_data){
        psi_pointer_data.pointer_field = ReadBits(this, 8);
    }

    public void readPat(ProgramAssociationTable pat){
        pat.table_id                 = ReadBits(this, 8);
        pat.section_syntax_indicator = ReadBits(this, 1);
        pat.zero                     = ReadBits(this, 1);
        pat.reserved_1               = ReadBits(this, 2);
        pat.section_length           = ReadBits(this, 12);
        pat.transport_stream_id      = ReadBits(this, 16);
        pat.reserved_2               = ReadBits(this, 2);
        pat.version_number           = ReadBits(this, 5);
        pat.current_next_indicator   = ReadBits(this, 1);
        pat.section_number           = ReadBits(this, 8);
        pat.last_section_number      = ReadBits(this, 8);
        pat.channel_number           = (pat.section_length-9)/4;

        // allocate memory to store program info
        pat.allocateProgramInfoArray();
        for(int i = 0; i < pat.channel_number; i++){
            pat.program_info_array[i].program_number  = ReadBits(this, 16);
            pat.program_info_array[i].reserved        = ReadBits(this, 3);
            pat.program_info_array[i].program_map_pid = ReadBits(this, 13);
        }
        pat.crc_32                    = ReadBits(this, 32);
    }

    public void readPmt(ProgramMapTable pmt){
        pmt.table_id                 = ReadBits(this, 8);
        pmt.section_syntax_indicator = ReadBits(this, 1);
        pmt.zero                     = ReadBits(this, 1);
        pmt.reserved_1               = ReadBits(this, 2);
        pmt.section_length           = ReadBits(this, 12);
        pmt.program_number           = ReadBits(this, 16);
        pmt.reserved_2               = ReadBits(this, 2);
        pmt.version_number           = ReadBits(this, 5);
        pmt.current_next_indicator   = ReadBits(this, 1);
        pmt.section_number           = ReadBits(this, 8);
        pmt.last_section_number      = ReadBits(this, 8);
        pmt.reserved_3               = ReadBits(this, 3);
        pmt.pcr_pid                  = ReadBits(this, 13);
        pmt.reserved_4               = ReadBits(this, 4);
        pmt.program_info_length      = ReadBits(this, 12);
        pmt.unread_size = pmt.section_length - 9; // 9 bytes: data size from program_number to program_info_length
    }

    public void readPmtStreamInfo(PmtStreamInfo stream_info, ProgramMapTable pmt){
        stream_info.stream_type    = ReadBits(this, 8);
        stream_info.reserved_1     = ReadBits(this, 3);
        stream_info.elementary_pid = ReadBits(this, 13);
        stream_info.reserved_2     = ReadBits(this, 4);
        stream_info.es_info_length = ReadBits(this, 12);
        pmt.unread_size = pmt.unread_size - 5;
    }

    public void readPmtCrc32(ProgramMapTable pmt){
        pmt.crc_32 = ReadBits(this, 32);
        pmt.unread_size = pmt.unread_size - 4;
        Log.i(TAG, String.format("PMT CRC32      = 0x%08X", pmt.crc_32));
    }

    int readAdaptationField(AdaptationField adaptation_field_data){
        int check_value = 0;
        adaptation_field_data.adaptation_field_length              = ReadBits(this, 8);
        adaptation_field_data.discontinuity_indicator              = ReadBits(this, 1);
        adaptation_field_data.random_access_indicator              = ReadBits(this, 1);
        adaptation_field_data.elementary_stream_priority_indicator = ReadBits(this, 1);
        adaptation_field_data.pcr_flag                             = ReadBits(this, 1);
        adaptation_field_data.opcr_flag                            = ReadBits(this, 1);
        adaptation_field_data.splicing_point_flag                  = ReadBits(this, 1);
        adaptation_field_data.transport_private_data_flag          = ReadBits(this, 1);
        adaptation_field_data.adaptation_field_extension_flag      = ReadBits(this, 1);

        check_value = adaptation_field_data.discontinuity_indicator |
                      adaptation_field_data.random_access_indicator |
                      adaptation_field_data.elementary_stream_priority_indicator |
                      adaptation_field_data.pcr_flag |
                      adaptation_field_data.opcr_flag |
                      adaptation_field_data.splicing_point_flag |
                      adaptation_field_data.transport_private_data_flag |
                      adaptation_field_data.adaptation_field_extension_flag;

        if(check_value != 0){
            Log.e(TAG, "Unsupported adaptation field parameter!");
            return -1;
        }

        return 0;
    }

    public int readPesHeader(PesHeader pes_header_buf){
        pes_header_buf.stream_id = ReadBits(this, 8);
        if(pes_header_buf.stream_id == PROGRAM_STREAM_MAP ||
           pes_header_buf.stream_id == PADDING_STREAM ||
           pes_header_buf.stream_id == PRIVATE_STREAM_2 ||
           pes_header_buf.stream_id == ECM ||
           pes_header_buf.stream_id == EMM ||
           pes_header_buf.stream_id == PROGRAM_STREAM_DIRECTORY ||
           pes_header_buf.stream_id == DSMCC_STREAM ||
           pes_header_buf.stream_id == ITU_T_REC_H2221_TYPE_E_STREAM)
        {
            Log.e(TAG, "Unsupported PES packet!");
            return -1;
        }

        pes_header_buf.pes_packet_length         = ReadBits(this, 16);
        pes_header_buf.binary_10                 = ReadBits(this, 2);
        pes_header_buf.pes_scrambling_control    = ReadBits(this, 2);
        pes_header_buf.pes_priority              = ReadBits(this, 1);
        pes_header_buf.data_alignment_indicator  = ReadBits(this, 1);
        pes_header_buf.copyright                 = ReadBits(this, 1);
        pes_header_buf.original_or_copy          = ReadBits(this, 1);
        pes_header_buf.pts_dts_flags             = ReadBits(this, 2);
        pes_header_buf.escr_flag                 = ReadBits(this, 1);
        pes_header_buf.es_rate_flag              = ReadBits(this, 1);
        pes_header_buf.dsm_trick_mode_flag       = ReadBits(this, 1);
        pes_header_buf.additional_copy_info_flag = ReadBits(this, 1);
        pes_header_buf.pes_crc_flag              = ReadBits(this, 1);
        pes_header_buf.pes_extension_flag        = ReadBits(this, 1);
        pes_header_buf.pes_header_data_length    = ReadBits(this, 8);

        if(pes_header_buf.data_alignment_indicator == 0){
            Log.e(TAG, "data_alignment_indicator = 0\n");
        }

        return 0;
    }

    void copyPesPayloadFromTs(PesPayload pes_packet, int source_start_byte, int copy_size){
        //unsigned char *source_start_ptr = (unsigned char*)(source_packet->ts_data) + source_start_byte;
        //unsigned char *target_start_ptr = (unsigned char*)(pes_packet->pes_data) + pes_packet->copied_byte;
        Log.i(TAG, String.format("Copy %d bytes data\nfrom TS packet position: %d\nto PES packet position: %d", copy_size, source_start_byte, pes_packet.copied_byte));
        //memcpy(target_start_ptr, source_start_ptr, copy_size);
        System.arraycopy(this.ts_data, source_start_byte, pes_packet.pes_data, pes_packet.copied_byte, copy_size);
        pes_packet.copied_byte = pes_packet.copied_byte + copy_size;
    }
}
