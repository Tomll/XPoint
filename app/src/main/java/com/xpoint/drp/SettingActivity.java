package com.xpoint.drp;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.Instrumentation;
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
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用设置界面
 */
public class SettingActivity extends AppCompatActivity {
    Switch xSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();

    }

    public void initView() {
        xSwitch = (Switch) findViewById(R.id.switch0);
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
        checkDrawOverlyAndAccessibilityServicePermission();//检查悬浮窗 及AccessibilityService权限
    }

    /**
     * 检查悬浮窗 及AccessibilityService权限,并引导开启
     */
    public void checkDrawOverlyAndAccessibilityServicePermission() {
        //API >=23，需要在manifest中申请权限，并在每次需要用到权限的时候检查是否已有该权限，因为用户随时可以取消掉。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {
                new AlertDialog.Builder(SettingActivity.this)
                        .setCancelable(false)
                        .setCancelable(false)
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
            } else {
                checkAccessibilityServicePermission();
            }
        } else {
            checkAccessibilityServicePermission();
        }
    }

    public void checkAccessibilityServicePermission() {
        if (!isStartAccessibilityService(getApplicationContext(), getPackageName())) {
            Log.d("SettingActivity", "未开启 无障碍服务");
            new AlertDialog.Builder(SettingActivity.this)
                    .setTitle("权限请求").setMessage("请在稍后弹出的界面中，开启XPoint悬浮球服务")
                    .setPositiveButton("是", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                        }
                    })
                    .setNegativeButton("否", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
        } else {
            Log.d("SettingActivity", "yikaiqi  无障碍服务");

        }
    }

    /**
     * 判断AccessibilityService服务是否已经启动
     *
     * @param context
     * @param name:应用包名
     * @return
     */
    public static boolean isStartAccessibilityService(Context context, String name) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> serviceInfos = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : serviceInfos) {

            String id = info.getId();
            Log.d("SettingActivity", id);
            if (id.contains(name)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 判断某一个服务是否正在运行
     * ServiceName:包名全路径，例如：com.xpoint.drp.PointService
     */
    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningServices = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(50);
        for (int i = 0; i < runningServices.size(); i++) {
            if (runningServices.get(i).service.getClassName().toString().equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }

}
