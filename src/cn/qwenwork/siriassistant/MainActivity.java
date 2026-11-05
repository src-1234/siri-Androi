package cn.qwenwork.siriassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 主界面：完全复刻参考设计（渐变背景、Hey Siri 标题、Siri 光球、
 * 液态玻璃输入框 + 蓝色圆形发送按钮）。
 *
 * - 点击 Siri 光球 -> 启动麦克风语音识别（需麦克风权限），识别结果传送到输入框
 * - 输入框空时（只有 Ask Siri 水印）点发送无效
 * - 对话界面不显示快捷指令入口，输入命令才会触发系统插件（快捷指令）
 */
public class MainActivity extends Activity {

    private static final int REQ_MIC = 100;

    private LinearLayout root, chatList;
    private ScrollView chatScroll;
    private View spacer;
    private ImageView orb;
    private EditText input;
    private LinearLayout sendBtn;

    private Assistant assistant;
    private SpeechRecognizer recognizer;
    private boolean listening = false;
    private boolean chatting = false;

    private static final int BG_TOP = Color.parseColor("#1A84B8");
    private static final int BG_BOTTOM = Color.parseColor("#123072");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assistant = new Assistant(this);
        buildUi();
    }

    // ---------------- UI ----------------

    private void buildUi() {
        Context c = this;
        root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);

        // 背景：蓝绿 -> 深蓝垂直渐变（取自参考图）
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{BG_TOP, BG_BOTTOM});
        root.setBackground(bg);

        // 标题 "Hey, Siri"
        TextView title = new TextView(c);
        title.setText(R.string.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 38);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) title.getLayoutParams();
        lp.topMargin = dp(64);

        // Siri 光球（复刻参考图，白色描边 + 彩色漩涡 + 发光核心）
        orb = new ImageView(c);
        orb.setImageResource(R.drawable.orb);
        orb.setScaleType(ImageView.ScaleType.FIT_CENTER);
        orb.setOnClickListener(v -> onOrbClicked());
        // 点击时不允许长按弹出菜单 / 下载等行为
        orb.setLongClickable(false);
        orb.setOnLongClickListener(v -> true);
        LinearLayout.LayoutParams orbLp = new LinearLayout.LayoutParams(dp(250), dp(250));
        orbLp.topMargin = dp(30);
        orbLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(orb, orbLp);
        startPulse();

        // 对话区（初始不显示，保持与参考主页一致）
        chatScroll = new ScrollView(c);
        chatScroll.setFillViewport(true);
        chatScroll.setVerticalScrollBarEnabled(false);
        chatScroll.setVisibility(View.GONE);
        chatList = new LinearLayout(c);
        chatList.setOrientation(LinearLayout.VERTICAL);
        chatList.setPadding(dp(20), dp(12), dp(20), dp(12));
        chatScroll.addView(chatList, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(chatScroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 占位（无对话时把光球撑在页面上部，与参考图一致）
        spacer = new View(c);
        root.addView(spacer, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 底部输入区
        root.addView(buildInputBar(c), new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    /** 液态玻璃药丸输入框 + 蓝色圆形发送按钮。 */
    private View buildInputBar(Context c) {
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(16), dp(20), dp(24));

        // 玻璃输入框容器
        GradientDrawable glass = new GradientDrawable();
        glass.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        glass.setColor(Color.parseColor("#33153255"));
        glass.setCornerRadius(dp(30));
        glass.setStroke(Math.max(1, dp(1)), Color.parseColor("#4DFFFFFF"));

        LinearLayout inputBox = new LinearLayout(c);
        inputBox.setBackground(glass);
        inputBox.setOrientation(LinearLayout.HORIZONTAL);
        inputBox.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(c);
        input.setBackgroundResource(0);
        input.setHint(R.string.hint); // "Ask Siri" 水印，输入后自动消失
        input.setHintTextColor(Color.parseColor("#99FFFFFF"));
        input.setTextColor(Color.WHITE);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setSingleLine(true);
        input.setMaxLines(1);
        input.setPadding(dp(22), dp(16), dp(12), dp(16));
        input.setImeActionLabel("发送", 0);
        input.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
        });
        inputBox.addView(input, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(0, dp(60), 1f);
        boxLp.rightMargin = dp(14);
        bar.addView(inputBox, boxLp);

        // 发送按钮：亮蓝色圆形渐变 + 白色纸飞机
        GradientDrawable sendBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#6FC6F7"), Color.parseColor("#2A7FD4")});
        sendBg.setShape(GradientDrawable.OVAL);

        sendBtn = new LinearLayout(c);
        ((LinearLayout) sendBtn).setOrientation(LinearLayout.VERTICAL);
        sendBtn.setBackground(sendBg);
        sendBtn.setGravity(Gravity.CENTER);
        sendBtn.setOnClickListener(v -> sendMessage());
        ImageView sendIcon = new ImageView(c);
        sendIcon.setImageResource(R.drawable.ic_send);
        sendIcon.setPadding(dp(2), dp(2), dp(0), dp(0));
        ((LinearLayout) sendBtn).addView(sendIcon, new LinearLayout.LayoutParams(dp(26), dp(26)));
        bar.addView(sendBtn, new LinearLayout.LayoutParams(dp(60), dp(60)));

        return bar;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ---------------- 光球呼吸动画 ----------------

    private void startPulse() {
        android.animation.ValueAnimator anim =
            android.animation.ValueAnimator.ofFloat(1f, 1.05f, 1f);
        anim.setDuration(2600);
        anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            orb.setScaleX(f);
            orb.setScaleY(f);
        });
        anim.start();
    }

    // ---------------- 发送 / 对话 ----------------

    private void sendMessage() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return; // 只有水印时发不了
        if (listening) stopListening();

        input.setText("");
        addBubble(text, true);
        ensureChatMode();

        final String q = text;
        // 稍作停顿再回复，更像真实助手
        orb.postDelayed(() -> {
            Assistant.Reply reply = assistant.respond(q);
            addBubble(reply.text, false);
            if (reply.action != null && !assistant.runAction(reply.action)) {
                Toast.makeText(this, "没有找到可以处理该指令的应用", Toast.LENGTH_SHORT).show();
            }
        }, 550);
    }

    /** 首次对话后进入聊天模式：光球缩小置顶，消息填充中间区域。 */
    private void ensureChatMode() {
        if (chatting) return;
        chatting = true;
        chatScroll.setVisibility(View.VISIBLE);
        spacer.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(110), dp(110));
        lp.topMargin = dp(20);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        orb.setLayoutParams(lp);
    }

    private void addBubble(String text, boolean user) {
        Context c = this;
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(user ? Gravity.END : Gravity.START);

        TextView tv = new TextView(c);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTextColor(Color.WHITE);
        tv.setLineSpacing(dp(3), 1f);
        tv.setPadding(dp(16), dp(10), dp(16), dp(10));

        GradientDrawable bubble = new GradientDrawable();
        if (user) {
            bubble.setColor(Color.parseColor("#D92E7FD6"));
            bubble.setCornerRadius(dp(18));
            bubble.setStroke(Math.max(1, dp(1)), Color.parseColor("#3DFFFFFF"));
        } else {
            bubble.setColor(Color.parseColor("#40153255"));
            bubble.setCornerRadius(dp(18));
            bubble.setStroke(Math.max(1, dp(1)), Color.parseColor("#4DFFFFFF"));
        }
        tv.setBackground(bubble);

        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLp.width = Math.min(dp(300), (int) (getResources().getDisplayMetrics().widthPixels * 0.78));
        tvLp.topMargin = dp(8);
        row.addView(tv, tvLp);
        chatList.addView(row);
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    // ---------------- 语音识别（点击 Siri 光球） ----------------

    private void onOrbClicked() {
        if (listening) { stopListening(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "此设备没有可用的语音识别服务", Toast.LENGTH_SHORT).show();
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onResults(android.os.Bundle results) {
                ArrayList<String> list = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null && !list.isEmpty()) {
                    // 语音内容传送到输入框
                    input.setText(list.get(0));
                    input.setSelection(input.getText().length());
                }
                stopListening();
            }
            @Override public void onError(int error) {
                if (error == SpeechRecognizer.ERROR_NO_MATCH
                    || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Toast.makeText(MainActivity.this, "没听清，再试一次？", Toast.LENGTH_SHORT).show();
                } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    Toast.makeText(MainActivity.this, "需要麦克风权限", Toast.LENGTH_SHORT).show();
                }
                stopListening();
            }
            @Override public void onReadyForSpeech(android.os.Bundle params) {
                input.setHint("正在聆听…说点什么吧");
                orb.animate().alpha(0.7f).setDuration(300).start();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(android.os.Bundle partialResults) {}
            @Override public void onEvent(int eventType, android.os.Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault().toString());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizer.startListening(intent);
        listening = true;
    }

    private void stopListening() {
        listening = false;
        input.setHint(R.string.hint); // 恢复 Ask Siri 水印
        orb.animate().alpha(1f).setDuration(300).start();
        if (recognizer != null) {
            try { recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "未授予麦克风权限，无法使用语音输入", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        super.onDestroy();
    }
}
