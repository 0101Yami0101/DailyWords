package com.example.dailywords;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WordReceiver extends BroadcastReceiver {
    private static final String TAG = "WordReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered: fetching new word...");
        // Auto fetch (with notification)
        WordFetcher.fetchWord(context, true, () -> {
            // App UI will update via broadcast
            Intent updateIntent = new Intent("com.example.dailywords.NEW_WORD");
            context.sendBroadcast(updateIntent);
        });
    }

}
