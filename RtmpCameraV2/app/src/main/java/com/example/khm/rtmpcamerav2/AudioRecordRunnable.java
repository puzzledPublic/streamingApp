package com.example.khm.rtmpcamerav2;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.nio.ShortBuffer;

/**
 * Created by khm on 2017-06-29.
 */

public class AudioRecordRunnable implements Runnable {
    final static private String LOG_TAG = "AudioRecordRunnable";

    static AudioRecord audioRecord;


    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        // Audio
        int bufferSize;
        ShortBuffer audioData;
        int bufferReadResult;

        bufferSize = AudioRecord.getMinBufferSize(RecordingActivity.sampleAudioRateInHz,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RecordingActivity.sampleAudioRateInHz,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);


        audioData = ShortBuffer.allocate(bufferSize);


        Log.d(LOG_TAG, "audioRecord.startRecording()");
        audioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
        while (RecordingActivity.runAudioThread) {

            //Log.v(LOG_TAG,"recording? " + recording);
            bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
            audioData.limit(bufferReadResult);
            if (bufferReadResult > 0) {
                Log.v(LOG_TAG,"bufferReadResult: " + bufferReadResult);
                // If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
                // Why?  Good question...
                if (RecordingActivity.recording) {
                    try {
                        RecordingActivity.recorder.recordSamples(audioData);
                        //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                    } catch (FFmpegFrameRecorder.Exception e) {
                        Log.v(LOG_TAG,e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        Log.v(LOG_TAG,"AudioThread Finished, release audioRecord");

            /* encoding finish, release recorder */
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            Log.v(LOG_TAG,"audioRecord released");
        }
    }
}
