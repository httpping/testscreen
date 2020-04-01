package com.yorhp.luckmoney.service;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.Toast;

import com.yorhp.luckmoney.fc.WindowManager.WindowTipController;
import com.yorhp.luckmoney.util.AppUtils;
import com.yorhp.luckmoney.util.FileUtil;
import com.yorhp.luckmoney.util.PollingUtil;
import com.yorhp.luckmoney.util.ScreenUtil;

import org.w3c.dom.ls.LSException;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;

/**
 *  辅助
 *
 */
public class WxScanAccessibilityService extends BaseAccessbilityService {


    public static WxScanAccessibilityService mService;

    /**
     * 截屏消息
     */
    public static final int jieping_msg = 1;

    /**
     * 截屏朋友圈
     */
    public static final int jieping_pyq = 2;

    /**
     * 运行类型
     */
    public int scan_type ;

    /**
     * 是否结束
     */
    public boolean isend = false;

    public static final String TAG="WxScanAccessibilityService";


    /**
     * 当前界面是否在聊天消息里面
     */
    public static boolean isInChatList=false;


    /**
     * 微信包名
     */
    private static final String WX_PACKAGE_NAME = "com.tencent.mm";


    String MY_APP_PACKAGE_NAME ;

    /**
     * 获取屏幕宽高
     */
    private int screenWidth = ScreenUtil.SCREEN_WIDTH;
    private int screenHeight = ScreenUtil.SCREEN_HEIGHT;

    AtomicInteger count = new AtomicInteger(0);


    /**
     * 开始截屏
     */

    String  androidSystemUI =  "com.android.systemui";


    long  endWindowContextChangeTime ;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {



        String packageName = event.getPackageName().toString();

        //窗口改变了,开始后窗体改变，就结束
        if (event.getEventType() == TYPE_WINDOW_STATE_CHANGED && isStartJiepin){
            Log.d(TAG,"窗口改变了,有界面操作");
            if (!packageName.equalsIgnoreCase(MY_APP_PACKAGE_NAME)) {
                isStartJiepin = false;
                isend =true;
//                Toast.makeText(getApplicationContext(), "有窗口改变", Toast.LENGTH_SHORT).show();
                mHandler.sendEmptyMessage(1002);
            }
            if (packageName.equalsIgnoreCase(WX_PACKAGE_NAME)){
                Log.d(TAG,"wxx窗口改变了,有界面操作");

            }
        }

        if (event.getEventType()== AccessibilityEvent.TYPE_WINDOWS_CHANGED){
            Log.d(TAG,"窗口改变TYPE_WINDOWS_CHANGED");
        }

        if (isStartJiepin && !isend  && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && packageName.equalsIgnoreCase(WX_PACKAGE_NAME)){
            if (event.getClassName().equals(packageList)) {
                Log.d(TAG, "TYPE_WINDOW_CONTENT_CHANGED");
            }
            endWindowContextChangeTime = System.currentTimeMillis()/1000;
        }

        if (packageName.equalsIgnoreCase(androidSystemUI)){
            return;
        }
        if (packageName.equalsIgnoreCase("com.android.settings")){
            return;
        }

        if (!packageName.contains(WX_PACKAGE_NAME) && !packageName.isEmpty()) {
            //不是微信就退出
            isInChatList=false;
            return;
        }
        isInChatList = true;

        //消息扫描
        if (jieping_msg == scan_type) {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                //当前类名,查找到list，执行定时任务job
                    String className = event.getClassName().toString();
                if (packageList.equalsIgnoreCase(className)) {
                    nodeInfoList = event.getSource();
                }
                if (packageViewPager.equals(className)){
                    nodeInfoWxViewPager = event.getSource();
                }
            }
        }


        if (event.getRecordCount() !=0){
            Log.d(TAG,"event :"+event.getToIndex());
        }

        Log.d(TAG,"监听变化结束");


    }



    public boolean isStartJiepin = false;
    public int  scrollWhat = 10010;
    public int  scrollWhatWXViewPager  = 10011;

    //滑动方向
    public int action ;
    public ImageReader mImageReader;

    /**
     * 截屏
     */
    public void jieping(int scroll){
        action = scroll;
        //最大间隔5S
        int maxJgTime =5;
        endWindowContextChangeTime = System.currentTimeMillis()/1000;
        count = new AtomicInteger(0);
        while (true && nodeInfoList!=null && !isend && mService!=null && isStartJiepin) {
            long time = System.currentTimeMillis()/1000 - endWindowContextChangeTime;
            if (time > maxJgTime){
                isStartJiepin = false;
                isend = true;
                mHandler.sendEmptyMessage(1002);
                return;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //最大截屏数量
            if (count.intValue()>1000){
                return;
            }
            Log.d(TAG,"循环:" +count.incrementAndGet());

            if (mImageReader!=null){
                jiepin();
                mHandler.sendEmptyMessage(1000);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mHandler.sendEmptyMessage(1001);

            nodeInfoList.performAction(scroll);
        }
    }


    /**
     * 截屏
     */
    public void jiepingWxViewPager(int scroll){
        action = scroll;
        //最大间隔5S
        int maxJgTime =7;
        endWindowContextChangeTime = System.currentTimeMillis()/1000;
        count = new AtomicInteger(0);
        while (true && nodeInfoWxViewPager!=null && !isend && mService!=null && isStartJiepin) {
            long time = System.currentTimeMillis()/1000 - endWindowContextChangeTime;
            if (time > maxJgTime){
                isStartJiepin = false;
                isend = true;
                mHandler.sendEmptyMessage(1002);
                return;
            }
            try {
                //多个时间加载页面
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //最大截屏数量
            if (count.intValue()>1000){
                return;
            }
            Log.d(TAG,"循环:" +count.incrementAndGet());

            if (mImageReader!=null){
                jiepin();
                mHandler.sendEmptyMessage(1000);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mHandler.sendEmptyMessage(1001);

            nodeInfoWxViewPager.performAction(scroll);
        }
    }

    /**
     * 异步滑动
     */
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
             if (msg.what == scrollWhat){
                 if (nodeInfoList!=null) {
                     nodeInfoList.performAction(action);
                 }
             }
             if (msg.what == scrollWhatWXViewPager){
                 if (nodeInfoWxViewPager !=null){
                     nodeInfoWxViewPager.performAction(action);
                 }
             }
             if (msg.what==1000){
                 WindowTipController.getInstance().showThumbWindow(null,"截屏第"+count.intValue()+"页成功");
             }
            if (msg.what==1001){
                WindowTipController.getInstance().destroyThumbWindow();
            }
            if (msg.what == 1002){
                Toast.makeText(getApplicationContext(),"截屏结束了",Toast.LENGTH_SHORT).show();
            }
        }
    };



    @Override
    public void onInterrupt() {
        mService = null;
        isend =true;
        isStartJiepin=false;
    }



    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        MY_APP_PACKAGE_NAME = AppUtils.getPackageName(this);

//        Toast.makeText(getApplication(),"辅助服务开启成功",Toast.LENGTH_SHORT).show();
        mService = this;

        ScreenUtil.getScreenSize(this);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        // 创建唤醒锁
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "WxService:wakeLock");
        // 获得唤醒锁
        wakeLock.acquire();
        performGlobalAction(GLOBAL_ACTION_BACK);
        performGlobalAction(GLOBAL_ACTION_BACK);
        performGlobalAction(GLOBAL_ACTION_BACK);
    }


    /**
     * 截屏
     */
    public void jiepin(){
//        strDate = dateFormat.format(new java.util.Date());
//        nameImage = pathImage+strDate+".png";
        Image image = mImageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width+rowPadding/pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,width, height);
        image.close();
        FileUtil.saveBitmapToSDCard(bitmap,System.currentTimeMillis()+"");
        Log.i(TAG,"捷帕尼开始"+count.intValue());
//        ImageView imageView =findViewById(R.id.img_show);
//        imageView.setImageBitmap(bitmap);

    }


}
