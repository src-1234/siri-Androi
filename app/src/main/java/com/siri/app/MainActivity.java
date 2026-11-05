package com.siri.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Siri 风格安卓语音助手
 * 点击光球 -> 麦克风 -> 语音识别结果进输入框 -> 发送 -> 快捷指令应答 + TTS 朗读
 */
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final int REQ_VOICE = 1001;
    private static final int REQ_RECORD_PERMISSION = 1002;

    private EditText etInput;
    private ScrollView chatScroll;
    private LinearLayout chatContainer;
    private ImageView ivSiri;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private Random random = new Random();

    // 常用应用包名映射（快捷指令：打开应用）
    private static final Map<String, String> APP_MAP = new HashMap<>();

    static {
        APP_MAP.put("微信", "com.tencent.mm");
        APP_MAP.put("qq", "com.tencent.mobileqq");
        APP_MAP.put("抖音", "com.ss.android.ugc.aweme");
        APP_MAP.put("相机", "camera");
        APP_MAP.put("相册", "gallery");
        APP_MAP.put("设置", "settings");
        APP_MAP.put("浏览器", "browser");
        APP_MAP.put("音乐", "music");
        APP_MAP.put("电话", "dialer");
        APP_MAP.put("短信", "sms");
        APP_MAP.put("日历", "calendar");
        APP_MAP.put("计算器", "calculator");
        APP_MAP.put("地图", "map");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etInput = findViewById(R.id.et_input);
        chatScroll = findViewById(R.id.chat_scroll);
        chatContainer = findViewById(R.id.chat_container);
        ivSiri = findViewById(R.id.iv_siri);
        ImageButton btnSend = findViewById(R.id.btn_send);

        // 初始化 TTS（中文朗读）
        tts = new TextToSpeech(this, this);

        // 点击 Siri 光球 -> 启动麦克风
        ivSiri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListening();
            }
        });

        // 发送按钮：空输入发不了（水印 Ask Siri 不参与发送）
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInput();
            }
        });

        // 键盘发送键
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            sendInput();
            return true;
        });
    }

    // ==================== 语音识别 ====================
    private void startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                // 先请求麦克风权限
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},
                        REQ_RECORD_PERMISSION);
                return;
            }
        }
        doRecognize();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doRecognize();
            } else {
                Toast.makeText(this, "没有麦克风权限，无法语音输入", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doRecognize() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出指令");
        try {
            startActivityForResult(intent, REQ_VOICE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "设备不支持语音识别，请直接输入文字", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VOICE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    // 语音结果直接传送到输入框
                    etInput.setText(results.get(0));
                    etInput.setSelection(etInput.getText().length());
                }
            }
        }
    }

    // ==================== 发送逻辑 ====================
    private void sendInput() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            // 水印 Ask Siri... 时直接点发送是发不了的
            Toast.makeText(this, "请先说话或输入内容", Toast.LENGTH_SHORT).show();
            return;
        }
        // 显示用户气泡
        addBubble(text, true);
        etInput.setText("");
        hideKeyboard();

        // 解析并回复
        String reply = getReply(text);
        addBubble(reply, false);
        speak(reply);
    }

    // ==================== 快捷指令应答 ====================
    private String getReply(String input) {
        // 1. 打开应用
        for (Map.Entry<String, String> e : APP_MAP.entrySet()) {
            if (input.contains("打开" + e.getKey()) || input.contains("启动" + e.getKey())
                    || input.equals(e.getKey())) {
                if (openApp(e.getValue())) {
                    return "好的，帮你打开" + e.getKey();
                }
                return "找不到" + e.getKey() + "，请确认已安装";
            }
        }
        // 2. 天气
        if (input.contains("天气")) {
            if (openWeather()) {
                return "好的，为你打开天气";
            }
            return "没有找到天气应用，请先安装一个天气软件";
        }
        // 3. 定时 / 闹钟
        if (input.contains("定时") || input.contains("闹钟") || input.contains("提醒")) {
            if (openAlarm()) {
                return "好的，打开闹钟设置，帮你定时";
            }
            return "无法打开闹钟设置";
        }
        // 4. 时间
        if (input.contains("几点") || input.contains("时间")) {
            return "现在是 " + new SimpleDateFormat("HH点mm分", Locale.CHINA).format(new Date());
        }
        // 5. 日期
        if (input.contains("日期") || input.contains("几号") || input.contains("星期")) {
            return "今天是 " + new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date());
        }
        // 6. 计算
        if (input.contains("计算") || input.contains("等于") || input.contains("多少")) {
            String r = tryCalculate(input);
            if (r != null) return r;
        }
        // 7. 笑话
        if (input.contains("笑话") || input.contains("开心")) {
            return pick("为什么手机总是很热？因为在发烧友手里呀！",
                    "小明问妈妈：为什么我总被叫小明？妈妈说：因为你爸爸姓小呀！",
                    "为什么鸟不会玩手机？因为怕戳破屏幕！");
        }
        // 8. 你好
        if (input.contains("你好") || input.contains("嗨") || input.contains("hello")) {
            return "你好呀！有什么可以帮你？可以说：打开微信、查天气、定时、讲个笑话。";
        }
        // 默认兜底
        return "我是 Siri 风格的语音助手，可以试试对我说：打开微信、天气、定时、讲个笑话。";
    }

    // ---- 打开应用 ----
    private boolean openApp(String key) {
        Intent intent = null;
        switch (key) {
            case "camera":
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                break;
            case "gallery":
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("image/*");
                break;
            case "settings":
                intent = new Intent(Settings.ACTION_SETTINGS);
                break;
            case "browser":
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.baidu.com"));
                break;
            case "music":
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("audio/*");
                break;
            case "dialer":
                intent = new Intent(Intent.ACTION_DIAL);
                break;
            case "sms":
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("vnd.android-dir/mms-sms");
                break;
            case "calendar":
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("vnd.android.cursor.item/event");
                break;
            case "calculator":
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("vnd.android.cursor.item/event");
                break;
            case "map":
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=郑州"));
                break;
            default:
                intent = getPackageManager().getLaunchIntentForPackage(key);
        }
        if (intent == null && !key.equals("camera")) {
            intent = getPackageManager().getLaunchIntentForPackage(key);
        }
        try {
            if (intent != null) {
                startActivity(intent);
                return true;
            }
        } catch (ActivityNotFoundException ignored) {
        }
        return false;
    }

    // ---- 打开天气应用（常见品牌包名） ----
    private boolean openWeather() {
        String[] pkgs = {
                "com.miui.weather2",             // 小米
                "com.huawei.android.totemweather",// 华为
                "com.oplus.weather",             // OPPO/一加
                "com.vivo.weather",              // vivo
                "com.samsung.android.weather",   // 三星
                "com.android.weather"            // 其他
        };
        for (String p : pkgs) {
            Intent i = getPackageManager().getLaunchIntentForPackage(p);
            if (i != null) {
                try {
                    startActivity(i);
                    return true;
                } catch (ActivityNotFoundException ignored) {
                }
            }
        }
        // 兜底：系统天气查询页面
        Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("weather://today"));
        try {
            startActivity(fallback);
            return true;
        } catch (ActivityNotFoundException ignored) {
        }
        return false;
    }

    // ---- 打开闹钟设置（定时） ----
    private boolean openAlarm() {
        try {
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, "Siri 定时提醒");
            startActivity(i);
            return true;
        } catch (ActivityNotFoundException ignored) {
        }
        return false;
    }

    // ---- 简单四则运算 ----
    private String tryCalculate(String input) {
        try {
            String q = input.replace("等于多少", "").replace("等于", "")
                    .replace("是多少", "").replace("？", "").replace("?", "");
            int a = 0, b = 0, op = 0;
            String[] parts = null;
            if (q.contains("加") || q.contains("+")) {
                parts = q.split("加|\\+"); op = 1;
            } else if (q.contains("减") || q.contains("-")) {
                parts = q.split("减|-"); op = 2;
            } else if (q.contains("乘") || q.contains("×") || q.contains("*")) {
                parts = q.split("乘|×|\\*"); op = 3;
            } else if (q.contains("除") || q.contains("÷") || q.contains("/")) {
                parts = q.split("除|÷|/"); op = 4;
            }
            if (parts != null && parts.length == 2) {
                a = Integer.parseInt(parts[0].trim().replaceAll("[^0-9]", ""));
                b = Integer.parseInt(parts[1].trim().replaceAll("[^0-9]", ""));
                double r = op == 1 ? a + b : op == 2 ? a - b : op == 3 ? a * b : (double) a / b;
                String opName = op == 1 ? "加" : op == 2 ? "减" : op == 3 ? "乘" : "除";
                if (r == Math.floor(r)) return a + opName + b + "等于" + (int) r;
                return a + opName + b + "约等于" + String.format(Locale.CHINA, "%.2f", r);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    // ==================== 对话气泡 ====================
    private void addBubble(String text, boolean isUser) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15f);
        tv.setPadding(dp(16), dp(10), dp(16), dp(10));
        tv.setTextColor(isUser ? 0xFFFFFFFF : 0xFF111111);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        if (isUser) {
            tv.setBackgroundResource(R.drawable.bubble_user);
            lp.gravity = android.view.Gravity.END;
        } else {
            tv.setBackgroundResource(R.drawable.bubble_siri);
            lp.gravity = android.view.Gravity.START;
        }
        chatContainer.addView(tv, lp);
        // 滚动到底部
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    // ==================== TTS 朗读 ====================
    private void speak(String text) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "siri-reply");
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int r = tts.setLanguage(Locale.CHINESE);
            ttsReady = (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etInput.getWindowToken(), 0);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
