package com.example.cliff.tsplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by CLIFF on 2017/5/22.
 */

public class AacPlayer implements Runnable{
    public static final String TAG = "Cliff";

    int samplingFreq[] = {
            96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
            16000, 12000, 11025, 8000
    };
    int audioProfile, sampleIndex;

    private int sampleRate, channelsNumber, sampleNumberPerFrame = 1024;

    private long pts_us = 0, pts_unit;
    private String codecType = "audio/mp4a-latm";
    private MediaCodec audioDecoder;
    private AudioTrack audioTrack;

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;

    private int channelConfiguration, minSize;

    int mCount = 0;
    int inputBufferIndex,outputBufferIndex;
    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private byte[] aacData, pcmData;


    private void configMediaDecoder(){
        try {
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelsNumber);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_IS_ADTS, 1);

            byte [] csd0 = new byte[2];//{0x11, (byte)0x90};
            csd0[0] = (byte) ((audioProfile << 3) | (sampleIndex >> 1));
            csd0[1] = (byte) ((byte) ((sampleIndex << 7) & 0x80) | (channelsNumber << 3));
            Log.i("Cliff", String.format("csd0[0] = 0x%X", csd0[0]));
            Log.i("Cliff", String.format("csd0[1] = 0x%X", csd0[1]));
            ByteBuffer csd = ByteBuffer.wrap(csd0);
            csd.rewind();
            format.setByteBuffer("csd-0", csd);
            audioDecoder = MediaCodec.createDecoderByType(codecType);
            audioDecoder.configure(format, null, null, 0);
            audioDecoder.start();
            inputBuffers = audioDecoder.getInputBuffers();
            outputBuffers = audioDecoder.getOutputBuffers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configAudioTrack(){
        minSize = AudioTrack.getMinBufferSize(sampleRate, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfiguration,
                AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    public void setAacFrame(byte[] aacData){
        this.aacData = aacData;
    }

    public void play(){
        inputBufferIndex = audioDecoder.dequeueInputBuffer(1000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(aacData);
            audioDecoder.queueInputBuffer(inputBufferIndex, 0, aacData.length, pts_us, 0);
            outputBufferIndex = audioDecoder.dequeueOutputBuffer(info, 0);
            if (outputBufferIndex >= 0) {
                ByteBuffer buf = outputBuffers[outputBufferIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if(chunk.length > 0){
                    audioTrack.write(chunk,0,chunk.length);
                }
                audioDecoder.releaseOutputBuffer(outputBufferIndex, false);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = audioDecoder.getOutputBuffers();
                Log.d("Cliff", "output buffers have changed.");
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = audioDecoder.getOutputFormat();
                Log.d("Cliff", "output format has changed to " + oformat);
            } else {
                Log.d("Cliff", "dequeueOutputBuffer returned " + outputBufferIndex);
            }
            pts_us = pts_us + pts_unit;
        }
        else{
            //Log.e(TAG, "dequeueInputBuffer timeout");
        }
    }

    public AacPlayer(AdtsHeader adts_header_data){
        channelsNumber = adts_header_data.adts_fixed_header_data.channel_configuration;
        sampleIndex = adts_header_data.adts_fixed_header_data.sampling_frequency_index;
        if( sampleIndex < samplingFreq.length){
            sampleRate = samplingFreq[sampleIndex];
            pts_unit = sampleNumberPerFrame*1000/(sampleRate/1000)+1;
        }
        else{
            Log.e(TAG, "sampling_frequency_index = " + adts_header_data.adts_fixed_header_data.channel_configuration);
        }

        if(adts_header_data.adts_fixed_header_data.profile == 1){
            audioProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
        }
        else{
            Log.e("Cliff", "Not support AAC profile = " + adts_header_data.adts_fixed_header_data.profile);
        }

        if(channelsNumber == 1){
            channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
        }
        else{
            channelConfiguration = AudioFormat.CHANNEL_OUT_STEREO;
        }

        Log.i(TAG, "Sample Rate     = " + sampleRate);
        Log.i(TAG, "Channels Number = " + channelsNumber);
        Log.i(TAG, "PTS unit time   = " + pts_unit);

        configMediaDecoder();
        configAudioTrack();
    }

    public void copyAac(byte[] aac_data, int aac_size){
        this.aacData = new byte[aac_size];
        System.arraycopy(aac_data, 0, this.aacData, 0, aac_size);
    }

    @Override
    public void run() {
        play();
    }
}
