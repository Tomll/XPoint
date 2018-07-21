package com.xpoint.drp;

import android.app.Application;
import android.util.Log;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //Log.d("MyApplication", "Application 启动了");
        RootContext.hasRootAccess(this);
    }
}
