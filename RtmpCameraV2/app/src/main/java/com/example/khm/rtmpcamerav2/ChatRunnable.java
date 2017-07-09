package com.example.khm.rtmpcamerav2;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

/**
 * Created by khm on 2017-06-30.
 */

public class ChatRunnable implements Runnable {
    private Socket sock;

    @Override
    public void run() {
        try{
            sock = IO.socket("http://192.168.0.5:3000");

        }catch(URISyntaxException e){
            Log.d("ChatRunnable","CHAT URI ERROR");
        }
    }

}
