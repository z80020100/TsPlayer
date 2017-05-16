package com.example.cliff.tsplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by CLIFF on 2017/5/15.
 */

public class H264DecoderRunnable{
    public static final String TAG = "Cliff";

    private MediaCodec mMeidaCodec;
    private SurfaceView mSurfaceView;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;

    int mCount = 0;
    int inputBufferIndex,outputBufferIndex;
    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    byte[] tmpByte;
    int framType;
    boolean startKeyFrame = true;

    public H264DecoderRunnable(SurfaceView surfaceView){
        mSurfaceView = surfaceView;
        configMediaDecoder();
    }

    public void setNalu(byte[] nalu_data){
        tmpByte = nalu_data;
    }

    public void copyNalu(byte[] nalu_data, int nalu_size){
        tmpByte = new byte[nalu_size];
        System.arraycopy(nalu_data, 0, tmpByte, 0, nalu_size);
    }

    public void configMediaDecoder(){
        if(Build.VERSION.SDK_INT > 15) {
            try {
                mMeidaCodec = MediaCodec.createDecoderByType("video/avc");
                MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
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
        //Log.i("Decode", "framType = " + framType);

        if(framType == 5/*IDR*/) {
            startKeyFrame = true;
        }
        if(startKeyFrame || framType == 7/*SPS*/ || framType == 8/*PPS*/) {
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
                        Log.e(TAG, "dequeueOutputBuffer timeout");
                        break;
                    default:
                        mMeidaCodec.releaseOutputBuffer(outputBufferIndex, true);
                        break;
                }
                mCount++;
            }
            else{
                Log.e(TAG, "dequeueInputBuffer timeout");
            }
        }
    }
}
