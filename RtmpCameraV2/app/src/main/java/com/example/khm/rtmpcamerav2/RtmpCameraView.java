package com.example.khm.rtmpcamerav2;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioRecord;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by khm on 2017-06-29.
 */

public class RtmpCameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    final static private String LOG_TAG = "RtmpCameraView";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean isPreviewOn = false;

    private int imageWidth;
    private int imageHeight;

    public RtmpCameraView(Context context, Camera camera, int imageWidth, int imageHeight){
        super(context);
        mCamera = camera;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera.setPreviewCallback(RtmpCameraView.this);

        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
            try{
                stopPreview();
                mCamera.setPreviewDisplay(holder);

            } catch (IOException e) {
                Log.d(LOG_TAG, "Error setting camera preview: " + e.getMessage());
                mCamera.release();
                mCamera = null;
            }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            stopPreview();

        Camera.Parameters camParams = mCamera.getParameters();
        /*List<Camera.Size> sizes = camParams.getSupportedPreviewSizes();
        // Sort the list in ascending order
        Collections.sort(sizes, new Comparator<Camera.Size>() {

            public int compare(final Camera.Size a, final Camera.Size b) {
                return a.width * a.height - b.width * b.height;
            }
        });
        for(Camera.Size size : sizes){
            Log.d("supportedPreviewSize",size.width+", "+ size.height);
        }*/

        //imageWidth = 320;//sizes.get(sizes.size() - 1).width;
        //imageHeight = 240;//sizes.get(sizes.size() - 1).height;

        //화면 재설정 호출을 위한 width, height 설정

        camParams.setPreviewSize(imageWidth, imageHeight);

        Log.v(LOG_TAG,"Setting imageWidth: " + imageWidth + " imageHeight: " + imageHeight + " frameRate: " + RecordingActivity.frameRate);

        camParams.setPreviewFrameRate(RecordingActivity.frameRate);
        Log.v(LOG_TAG,"Preview Framerate: " + camParams.getPreviewFrameRate());

        mCamera.setParameters(camParams);

        // Set the holder (which might have changed) again
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(RtmpCameraView.this);
            startPreview();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not set preview display in surfaceChanged");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            mHolder.addCallback(null);
            mCamera.setPreviewCallback(null);
        } catch (RuntimeException e) {
            // The camera has probably just been released, ignore.
        }
    }
    public void startPreview() {
        if (!isPreviewOn && mCamera != null) {
            isPreviewOn = true;
            mCamera.startPreview();
        }
    }
    public void stopPreview() {
        if (isPreviewOn && mCamera != null) {
            isPreviewOn = false;
            mCamera.stopPreview();
        }
    }
    public void setResolution(int imageWidth, int imageHeight){
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (AudioRecordRunnable.audioRecord == null || AudioRecordRunnable.audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            RecordingActivity.startTime = System.currentTimeMillis();
            return;
        }
            /* get video data */
        if (RecordingActivity.yuvImage != null && RecordingActivity.recording) {
            ((ByteBuffer)RecordingActivity.yuvImage.image[0].position(0)).put(data);

             try {
                Log.v(LOG_TAG,"Writing Frame");
                long t = 1000 * (System.currentTimeMillis() - RecordingActivity.startTime);
                if (t > RecordingActivity.recorder.getTimestamp()) {
                    RecordingActivity.recorder.setTimestamp(t);
                }

                RecordingActivity.recorder.record(RecordingActivity.yuvImage);

            } catch (FFmpegFrameRecorder.Exception e) {
                Log.v(LOG_TAG,e.getMessage());
                e.printStackTrace();
            }
        }
    }


}
