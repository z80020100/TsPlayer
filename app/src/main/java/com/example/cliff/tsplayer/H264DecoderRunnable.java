package com.example.cliff.tsplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by CLIFF on 2017/5/15.
 */

public class H264DecoderRunnable implements Runnable{
    public static final String TAG = "Cliff";

    private MediaCodec mMeidaCodec;
    private SurfaceView mSurfaceView;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;

    int mCount = 0;
    int inputBufferIndex,outputBufferIndex;
    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    byte[] tmpByte, tmpByte2;
    int framType;
    //boolean startKeyFrame = true;

    //byte[] sps = {0x00, 0x00, 0x00, 0x01, 0x67, 0x42, (byte)0x80, 0x1F, (byte)0xDA, 0x01, 0x40, 0x16, (byte)0xE8, 0x06, (byte)0xD0, (byte)0xA1, 0x35}; // SPS for HTC M8 720P
    //byte[] pps = {0x00, 0x00, 0x00, 0x01, 0x68, (byte) 0xCE, 0x06, (byte) 0xE2}; // PPS for HTC M8 720P

    //byte[] sps = {0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0x00, 0x1F, (byte)0x8D, (byte)0x8D, 0x40, 0x50, 0x1E, (byte)0xC8}; // SPS for Mi
    //byte[] pps = {0x00, 0x00, 0x00, 0x01, 0x68, (byte) 0xCA, 0x43, (byte) 0xC8}; // PPS for Mi

    byte[] sps, pps;
    int sps_start_pos = -1, pps_start_pos = -1, key_frame_start_pos = -1, tempFrameType;
    int sps_end_pos = -1, pps_end_pos = -1, key_frame_end_pos = -1;
    boolean detect_sps = false, detect_pps = false, detect_key_frame;

    LinkedBlockingDeque<byte[]> fifo = new LinkedBlockingDeque<byte[]>();

    public H264DecoderRunnable(SurfaceView surfaceView){
        mSurfaceView = surfaceView;
        configMediaDecoder();
    }

    public void setNalu(byte[] nalu_data){
        tmpByte = nalu_data;
    }

    public void copyNalu(byte[] nalu_data, int nalu_size){
        tmpByte2 = new byte[nalu_size];
        System.arraycopy(nalu_data, 0, tmpByte2, 0, nalu_size);
        fifo.add(tmpByte2);
    }

    public void closeMediaDecoder(){
        mMeidaCodec.stop();
        mMeidaCodec.release();
    }

    public void configMediaDecoder(){
        if(Build.VERSION.SDK_INT > 15) {
            try {
                mMeidaCodec = MediaCodec.createDecoderByType("video/avc");
                MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 1280, 720);
                //mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
                //mediaFormat.setByteBuffer("cud-1", ByteBuffer.wrap(pps));
                mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
                mMeidaCodec.configure(mediaFormat, mSurfaceView.getHolder().getSurface(), null, 0);
                mMeidaCodec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                mMeidaCodec.start();
                inputBuffers = mMeidaCodec.getInputBuffers();
                outputBuffers = mMeidaCodec.getOutputBuffers();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void decede() {
        framType = tmpByte[4]&0x1F;
        if(framType != 0x01){
            //Log.i("Decode", "framType = " + framType);
            //Log.i("Decode", "tmpByte.length = " + tmpByte.length);
            //framType = tmpByte[sps.length+pps.length+4]&0x1F;
            //Log.i("Decode", "Check I frame type =  = " + framType);

            // Detect NALU, SPS, PPS
            for(int i = 0; i < tmpByte.length-3; i++){
                if(tmpByte[i] == 0x00 && tmpByte[i+1] == 0x00 && tmpByte[i+2] == 0x00 && tmpByte[i+3] == 0x01){
                    //Log.i("NALU", "NAL start position = " + i);

                    if(detect_sps == true){
                        detect_sps = false;
                        sps_end_pos = i;
                        sps = Arrays.copyOfRange(tmpByte, sps_start_pos, sps_end_pos);
                        //Log.i("Decode", "sps_end_pos = " + sps_end_pos);
                    }

                    if(detect_pps == true){
                        detect_pps = false;
                        pps_end_pos = i;
                        pps = Arrays.copyOfRange(tmpByte, pps_start_pos, pps_end_pos);
                        //Log.i("Decode", "pps_end_pos = " + pps_end_pos);
                    }

                    if(detect_key_frame == true){
                        detect_key_frame = false;
                        key_frame_end_pos = i;
                        //Log.i("Decode", "key_frame_end_pos = " + key_frame_end_pos);
                    }

                    tempFrameType = tmpByte[i+4] & 0x1F;
                    if(sps_start_pos == -1){
                        if(tempFrameType == 0x07){
                            detect_sps = true;
                            sps_start_pos = i;
                            //Log.i("Decode", "sps_start_pos = " + sps_start_pos);
                        }
                    }

                    if(pps_start_pos == -1){
                        if(tempFrameType == 0x08){
                            detect_pps = true;
                            pps_start_pos = i;
                            //Log.i("Decode", "pps_start_pos = " + pps_start_pos);
                        }
                    }

                    if(tempFrameType == 5){
                        detect_key_frame = true;
                        key_frame_start_pos = i;
                        //Log.i("Decode", "key_frame_start_pos = " + key_frame_start_pos);
                    }
                }

                if(i == tmpByte.length-4){
                    //Log.i("Decode", "Impossible for finding next NALU");
                    if(detect_sps == true){
                        detect_sps = false;
                        sps_end_pos = i+4;
                        sps = Arrays.copyOfRange(tmpByte, sps_start_pos, sps_end_pos);
                        Log.i("Decode", "sps_end_pos = " + sps_end_pos);
                    }

                    if(detect_pps == true){
                        detect_pps = false;
                        pps_end_pos = i+4;
                        pps = Arrays.copyOfRange(tmpByte, pps_start_pos, pps_end_pos);
                        Log.i("Decode", "pps_end_pos = " + pps_end_pos);
                    }

                    if(detect_key_frame == true){
                        detect_key_frame = false;
                        key_frame_end_pos = i+4;
                        Log.i("Decode", "key_frame_end_pos = " + key_frame_end_pos);
                    }
                }
            }

            if(framType == 5/*IDR*/ || framType == 7/*SPS*/ /*|| framType == 8/*PPS*/ || framType == 9 /*AUD*/) {
                if(mCount == 0){
                    // Feed SPS and PPS to decoder
                    Log.i(TAG, "Feed SPS");
                    inputBufferIndex = mMeidaCodec.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        inputBuffer.clear();
                        inputBuffer.put(sps);
                        mMeidaCodec.queueInputBuffer(inputBufferIndex, 0, sps.length, mCount, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                        outputBufferIndex = mMeidaCodec.dequeueOutputBuffer(info, 0);
                        //outputBufferIndex = mMeidaCodec.dequeueOutputBuffer(info, 1000000l);
                        //Log.i("Decode", "outputBufferIndex = " + Integer.toString(outputBufferIndex));
                        switch (outputBufferIndex) {
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                break;
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                break;
                            default:
                                mMeidaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                break;
                        }
                        mCount++;
                    }
                    Log.i(TAG, "Feed PPS");
                    inputBufferIndex = mMeidaCodec.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        inputBuffer.clear();
                        inputBuffer.put(pps);
                        mMeidaCodec.queueInputBuffer(inputBufferIndex, 0, pps.length, mCount, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                        outputBufferIndex = mMeidaCodec.dequeueOutputBuffer(info, 0);
                        //outputBufferIndex = mMeidaCodec.dequeueOutputBuffer(info, 1000000l);
                        //Log.i("Decode", "outputBufferIndex = " + Integer.toString(outputBufferIndex));
                        switch (outputBufferIndex) {
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                break;
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                break;
                            default:
                                mMeidaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                break;
                        }
                        mCount++;
                    }
                }

                inputBufferIndex = mMeidaCodec.dequeueInputBuffer(33*1000);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(tmpByte);
                    mMeidaCodec.queueInputBuffer(inputBufferIndex, 0, tmpByte.length, mCount, 0);
                    outputBufferIndex = mMeidaCodec.dequeueOutputBuffer(info, 0);
                    //outputBufferIndex = mMeidaCodec.dequeueOutputBuffer(info, 1000000l);
                    //Log.i(TAG, "outputBufferIndex = " + Integer.toString(outputBufferIndex));
                    switch (outputBufferIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            //Log.e(TAG, "dequeueOutputBuffer timeout");
                            break;
                        default:
                            mMeidaCodec.releaseOutputBuffer(outputBufferIndex, true);
                            break;
                    }
                    mCount++;
                }
                else{
                    //Log.e(TAG, "dequeueInputBuffer timeout");
                }
            }
        }
        else{ // framType == 1 /*SLICE*/
            inputBufferIndex = mMeidaCodec.dequeueInputBuffer(33*1000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(tmpByte);
                mMeidaCodec.queueInputBuffer(inputBufferIndex, 0, tmpByte.length, mCount, 0);
                outputBufferIndex = mMeidaCodec.dequeueOutputBuffer(info, 0);
                //outputBufferIndex = mMeidaCodec.dequeueOutputBuffer(info, 1000000l);
                //Log.i(TAG, "outputBufferIndex = " + Integer.toString(outputBufferIndex));
                switch (outputBufferIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        //Log.e(TAG, "dequeueOutputBuffer timeout");
                        break;
                    default:
                        mMeidaCodec.releaseOutputBuffer(outputBufferIndex, true);
                        break;
                }
                mCount++;
            }
            else{
                //Log.e(TAG, "dequeueInputBuffer timeout");
            }
        }
    }

    public String byte2Hex(byte[] b) {
        String result = "";
        for (int i = 0 ; i < b.length ; i++) {
            result += "0x";
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            result += " ";
        }
        return result;
    }

    @Override
    public void run() {
        while(true) {
            while(fifo.size() <= 0){

            }
            tmpByte = fifo.getFirst();
            decede();
            fifo.removeFirst();
        }
    }
}
