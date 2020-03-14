package com.yorhp.luckmoney;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.yorhp.luckmoney.service.WxScanAccessibilityService;
import com.yorhp.luckmoney.util.AccessbilityUtil;
import com.yorhp.luckmoney.util.PollingUtil;
import com.yorhp.luckmoney.util.ScreenUtil;
import com.yorhp.luckmoney.util.SharedPreferencesUtil;

import androidx.appcompat.app.AppCompatActivity;

/**
 * @author yorhp
 */
public class MainActivity extends AppCompatActivity {

    Switch swWx;
    CheckBox ckSingle,ckPause;
    TextView tvTime,tvOpenTime,tvDevice;

    /**
     * 等待红包弹出窗时间
     */
    private static final int MAX_WAIT_WINDOW_TIME=2000;

    /**
     * 保存状态字段
     */
    public static final String NEED_SET_TIME="need_set_time";
    public static final String WAIT_WINDOW_TIME="waitWindowTime";
    public static final String WAIT_GET_MONEY_TIME="waitGetMoneyTime";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferencesUtil.init(getApplication());
        setContentView(R.layout.activity_main);
        ScreenUtil.getScreenSize(this);
        ckSingle = findViewById(R.id.ckSingle);
        ckPause = findViewById(R.id.ckPause);
        swWx = findViewById(R.id.swWx);
        tvDevice=findViewById(R.id.tv_device);
        tvTime=findViewById(R.id.tv_wait_time);
        WxScanAccessibilityService.waitWindowTime=SharedPreferencesUtil.getInt(WAIT_WINDOW_TIME,150);
        tvTime.setText(WxScanAccessibilityService.waitWindowTime+"ms");
        tvOpenTime=findViewById(R.id.tv_wait_open_time);
        WxScanAccessibilityService.waitGetMoneyTime=SharedPreferencesUtil.getInt(WAIT_GET_MONEY_TIME,700);
        tvOpenTime.setText(WxScanAccessibilityService.waitGetMoneyTime+"ms");
        swWx.setOnClickListener((v) -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        findViewById(R.id.ll_wait_time).setOnClickListener(v->{
            if(WxScanAccessibilityService.needSetTime==0){
                Toast.makeText(MainActivity.this,"当前不可修改",Toast.LENGTH_SHORT).show();
                return;
            }
            if(WxScanAccessibilityService.waitWindowTime<MAX_WAIT_WINDOW_TIME/4){
                WxScanAccessibilityService.waitWindowTime= WxScanAccessibilityService.waitWindowTime+30;
            }else {
                WxScanAccessibilityService.waitWindowTime=0;
            }
            tvTime.setText(WxScanAccessibilityService.waitWindowTime+"ms");
        });

        findViewById(R.id.ll_wait_open_time).setOnClickListener(v->{
            if(WxScanAccessibilityService.needSetTime==0){
                Toast.makeText(MainActivity.this,"当前不可修改",Toast.LENGTH_SHORT).show();
                return;
            }
            if(WxScanAccessibilityService.waitGetMoneyTime<MAX_WAIT_WINDOW_TIME){
                WxScanAccessibilityService.waitGetMoneyTime= WxScanAccessibilityService.waitGetMoneyTime+100;
            }else {
                WxScanAccessibilityService.waitGetMoneyTime=0;
            }
            tvOpenTime.setText(WxScanAccessibilityService.waitGetMoneyTime+"ms");
        });


        startJob();

    }

    PollingUtil pollingUtil = new PollingUtil(new Handler());
    /**
     * 开启job
     */
    public void startJob(){
        //每3秒打印一次日志
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.e("MainActivity", "----------handler 定时轮询任务----------");
                if (WxScanAccessibilityService.mService!=null) {
//                    LuckMoneyService.mService.performScrollBackward();
                }

            }
        };
//        pollingUtil.startPolling(runnable, 3000, true);


    }

    @Override
    protected void onResume() {
        super.onResume();
        if(WxScanAccessibilityService.needSetTime==-1){
            WxScanAccessibilityService.needSetTime=SharedPreferencesUtil.getInt(NEED_SET_TIME,-1);
        }
        swWx.setChecked(AccessbilityUtil.isAccessibilitySettingsOn(this, WxScanAccessibilityService.class));
        if(WxScanAccessibilityService.needSetTime==1){
            tvDevice.setText("当前设备需要进行下面两项时间设置以达到最佳状态，值的大小不会影响抢红包的速度，值越大越能确保抢到红包，但是值太大返回流程可能会出问题，无法继续抢下一个");
        }else if(WxScanAccessibilityService.needSetTime==0){
            tvDevice.setText("当前设备不需要关心下面两项设置");
        }
        SharedPreferencesUtil.save(NEED_SET_TIME, WxScanAccessibilityService.needSetTime);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferencesUtil.save(WAIT_WINDOW_TIME, WxScanAccessibilityService.waitWindowTime);
        SharedPreferencesUtil.save(WAIT_GET_MONEY_TIME, WxScanAccessibilityService.waitGetMoneyTime);
    }


}
