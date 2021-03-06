package com.library.talk.coder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.library.common.VoicePlayer;
import com.library.talk.stream.ListenRecive;
import com.library.util.OtherUtil;

/**
 * Created by android1 on 2017/12/25.
 */

public class ListenTrack implements VoicePlayer {
    private AudioTrack audioTrack;
    private ListenDecoder listenDecoder;

    public ListenTrack(ListenRecive listenRecive) {
        int recBufSize = AudioTrack.getMinBufferSize(
                OtherUtil.samplerate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                OtherUtil.samplerate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufSize,
                AudioTrack.MODE_STREAM);

        listenDecoder = new ListenDecoder();
        listenRecive.setVoiceCallback(listenDecoder);
        listenDecoder.register(this);
    }

    public void start() {
        if (audioTrack != null) {
            audioTrack.play();
            listenDecoder.start();
        }
    }

    @Override
    public void voicePlayer(byte[] voicebyte) {
        if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.write(voicebyte, 0, voicebyte.length);
        }
    }


    public void stop() {
        if (audioTrack != null) {
            listenDecoder.stop();
            audioTrack.stop();
        }
    }

    public void destroy() {
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        listenDecoder.destroy();
    }
}

