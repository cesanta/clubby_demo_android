package com.cesanta.clubby.demo.android;

import java.io.IOException;

import com.cesanta.clubby.lib.Clubby;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static String _TAG = "clubby";
    private Clubby clubby = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.v(_TAG, "MainActivity onCreate");
        super.onCreate(savedInstanceState);
        Log.v(_TAG, "MainActivity after super onCreate");
        //setContentView(R.layout.main);

        try {
            clubby = new Clubby.Builder()
                .device("//api.cesanta.com/d/dfrank", "dfrank_psk")
                .build();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        clubby.connect();
    }

    @Override public void onDestroy() {
        Log.v(_TAG, "MainActivity onDestroy()");
        clubby.disconnect();

        super.onDestroy();
    }

}

