package cn.qwenwork.siriwake;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 唤醒插件主界面：一个开关。
 * 打开 -> 前台服务常驻后台聆听「嘿 Siri」；
 * 听到唤醒词 -> 自动打开 Siri 助手主应用，语音文本进输入框自动发送，回答显示在主页面。
 */
public class MainActivity extends Activity {

    private static final int REQ_MIC = 1;
    private static final int REQ_NOTIF = 2;

    private Button toggleBtn;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setFitsSystemWindows(true);
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#1A84B8"), Color.parseColor("#123072")});
        root.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("Siri 唤醒");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        root.addView(title, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(this);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        status.setTextColor(Color.parseColor("#CCFFFFFF"));
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(16), 0, dp(28));
        root.addView(status, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        toggleBtn = new Button(this);
        toggleBtn.setTextColor(Color.WHITE);
        toggleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#33FFFFFF"));
        btnBg.setCornerRadius(dp(26));
        btnBg.setStroke(Math.max(1, dp(1)), Color.parseColor("#66FFFFFF"));
        toggleBtn.setBackground(btnBg);
        toggleBtn.setPadding(dp(40), dp(16), dp(40), dp(16));
        toggleBtn.setOnClickListener(v -> onToggle());
        root.addView(toggleBtn, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tip = new TextView(this);
        tip.setText("开启后会常驻后台聆听，对手机说「嘿 Siri」即可唤醒助手。\n需要同时安装 Siri 助手主应用。");
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tip.setTextColor(Color.parseColor("#99FFFFFF"));
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(dp(40), dp(36), dp(40), 0);
        root.addView(tip, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void refreshUi() {
        boolean on = WakeService.running;
        toggleBtn.setText(on ? "停止值守" : "开始聆听");
        status.setText(on
            ? "正在聆听「嘿 Siri」…\n听到后自动打开助手并回答"
            : "值守未开启");
    }

    private void onToggle() {
        if (WakeService.running) {
            stopService(new Intent(this, WakeService.class));
            Toast.makeText(this, "已停止聆听", Toast.LENGTH_SHORT).show();
            refreshUi();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        maybeAskNotifPermission();
        startService(new Intent(this, WakeService.class));
        refreshUi();
    }

    private void maybeAskNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                   != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                maybeAskNotifPermission();
                startService(new Intent(this, WakeService.class));
                refreshUi();
            } else {
                Toast.makeText(this, "需要麦克风权限才能聆听唤醒词", Toast.LENGTH_LONG).show();
            }
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
