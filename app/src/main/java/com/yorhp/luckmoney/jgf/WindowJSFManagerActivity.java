package com.yorhp.luckmoney.jgf;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.yorhp.luckmoney.R;
import com.yorhp.luckmoney.fc.WindowManager.WindowJSFController;
import com.yorhp.luckmoney.fc.WindowManager.WindowUtil;
import com.yorhp.luckmoney.service.WxScanAccessibilityService;
import com.yorhp.luckmoney.txl.bean.WxUser;
import com.yorhp.luckmoney.util.AccessbilityUtil;
import com.yorhp.luckmoney.util.ScreenUtil;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD;
import static com.yorhp.luckmoney.service.WxScanAccessibilityService.jieping_msg;
import static com.yorhp.luckmoney.service.WxScanAccessibilityService.jsf;
import static com.yorhp.luckmoney.service.WxScanAccessibilityService.txl;

/**
 * 聊天窗口
 */
public class WindowJSFManagerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE = 101;
    private static final int REQUEST_MEDIA_PROJECTION = 300;
    Switch aSwitch;
    Switch startFz;

    List<String> wxUsers;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        ScreenUtil.getScreenSize(this);
        setContentView(R.layout.activity_txl);
        startFz = findViewById(R.id.start_fz);
        aSwitch = findViewById(R.id.swWx);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    start_window(aSwitch);
                }else {
                    WindowJSFController.getInstance().destroyThumbWindow();
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
                if (WxScanAccessibilityService.mService.mImageReader == null){
                    toast("没有开启截屏");
                    return;
                }
                WxScanAccessibilityService.mService.scan_type = jieping_msg;
                //打开微信，开启服装
                openWx();
            }

        });
    }

    /**
     * 开启浮动窗口，方便操作x
     * @param view
     */
    public void start_window(View view) {
        if (!WindowUtil.canOverDraw(this)) {
            //跳转到设置页面
            WindowUtil.jump2Setting(this, REQUEST_CODE);
            return;
        }


        if (!startFz.isChecked() || WxScanAccessibilityService.mService== null){
            toast("开启服务");
            return;
        }
        //开启悬浮窗
        WindowJSFController.getInstance().showThumbWindow(this);

        openWx();

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

        }
    }

    private void toast(String s) {
        Toast.makeText(this,s+"",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WindowJSFController.getInstance().destroyThumbWindow();
    }

    public void close_window(View view) {
        WindowJSFController.getInstance().destroyThumbWindow();
    }

    @Override
    public void onClick(View v) {
        //开始按钮
        if (v.getId() == R.id.btn_start){
            if (WxScanAccessibilityService.mService!=null ) {
                Button button = (Button) v;
                if (button.getText().toString().equalsIgnoreCase("结束")) {
                    WindowJSFController.getInstance().destroyThumbWindow();
                    WxScanAccessibilityService.mService.isStartTxl = false;
                    WxScanAccessibilityService.mService.isend = true;
                } else {
                    button.setText("结束");
                    toast("竖向准备开始了");
                    mHandler.sendEmptyMessageDelayed(100, 1000);
                }
            }else {
               if (WxScanAccessibilityService.mService == null){
                    toast("请重启辅助功能");
                }
            }
        }
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //朋友圈
                        WxScanAccessibilityService.mService.isend = false;
                        //开始截屏
                        WxScanAccessibilityService.mService.isStartJiepin = false;
                        WxScanAccessibilityService.mService.isStartTxl = false;
                        WxScanAccessibilityService.mService.scan_type = jsf;
                        wxUsers = WxScanAccessibilityService.mService.scanJSF(ACTION_SCROLL_FORWARD);
                        mHandler.sendEmptyMessage(1001);
                    }
                }).start();
            }
            if (msg.what == 1001){
                WindowJSFController.getInstance().destroyThumbWindow();

                TextView textView =  findViewById(R.id.tv_result);
                StringBuffer stringBuffer = new StringBuffer();
//                for (String wxUser:wxUsers){
//                    stringBuffer.append("用户："+wxUser+"\n");
//                }
                textView.setText(stringBuffer);

            }
        }
    };

    private void openWx() {

        if(WxScanAccessibilityService.mService!=null){
            WxScanAccessibilityService.mService.scan_type = txl;
        }

        //打开微信
        String weChatPackageName = "com.tencent.mm";
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName cn = new ComponentName(weChatPackageName, "com.tencent.mm.ui.LauncherUI");
        intent.setComponent(cn);
        startActivity(intent);

    }



    @Override
    protected void onResume() {
        super.onResume();

        startFz.setChecked(AccessbilityUtil.isAccessibilitySettingsOn(this, WxScanAccessibilityService.class) && WxScanAccessibilityService.mService!=null);

        if (WxScanAccessibilityService.mService!=null && WxScanAccessibilityService.mService.isStartJiepin) {
            aSwitch.setChecked(true);
        }else {
            aSwitch.setChecked(false);
        }
    }



}
