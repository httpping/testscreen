package com.yorhp.luckmoney.fc;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.yorhp.luckmoney.R;
import com.yorhp.luckmoney.fc.WindowManager.WindowController;
import com.yorhp.luckmoney.fc.WindowManager.WindowUtil;
import com.yorhp.luckmoney.service.WxScanAccessibilityService;
import com.yorhp.luckmoney.util.AccessbilityUtil;
import com.yorhp.luckmoney.util.FileUtil;
import com.yorhp.luckmoney.util.ScreenUtil;


import java.nio.ByteBuffer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD;
import static com.yorhp.luckmoney.service.WxScanAccessibilityService.jieping_msg;

/**
 * 聊天窗口
 */
public class WindowManagerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE = 101;
    private static final int REQUEST_MEDIA_PROJECTION = 300;
    Switch aSwitch;
    Switch startFz;
    Switch startJp;
    //截屏用
    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;
    private VirtualDisplay mVirtualDisplay;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,10010);
        }

        ScreenUtil.getScreenSize(this);
        setContentView(R.layout.activity_fc);
        startFz = findViewById(R.id.start_fz);
        aSwitch = findViewById(R.id.swWx);
        startJp = findViewById(R.id.openJp);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    start_window(aSwitch);
                }else {
                    WindowController.getInstance().destroyThumbWindow();
                }
            }
        });

        findViewById(R.id.btn_end).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WxScanAccessibilityService.mService!=null){
                    WxScanAccessibilityService.mService.isend= true;
                }
            }
        });

        startJp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    startJP();
                }else {
                    mImageReader = null;
                    if (WxScanAccessibilityService.mService!=null){
                        WxScanAccessibilityService.mService.mImageReader = null;
                    }
                }
            }
        } );

        startFz.setOnClickListener((v) -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        findViewById(R.id.btn_openwx).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WxScanAccessibilityService.mService==null){
                    toast("请重新开启辅助功能");
                    return;
                }
                WxScanAccessibilityService.mService.scan_type = jieping_msg;
                //打开微信，开启服装
                openWx();
            }

        });
    }

    /**
     * 开启浮动窗口，方便操作
     * @param view
     */
    public void start_window(View view) {
        if (!WindowUtil.canOverDraw(this)) {
            //跳转到设置页面
            WindowUtil.jump2Setting(this, REQUEST_CODE);
            return;
        }

        if (startJp.isChecked() && startFz.isChecked()){
            openWx();
        }else{
            toast("确保所有准备操作都已处理");
            return;
        }
            //开启悬浮窗
        WindowController.getInstance().showThumbWindow(this);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
                if (!WindowUtil.canOverDraw(this)) {
                    toast("悬浮窗权限未开启，请在设置中手动打开");
                    return;
                }
                start_window(aSwitch);
                break;
            case REQUEST_MEDIA_PROJECTION:
                int mResultCode = resultCode;
                mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, data);
                mImageReader = ImageReader.newInstance(ScreenUtil.SCREEN_WIDTH, ScreenUtil.SCREEN_HEIGHT, 0x1, 2); //ImageFormat.RGB_565
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                        ScreenUtil.SCREEN_WIDTH, ScreenUtil.SCREEN_HEIGHT, ScreenUtil.density,       DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mImageReader.getSurface(), null, null);
                if (WxScanAccessibilityService.mService!=null) {
                    WxScanAccessibilityService.mService.mImageReader = mImageReader;
                }
                break;
        }
    }

    private void toast(String s) {
        Toast.makeText(this,s+"",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WindowController.getInstance().destroyThumbWindow();
    }

    public void close_window(View view) {
        WindowController.getInstance().destroyThumbWindow();
    }

    @Override
    public void onClick(View v) {
        //开始按钮
        if (v.getId() == R.id.btn_start){
            if (WxScanAccessibilityService.mService!=null && WxScanAccessibilityService.mService.nodeInfoList!=null) {
                toast("准备开始了");
                close_window(null);
                mHandler.sendEmptyMessageDelayed(100,1000);
            }else {

                if (WxScanAccessibilityService.mService == null){
                    toast("请重启辅助功能");
                }else {
                    toast("请重新切换页面，或手动滑动页面");
                }
            }
        }
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        WxScanAccessibilityService.mService.isend =false;
                        //页面向上滑动，截图, 微信就修改滑动方向就可以了
                        WxScanAccessibilityService.mService.jieping(ACTION_SCROLL_BACKWARD);
                    }
                }).start();
            }
        }
    };

    private void openWx() {

        if(WxScanAccessibilityService.mService!=null){
            WxScanAccessibilityService.mService.scan_type = jieping_msg;
        }

        //打开微信
        String weChatPackageName = "com.tencent.mm";
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName cn = new ComponentName(weChatPackageName, "com.tencent.mm.ui.LauncherUI");
        intent.setComponent(cn);
        startActivity(intent);

      /* if (mImageReader == null){
           startJP();
       }else {
           jiepin();
       }*/

    }
    MediaProjectionManager mMediaProjectionManager;
    public void startJP(){
        mMediaProjectionManager = (MediaProjectionManager)getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }



    @Override
    protected void onResume() {
        super.onResume();

        startFz.setChecked(AccessbilityUtil.isAccessibilitySettingsOn(this, WxScanAccessibilityService.class) && WxScanAccessibilityService.mService!=null);

        if (WxScanAccessibilityService.mService!=null && WxScanAccessibilityService.mService.mImageReader!=null) {
            startJp.setChecked(true);
        }else {
            startJp.setChecked(false);
        }
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
//        ImageView imageView =findViewById(R.id.img_show);
//        imageView.setImageBitmap(bitmap);

    }
}
