package com.xpoint.drp;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.KeyEvent.KEYCODE_NOTIFICATION;

/**
 * Created by dongrp on 2017/2/20.
 */

public class PointService extends AccessibilityService {
    private View pointView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private Context mContext;
    //手势监听需要注意两点
    //1：View必须设置longClickable为true，否则手势识别无法正确工作，只会返回Down, Show, Long三种手势
    //2：必须在View的onTouchListener中的onTouch()方法中调用手势识别，而不能像Activity一样重载onTouchEvent，否则同样手势识别无法正确工作
    private GestureDetector gestureDetector;


    Timer timerAlpha;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0 && null != pointView) {
                pointView.setAlpha(0.5f);
            } else if (msg.what == 1 && null != pointView) {
                pointView.setAlpha(0.3f);
            }
        }
    };

    //下面两个方法是父类AccessibilityService中的方法
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    /*
    AccessibilityService 中此方法是 final 所以我们就不能重写此方法了
    @Override
    public IBinder onBind(Intent intent) {
        //如果不允许以bind 的方式启动servcie 那就返回一个null ；如果允许，那就返回一个IBinder对象给调用者，调用者通过IBinder来调用service中的公开方法
        return null;
    }*/

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("PointService", "onCreate");
        mContext = getApplicationContext();
        //以下逻辑可以实现：将PointService设置为前台服务，同时可以去除通知栏的通知 （真假服务的ID必须相同）
        //1.将真的服务设置为前台服务
        startForeground(798, new Notification());
        //2.启动假的前台服务,在服务内部执行startForground、stopForground就可以去掉通知了，然后stopSlef()就可以了
        //Intent fakeIntent = new Intent(this, FakePointService.class);
        //startService(fakeIntent);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("PointService", "onStartCommand");
        //1、初始化params参数
        initWindowParams();
        //2、创建“小圆点”ImageView对象
        pointView = LayoutInflater.from(mContext).inflate(R.layout.layout_window, null, false);
        pointView.setAlpha(0.6f);
        //3、添加“小圆点”到window中
        windowManager.addView(pointView, params);
        //创建手势监听对象，在imageView的onTouch()方法中:return gestureDetector.onTouchEvent(event)
        gestureDetector = new GestureDetector(this, new MyGestureListener());
        //设置手势监听
        pointView.setOnTouchListener(new MyOnTouchListener());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("PointService", "onDestroy");
        windowManager.removeView(pointView);
        stopForeground(true); //删除前台服务的通知
    }

    /**
     * 初始化WindowParams
     */
    private void initWindowParams() {
        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.format = PixelFormat.TRANSLUCENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 750;
        params.y = 1450;
        params.width = 100;
        params.height = 100;
    }

    /**
     * 通过windowManager更新view的位置
     */
    public void updateViewLayout(int x, int y) {
        params.x = x;
        //因为event.getRawX()获取的是距离屏幕顶端的距离（包括状态栏的高度），而updateViewLayout（）并不包括状态栏在内，所以要减去一个状态栏的高度
        params.y = y - getStatusBarHeight();
        windowManager.updateViewLayout(pointView, params);
    }

    /**
     * 获取状态栏高度
     */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    //onTouch监听所需要的成员变量
    long downTime;
    long upTime;
    long moveTime;
    int startImageX;
    int startImageY;
    int startTouchX;
    int startTouchY;
    boolean longPress = false;
    int[] location = new int[2];

    /**
     * 自定义的OnTouch监听类(设置给View的)
     * onTouch是 View.OnTouchListener中定义的接口
     * onTouchEvent是重写的Activity中的方法
     * Activity中的一个view被点击：先执行 onTouch 再执行 onTouchEvent
     */
    class MyOnTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (null != timerAlpha) {
                        timerAlpha.cancel();
                        timerAlpha = null;
                    }
                    handler.sendEmptyMessage(0);
                    downTime = System.currentTimeMillis();
                    pointView.getLocationOnScreen(location);
                    startImageX = location[0];
                    startImageY = location[1];
                    startTouchX = (int) event.getRawX();
                    startTouchY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    moveTime = System.currentTimeMillis();
                    if (moveTime - downTime >= 500 && Math.abs((int) (event.getRawX() - startTouchX)) <= 50
                            && Math.abs((int) (event.getRawY() - startTouchY)) <= 50) { //长按的触发条件：按下超过500ms，且位移半径在50以内
                        longPress = true;
                    }
                    if (longPress) { //长按拖动情况：可任意拖动
                        updateViewLayout((int) (event.getRawX() - pointView.getWidth() / 2),
                                (int) (event.getRawY() - pointView.getWidth() / 2));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    upTime = System.currentTimeMillis();
                    timerAlpha = new Timer();
                    timerAlpha.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            handler.sendEmptyMessage(1);
                        }
                    }, 5 * 1000);//5s后透明度自动降低
                    if (longPress) { //如果是长按：则之前在ACTION_MOVE中执行的 updateViewLayout()生效，并将longPress归为初始状态false
                        longPress = false;
                    } else {//如果不是长按：则之前的 updateViewLayout()无效，并将imageView归回原来的位置
                        if (event.getRawY() - startImageY >= 100 /*&& Math.abs(event.getRawX() - startImageX) < 80*/) {
                            //Toast.makeText(mContext, "下拉", Toast.LENGTH_SHORT).show();
                            //exeShellCmd("input swipe 10  0  10  500 ");//shell屏幕滑动命令
                            performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                        } else if (event.getRawY() - startImageY <= -100 /*&& Math.abs(event.getRawX() - startImageX) < 80*/) {
                            //Toast.makeText(mContext, "上拉", Toast.LENGTH_SHORT).show();
                            openRecent();
                        } else if (event.getRawX() - startImageX >= 100 /*&& Math.abs(event.getRawY() - startImageY) < 80*/) {
                            Toast.makeText(mContext, "右拉", Toast.LENGTH_SHORT).show();
                        } else if (event.getRawX() - startImageX <= -100 /*&& Math.abs(event.getRawY() - startImageY) < 80*/) {
                            Toast.makeText(mContext, "左拉", Toast.LENGTH_SHORT).show();
                        }
                        updateViewLayout(startImageX, startImageY);
                    }
                    break;
            }
            //自己处理完长按拖动，剩下的单击、双击、短按滑动手势的判断交由 gestureDetector.onTouchEvent(event)处理
            return gestureDetector.onTouchEvent(event);
        }
    }

    /**
     * 自定义手势监听类
     * 安卓中的GestureDetector提供了两个侦听器接口，OnGestureListener处理单击类消息，OnDoubleTapListener处理双击类消息。
     * 有时候我们并不需要处理上面所有手势，方便起见，Android提供了另外一个类SimpleOnGestureListener实现了上述两个接口中的方法，
     * 我们只需要继承SimpleOnGestureListener然后重写感兴趣的手势即可
     */
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        //按下，触摸屏按下时立刻触发
        @Override
        public boolean onDown(MotionEvent e) {
            //Toast.makeText(mContext, "按下 " + e.getAction(), Toast.LENGTH_SHORT).show();
            return false;
        }

        // 短按，触摸屏按下后片刻后抬起，会触发这个手势，如果迅速抬起则不会
        @Override
        public void onShowPress(MotionEvent e) {
            //Toast.makeText(mContext, "短按" + e.getAction(), Toast.LENGTH_SHORT).show();
        }

        // 抬起，手指离开触摸屏时触发(长按、滚动、滑动时，不会触发这个手势)
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            //Toast.makeText(mContext, "抬起" + e.getAction(), Toast.LENGTH_SHORT).show();
            return false;
        }

        // 拖动，触摸屏按下后移动
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //Toast.makeText(mContext, "拖动" + e2.getAction(), Toast.LENGTH_SHORT).show();
            Log.d("MyGestureListener", "distanceX:" + distanceX);
            updateViewLayout((int) (e2.getRawX() - pointView.getWidth() / 2),
                    (int) (e2.getRawY() - pointView.getHeight() / 2));
            return false;
        }

        // 长按，触摸屏按下后既不抬起也不移动，过一段时间后触发
        @Override
        public void onLongPress(MotionEvent e) {
            //Toast.makeText(mContext, "长按" + e.getAction(), Toast.LENGTH_SHORT).show();
            //longPress = true;
        }

        // 滑动，触摸屏按下后快速移动并抬起，会先触发滚动手势，跟着触发一个滑动手势
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Toast.makeText(mContext, "滑动" + e2.getAction(), Toast.LENGTH_SHORT).show();
            return false;
        }

        // 双击，手指在触摸屏上迅速点击第二下时触发
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //Toast.makeText(mContext, "双击" + e.getAction(), Toast.LENGTH_SHORT).show();
            goToLauncher();//去Launcher主界面
            return false;
        }

        // 双击的按下跟抬起各触发一次
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            //Toast.makeText(mContext, "DOUBLE EVENT " + e.getAction(), Toast.LENGTH_SHORT).show();
            return false;
        }

        // 单击确认，即很快的按下并抬起，但并不连续点击第二下
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            //Toast.makeText(mContext, "单击" + e.getAction(), Toast.LENGTH_SHORT).show();
            //exeShellCmd(" input  keyevent 4 ");//shell命令执行返回键，就是 速度太慢
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            return false;
        }
    }


    /**
     * 反射打开最近任务列表
     */
    public void openRecent() {
        Class serviceManagerClass;
        try {
            serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClass.getMethod("getService",
                    String.class);
            IBinder retbinder = (IBinder) getService.invoke(
                    serviceManagerClass, "statusbar");
            Class statusBarClass = Class.forName(retbinder
                    .getInterfaceDescriptor());
            Object statusBarObject = statusBarClass.getClasses()[0].getMethod(
                    "asInterface", IBinder.class).invoke(null,
                    new Object[]{retbinder});
            Method clearAll = statusBarClass.getMethod("toggleRecentApps");
            clearAll.setAccessible(true);
            clearAll.invoke(statusBarObject);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 去Launcher主界面
     */
    public void goToLauncher() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private OutputStream os;

    /**
     * 执行shell命令（前提是应用获取了root权限）
     */
    public final void exeShellCmd(String cmd) {
        try {
            if (os == null) {
                //Log.e("PointService", "os  空");
                os = Runtime.getRuntime().exec("su").getOutputStream();
            }
            cmd += "\n";
            //Log.e("PointService", "cmd: " + cmd);
            os.write(cmd.getBytes());
            os.flush();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }


}
