package com.xpoint.drp;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by dongrp on 2017/2/22.
 */

public class FakePointService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("FakePointService", "onStartCommand");
        startForeground(798, new Notification());
        stopForeground(true);
        stopSelf();
        return START_NOT_STICKY;
    }
}
