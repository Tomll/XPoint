package com.xpoint.drp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by dongrp on 2017/2/20.
 * 开机广播接收器
 */

public class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            //Log.d("BootUpReceiver", "already_boot_up");
            //Application在onReceive之前先启动
            //接收到开机广播后，开启PointService
            context.startService( new Intent(context,PointService.class));
        }
    }
}
