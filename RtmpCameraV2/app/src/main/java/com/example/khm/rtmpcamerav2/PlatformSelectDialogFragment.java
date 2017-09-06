package com.example.khm.rtmpcamerav2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

/**
 * Created by khm on 2017-09-06.
 */

public class PlatformSelectDialogFragment extends DialogFragment {
    EditText rtmpURLConfigEditText;
    String[] platformBaseURLs = {
            "rtmp://live-sel.twitch.tv/app/",
            "rtmp://rtmpmanager-freecat.afreeca.tv/app/",
            "rtmp://a.rtmp.youtube.com/live2/",
            ""
    };
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("플랫폼 선택")
                .setItems(R.array.streaming_platform, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences configs = getActivity().getSharedPreferences("configs",getActivity().MODE_PRIVATE);
                        rtmpURLConfigEditText.setText(platformBaseURLs[which]);
                    }
                });

        return builder.create();
    }
    public void setRtmpURLConfigEditText(EditText rtmpURLConfigEditText){
        this.rtmpURLConfigEditText = rtmpURLConfigEditText;
    }
}
