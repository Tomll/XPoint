package com.xpoint.drp;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;

/**
 * 应用设置界面
 */
public class SettingActivity extends AppCompatActivity {
    Switch xSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        xSwitch = findViewById(R.id.switch0);
        if (isServiceRunning(this, "com.xpoint.drp.PointService")) {
            xSwitch.setChecked(true);
        }
        xSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startService(new Intent(SettingActivity.this, PointService.class));
                } else {
                    stopService(new Intent(SettingActivity.this, PointService.class));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOverlyPermission();//检查悬浮穿权限
    }

    /**
     * 检查并请求用户给予悬浮窗的权限
     */
    public void checkOverlyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(SettingActivity.this)
                        .setTitle("权限请求").setMessage("请允许XPoint在其他应用的上层显示")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                                startActivityForResult(intent, 18);
                            }
                        })
                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).show();
            }
        }
    }


    /**
     * 判断服务是否正在运行
     * ServiceName:包名全路径，例如：com.xpoint.drp.PointService
     */
    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningServices = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningServices.size(); i++) {
            if (runningServices.get(i).service.getClassName().toString().equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }


}
