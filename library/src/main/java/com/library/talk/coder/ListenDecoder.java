package com.library.talk.coder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.library.common.VoiceCallback;
import com.library.common.VoicePlayer;
import com.library.util.OtherUtil;
import com.library.util.mLog;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/12/25.
 */

public class ListenDecoder implements VoiceCallback {
    private final String AAC_MIME = MediaFormat.MIMETYPE_AUDIO_AAC;

    private MediaCodec mDecoder;
    private boolean isdecoder = false;
    private VoicePlayer voicePlayer;

    public ListenDecoder() {
        try {
            //初始化解码器
            mDecoder = MediaCodec.createDecoderByType(AAC_MIME);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, AAC_MIME);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, OtherUtil.samplerate);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //用来标记AAC是否有adts头，1->有
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);

            byte[] data = new byte[]{(byte) 0x12, (byte) 0x10};
            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(data));

            //解码器配置
            mDecoder.configure(mediaFormat, null, null, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mDecoder.start();
    }

    public void register(VoicePlayer voicePlayer) {
        this.voicePlayer = voicePlayer;
    }

    @Override
    public void voiceCallback(byte[] voice) {
        if (isdecoder) {
            //音频解码耗时较少，直接单线程顺序执行解码
            decoder(voice);
        }
    }

    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private int outputBufferIndex;

    public void decoder(byte[] voice) {
        try {
            //返回一个包含有效数据的input buffer的index,-1->不存在
            int inputBufIndex = mDecoder.dequeueInputBuffer(OtherUtil.waitTime);
            if (inputBufIndex >= 0) {
                //获取当前的ByteBuffer
                ByteBuffer dstBuf = mDecoder.getInputBuffer(inputBufIndex);
                dstBuf.clear();
                dstBuf.put(voice, 0, voice.length);
                mDecoder.queueInputBuffer(inputBufIndex, 0, voice.length, 0, 0);
            } else {
                mLog.log("dcoder_failure", "dcoder failure_VC");
                return;
            }
            outputBufferIndex = mDecoder.dequeueOutputBuffer(info, OtherUtil.waitTime);

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mDecoder.getOutputBuffer(outputBufferIndex);
                byte[] outData = new byte[info.size];
                outputBuffer.get(outData);
                outputBuffer.clear();
                if (voicePlayer != null) {
                    //通过接口回调播放
                    voicePlayer.voicePlayer(outData);
                }
                mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mDecoder.dequeueOutputBuffer(info, OtherUtil.waitTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        isdecoder = true;
    }

    /*
     * 释放资源
     */
    public void stop() {
        isdecoder = false;
    }

    public void destroy() {
        isdecoder = false;
        voicePlayer = null;
        mDecoder.stop();
        mDecoder.release();
        mDecoder = null;
    }
}


