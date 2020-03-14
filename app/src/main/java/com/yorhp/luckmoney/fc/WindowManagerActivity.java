package com.yorhp.luckmoney.fc;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.yorhp.luckmoney.R;
import com.yorhp.luckmoney.fc.WindowManager.WindowController;
import com.yorhp.luckmoney.fc.WindowManager.WindowUtil;
import com.yorhp.luckmoney.service.WxScanAccessibilityService;
import com.yorhp.luckmoney.util.AccessbilityUtil;


import androidx.appcompat.app.AppCompatActivity;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD;
import static com.yorhp.luckmoney.service.WxScanAccessibilityService.jieping_msg;

/**
 * 聊天窗口
 */
public class WindowManagerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE = 101;
    Switch aSwitch;
    Switch startFz;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fc);
        startFz = findViewById(R.id.start_fz);
        aSwitch = findViewById(R.id.swWx);
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
                toast("请检查辅助功能，或者滑动页面。");
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

        startFz.setChecked(AccessbilityUtil.isAccessibilitySettingsOn(this, WxScanAccessibilityService.class));


    }
}