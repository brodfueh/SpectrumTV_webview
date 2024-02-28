package com.workaround.spectv;

import static com.workaround.spectv.MainActivity.restartingFromDreaming;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DreamReceiver extends BroadcastReceiver {
    public DreamReceiver() {
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SPECDEBUG","DreamReceiver() "  + intent.getAction());
        restartingFromDreaming = true;

    }
}
