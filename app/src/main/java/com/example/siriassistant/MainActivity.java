package com.example.siriassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * 主界面：Siri 风格语音助手
 * 点击多彩球 -> 语音识别 -> 关键词应答 -> TTS 朗读
 */
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final int REQ_VOICE = 1001;
    private static final int REQ_RECORD_PERMISSION = 1002;

    private TextView tvStatus;
    private TextView tvHint;
    private SiriButtonView siriButton;
    private WaveView waveView;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String pendingSpeak = null;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 沉浸式状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(0xFF0A0E21);
        }
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvHint = findViewById(R.id.tv_hint);
        siriButton = findViewById(R.id.siri_button);
        waveView = findViewById(R.id.wave_view);

        // 点击多彩球 -> 开始说话
        siriButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListening();
            }
        });

        // 初始化文字转语音
        tts = new TextToSpeech(this, this);
    }

    /** 开始语音识别（先检查录音权限） */
    private void startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
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
                // 拒绝录音：改为手动输入
                showInputDialog();
            }
        }
    }

    /** 调用系统语音识别 */
    private void doRecognize() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出你的问题");
        try {
            setBusy(true, "正在聆听…");
            startActivityForResult(intent, REQ_VOICE);
        } catch (ActivityNotFoundException e) {
            // 设备没有语音识别服务：退回手动输入
            Toast.makeText(this, "设备不支持语音识别，改为手动输入", Toast.LENGTH_SHORT).show();
            showInputDialog();
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
                    handleQuestion(results.get(0));
                    return;
                }
            }
            setBusy(false, "点击球体，开始说话");
            tvHint.setText("没有听清，再试一次吧");
        }
    }

    /** 处理用户问题：关键词匹配 -> 回答 */
    private void handleQuestion(String question) {
        if (TextUtils.isEmpty(question)) return;
        setBusy(true, "正在思考…");
        String answer;

        if (containsAny(question, "你好", "您好", "hello", "hi", "嗨")) {
            answer = pick("你好呀！我是你的 AI 助手，有什么可以帮你？",
                    "嗨，很高兴见到你！");
        } else if (containsAny(question, "几点", "时间")) {
            answer = "现在时间是 " + new SimpleDateFormat("HH点mm分", Locale.CHINA).format(new Date());
        } else if (containsAny(question, "日期", "几号", "星期", "礼拜")) {
            answer = "今天是 " + new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA).format(new Date());
        } else if (containsAny(question, "天气")) {
            answer = "建议你打开手机自带的天气应用查看最新天气哦。";
        } else if (containsAny(question, "你是谁", "介绍", "名字")) {
            answer = "我是 Siri 风格的 AI 语音助手，可以陪你聊天、报时、讲笑话。";
        } else if (containsAny(question, "笑话", "段子", "开心")) {
            answer = pick("为什么手机总是很热？因为它在发烧友的手里呀！",
                    "小明问妈妈：为什么我名字叫小明？妈妈说：因为当时爸爸想让你长大以后当明星呀！",
                    "我买了一双鞋，结果发现鞋垫是活的，一穿上就会走路……");
        } else if (containsAny(question, "再见", "拜拜", "晚安")) {
            answer = "再见啦，记得常来找我聊天哦！";
        } else if (containsAny(question, "相机", "拍照")) {
            answer = "好的，帮你打开相机。";
            speakThenOpen(answer, new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE));
            return;
        } else if (containsAny(question, "设置")) {
            answer = "好的，帮你打开系统设置。";
            speakThenOpen(answer, new Intent(android.provider.Settings.ACTION_SETTINGS));
            return;
        } else if (containsAny(question, "计算", "等于", "多少")) {
            answer = tryCalculate(question);
        } else {
            answer = "这个问题我还不太会，换个问题问我吧，比如问我现在几点、讲个笑话。";
        }

        speakAndShow(answer);
    }

    /** 朗读并显示回答 */
    private void speakAndShow(String answer) {
        setBusy(true, "正在回答…");
        tvHint.setText(answer);
        if (ttsReady) {
            tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "siri-reply");
        } else {
            pendingSpeak = answer;
        }
        // 停顿后恢复待机
        siriButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                setBusy(false, "点击球体，开始说话");
            }
        }, 5000);
    }

    /** 朗读后再跳转应用 */
    private void speakThenOpen(final String answer, final Intent openIntent) {
        speakAndShow(answer);
        siriButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    startActivity(openIntent);
                } catch (ActivityNotFoundException ignored) {
                }
            }
        }, 2500);
    }

    /** 简单四则运算：如"3加5等于多少" -> 8 */
    private String tryCalculate(String question) {
        try {
            String q = question.replace("等于多少", "").replace("等于", "")
                    .replace("是多少", "").replace("？", "").replace("?", "");
            int a = 0, b = 0, op = 0; // op: 1加 2减 3乘 4除
            boolean found = false;
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
                found = true;
            }
            if (!found) return null;
            double r = op == 1 ? a + b : op == 2 ? a - b : op == 3 ? a * b : (double) a / b;
            String opName = op == 1 ? "加" : op == 2 ? "减" : op == 3 ? "乘" : "除";
            if (r == Math.floor(r)) {
                return a + opName + b + "等于" + (int) r;
            }
            return a + opName + b + "约等于" + String.format(Locale.CHINA, "%.2f", r);
        } catch (Exception e) {
            return null;
        }
    }

    /** 手动输入问题（无语音服务或拒绝权限时） */
    private void showInputDialog() {
        final EditText input = new EditText(this);
        input.setHint("输入你的问题");
        new AlertDialog.Builder(this)
                .setTitle("手动输入")
                .setView(input)
                .setPositiveButton("提问", (d, w) -> {
                    String q = input.getText().toString().trim();
                    if (!q.isEmpty()) handleQuestion(q);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean containsAny(String text, String... keys) {
        String t = text.toLowerCase(Locale.CHINA);
        for (String k : keys) {
            if (t.contains(k.toLowerCase(Locale.CHINA))) return true;
        }
        return false;
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    /** 切换忙碌/待机状态（控制波形动画） */
    private void setBusy(boolean busy, String status) {
        tvStatus.setText(status);
        if (busy) {
            waveView.start();
        } else {
            waveView.stop();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int r = tts.setLanguage(Locale.CHINESE);
            ttsReady = (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED);
            if (pendingSpeak != null) {
                tts.speak(pendingSpeak, TextToSpeech.QUEUE_FLUSH, null, "siri-reply");
                pendingSpeak = null;
            }
        }
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
