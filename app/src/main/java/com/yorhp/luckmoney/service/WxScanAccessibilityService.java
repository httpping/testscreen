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

import com.yorhp.luckmoney.util.FileUtil;
import com.yorhp.luckmoney.util.PollingUtil;
import com.yorhp.luckmoney.util.ScreenUtil;

import org.w3c.dom.ls.LSException;

import java.nio.ByteBuffer;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;

/**
 * 抢红包辅助
 *
 * @author Tyhj
 * @date 2019/6/30
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

    public static final String TAG="LuckMoneyService";


    /**
     * 当前界面是否在聊天消息里面
     */
    public static boolean isInChatList=false;


    /**
     * 微信包名
     */
    private static final String WX_PACKAGE_NAME = "com.tencent.mm";


    /**
     * 等待弹窗弹出时间
     */
    public static int waitWindowTime=150;


    /**
     * 等待红包领取时间
     */
    public static int waitGetMoneyTime=700;


    /**
     * 当前机型是否需要配置时间，是否能获取到弹窗
     */
    public static int needSetTime=-1;

    /**
     * 获取屏幕宽高
     */
    private int screenWidth = ScreenUtil.SCREEN_WIDTH;
    private int screenHeight = ScreenUtil.SCREEN_HEIGHT;

    /**
     * 计算领取红包的时间
     */
    private static long luckMoneyComingTime;

    /**
     * 是否在领取详情页
     */
    private static boolean inMoneyDetail=false;
    private Button button;

    /**
     *  listitem数量
     */
    public int listItemCount =0;
    /**
     * 统计内容没变化次数
     */
    public int itemNoChangeCount = 0 ;

    /**
     * 内容没变化最大尝试次数
     */
    public int maxItemNoChange  = 10 ;

    public int  count  = 0 ;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {


        String packageName = event.getPackageName().toString();
        if (!packageName.contains(WX_PACKAGE_NAME)) {
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

                    if (listItemCount == 0){
                        listItemCount = event.getItemCount();
                    }else {
                        //内容数量没变化
                        if (listItemCount == event.getItemCount()){
                            itemNoChangeCount++;
                        }else {
                            listItemCount = event.getItemCount();
                            itemNoChangeCount = 0;
                            //内容变化了。
                            Log.d(TAG+"-变化",count+++"页");
                        }

                        //叛变结束标志
                        if (itemNoChangeCount>=maxItemNoChange){
                            isend = false;
                        }
                    }


//                    Toast.makeText(this,"LENGTH_SHORT",Toast.LENGTH_LONG).show();
                    //结束了
                    if (event.getFromIndex()<=0){
                        Log.d(TAG,"结束了:"+event.getFromIndex() +" ev:" +event.getToIndex() +"");
                    }
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
    //滑动方向
    public int action ;
    public ImageReader mImageReader;

    /**
     * 截屏
     */
    public void jieping(int scroll){

        isStartJiepin = true;
        action = scroll;
        int count =0;
        while (true && nodeInfoList!=null && !isend ) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //最大截屏数量
            if (count>1000){
                return;
            }
            Log.d(TAG,"循环:" +count++);
            if (mImageReader!=null){
                jiepin();
            }
            nodeInfoList.performAction(scroll);
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
        }
    };



    @Override
    public void onInterrupt() {

    }



    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

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
        Log.i(TAG,"捷帕尼开始");
//        ImageView imageView =findViewById(R.id.img_show);
//        imageView.setImageBitmap(bitmap);

    }


}
