package com.example.khm.rtmpcamerav2;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by khm on 2017-06-29.
 */

public class RecordingActivity extends Activity {

    private final static String CLASS_LABEL = "RecordingActivity";
    private final static String LOG_TAG = CLASS_LABEL;
    //방송 RTMP URL, 채팅 방 이름
    private static String ffmpeg_link = "rtmp://rtmpmanager-freecat.afreeca.tv/app/gudals2001-548439256";
    private static String chatRoomName = "test";

    private int screenWidth, screenHeight;
    private Button btnRecorderControl;

    Socket sock;
    ChatListAdapter chatListAdapter;
    /*640, 480
    720, 480
    800, 480
    960, 720
    1056, 864
    1280, 720
    1440, 1080
    1920, 1080*/
    int imageWidth = 640;
    int imageHeight = 480;

    /* video data getting thread */
    private Camera cameraDevice;
    private RtmpCameraView cameraView;

    //static variables
    static boolean recording = false;
    static FFmpegFrameRecorder recorder;
    static Frame yuvImage = null;
    static int frameRate = 30;
    static int sampleAudioRateInHz = 44100;
    static volatile boolean runAudioThread = true;
    static long startTime = 0;

    AudioRecordRunnable audioRecordRunnable;
    Thread audioThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //화면 수평
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //화면 꺼짐 방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);

        initLayout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        recording = false;

        if (cameraView != null) {
            cameraView.stopPreview();
        }

        if(cameraDevice != null) {
            cameraDevice.stopPreview();
            cameraDevice.release();
            cameraDevice = null;
        }
    }
    //레이아웃 초기화 함수
    private void initLayout() {

        /* get size of screen *//*
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        screenWidth = display.getWidth();
        screenHeight = display.getHeight();*/

        RelativeLayout.LayoutParams layoutParam = null;
        LayoutInflater myInflate = null;
        myInflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        RelativeLayout topLayout = new RelativeLayout(this);

        setContentView(topLayout);

        //카메라 뷰 추가
        layoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        cameraDevice = Camera.open();
        Camera.Parameters cameraParams = cameraDevice.getParameters();
        cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        cameraDevice.setParameters(cameraParams);
        Log.i(LOG_TAG, "cameara open");
        cameraView = new RtmpCameraView(this, cameraDevice, imageWidth, imageHeight);
        topLayout.addView(cameraView, layoutParam);
        Log.i(LOG_TAG, "cameara preview start: OK");

        //채팅 리스트 뷰 추가
        final RelativeLayout chatViewLayout = (RelativeLayout)myInflate.inflate(R.layout.chat_list, null);
        topLayout.addView(chatViewLayout);
        ListView chatListView = (ListView)findViewById(R.id.chatList);
        chatListAdapter = new ChatListAdapter();
        chatListView.setAdapter(chatListAdapter);
        for(int i= 0 ; i < 10; i++){
            chatListAdapter.addItem("hello", "my name is ~");
        }

        //채팅 접속
        //initChatting();

        //인터베이스 뷰 추가
        final RelativeLayout preViewLayout = (RelativeLayout) myInflate.inflate(R.layout.record_button, null);
        layoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        final Handler handler = new Handler();
        final Runnable disappearAction = new Runnable() {
            @Override
            public void run() {
                preViewLayout.setAlpha(0);
            }
        };
        preViewLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        if(preViewLayout.getAlpha() == 1){
                            handler.removeCallbacks(disappearAction);
                            preViewLayout.setAlpha(0);
                        }else{
                            preViewLayout.setAlpha(1);
                            handler.postDelayed(disappearAction,3000);
                        }
                        break;
                }
                return false;
            }
        });

        topLayout.addView(preViewLayout, layoutParam);

        /* 스트리밍 ON/OFF 버튼 이벤트 등록 */
        btnRecorderControl = (Button) findViewById(R.id.recorder_button);
        btnRecorderControl.setText("Start");
        btnRecorderControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recording) {
                    startRecording();
                    Log.w(LOG_TAG, "Start Button Pushed");
                    btnRecorderControl.setText("Stop");
                } else {
                    // This will trigger the audio recording loop to stop and then set isRecorderStart = false;
                    stopRecording();
                    Log.w(LOG_TAG, "Stop Button Pushed");
                    btnRecorderControl.setText("Start");
                }
            }
        });

        //채팅 뷰 ON/OFF버튼 이벤트 등록
        Button btn = (Button)findViewById(R.id.chatConnect_button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chatViewLayout.getVisibility() == View.VISIBLE){
                    chatViewLayout.setVisibility(View.INVISIBLE);
                }else {
                    chatViewLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        //설정 이벤트 등록
        final LinearLayout innerLayout = (LinearLayout)findViewById(R.id.recordInner_layout);
        Button configOpenBtn = (Button)findViewById(R.id.configOpen_Btn);
        Button configConfirmBtn = (Button)findViewById(R.id.configConfirm_Btn);
        Button configCancelBtn = (Button)findViewById(R.id.configCancel_Btn);
        final EditText rtmpURLConfigEditText = (EditText)findViewById(R.id.rtmpURLConfig_EditText);
        final EditText chatConfigEditText = (EditText)findViewById(R.id.chatConfig_EditText);
        final RadioGroup resolutionRadioGroup = (RadioGroup)findViewById(R.id.resolution_radioGroup);
        ((RadioButton)resolutionRadioGroup.getChildAt(0)).setChecked(true);

        //기존의 설정 값 가져오기, 없다면 Default
        SharedPreferences configs = getSharedPreferences("configs",MODE_PRIVATE);
        String tempUrl = configs.getString("RTMPURL","rtmp://rtmpmanager-freecat.afreeca.tv/app/gudals2001-548439256");
        String tempChatRoomName = configs.getString("CHATROOMNAME","test");
        rtmpURLConfigEditText.setText(tempUrl);
        chatConfigEditText.setText(tempChatRoomName);

        configOpenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recording){
                    Toast.makeText(RecordingActivity.this, "방송 중에는 설정이 불가능 합니다.", Toast.LENGTH_SHORT).show();
                }else {
                    handler.removeCallbacks(disappearAction);
                    preViewLayout.setEnabled(false);
                    innerLayout.setEnabled(true);
                    innerLayout.setClickable(true);
                    innerLayout.setVisibility(View.VISIBLE);
                }
            }
        });
        configConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences configs = getSharedPreferences("configs",MODE_PRIVATE);
                SharedPreferences.Editor configEditor = configs.edit();
                //방송 주소, 채팅 룸 이름 설정 및 저장
                ffmpeg_link = rtmpURLConfigEditText.getText().toString();
                chatRoomName = chatConfigEditText.getText().toString();
                configEditor.putString("RTMPURL",ffmpeg_link);
                configEditor.putString("CHATROOMNAME",chatRoomName);
                configEditor.commit();
                //방송 화질 설정
                setResolution(resolutionRadioGroup);
                //설정 뷰 사라짐
                innerLayout.setVisibility(View.GONE);
                preViewLayout.setEnabled(true);
            }
        });
        configCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                innerLayout.setVisibility(View.GONE);
                preViewLayout.setEnabled(true);
            }
        });

    }
    //방송 화질 설정 함수
    private void setResolution(RadioGroup resolutionRadioGroup){
        RadioButton resolutionRadioButton = (RadioButton)findViewById(resolutionRadioGroup.getCheckedRadioButtonId());
        String resolution = resolutionRadioButton.getText().toString();
        if(resolution.equals("480p")){
            //surfaceView.Callback호출, 화면 재설정
            imageWidth = 640;
            imageHeight = 480;
        }else{  // "720p"
            imageWidth = 1280;
            imageHeight = 720;
        }
        cameraView.setResolution(imageWidth, imageHeight);
        cameraView.surfaceChanged(cameraView.getHolder(), -1, imageWidth, imageHeight);
    }
    //채팅 초기화
    private void initChatting() {
        try{
            sock = IO.socket("http://192.168.0.5:3000");
            sock.connect();

            JSONObject json = new JSONObject();
            json.put("room", chatRoomName);
            sock.emit("joinroom", json);
            sock.on("toclient",toClient);
        } catch (URISyntaxException | JSONException e) {
            Log.d("Socket","SOCKET EXCEPTION");
        }
    }

    private Emitter.Listener toClient = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try{
                        String nickName = data.getString("nickName");
                        String message = data.getString("msg");
                        chatListAdapter.addItem(nickName, message);
                        chatListAdapter.notifyDataSetChanged();

                    }catch (JSONException e) {
                        Log.getStackTraceString(e);
                    }
                }
            });
        }
    };

    private void initRecorder() {

        Log.w(LOG_TAG,"init recorder");

        System.setProperty("org.bytedeco.javacpp.maxphysicalbytes","0");
        System.setProperty("org.bytedeco.javacpp.maxbytes", "0");

        if (yuvImage == null) {
            yuvImage = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
            Log.i(LOG_TAG, "create yuvImage");
        }

        Log.i(LOG_TAG, "ffmpeg_url: " + ffmpeg_link);
        recorder = new FFmpegFrameRecorder(ffmpeg_link, imageWidth, imageHeight, 1);
        recorder.setFormat("flv");
        recorder.setSampleRate(sampleAudioRateInHz);
        recorder.setVideoBitrate(1500000);
        recorder.setVideoOption("preset", "ultrafast");
        // Set in the surface changed method
        recorder.setFrameRate(frameRate);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);

        Log.i(LOG_TAG, "recorder initialize success");

        audioRecordRunnable = new AudioRecordRunnable();
        audioThread = new Thread(audioRecordRunnable);
        runAudioThread = true;
    }

    public void startRecording() {

        initRecorder();

        try {
            recorder.start();
            startTime = System.currentTimeMillis();
            recording = true;
            audioThread.start();


        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {

        runAudioThread = false;
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            // reset interrupt to be nice
            Thread.currentThread().interrupt();
            return;
        }
        audioRecordRunnable = null;
        audioThread = null;

        if (recorder != null && recording) {
            recording = false;
            Log.v(LOG_TAG,"Finishing recording, calling stop and release on recorder");
            try {
                recorder.stop();
                recorder.release();

            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            recorder = null;

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //뒤로가기 버튼 클릭시 자원 정리
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (recording) {
                stopRecording();
                Log.d("App", "app finished");
            }
            if(sock != null){
                sock.disconnect();
                sock.off("toclient",toClient);
            }

            finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
