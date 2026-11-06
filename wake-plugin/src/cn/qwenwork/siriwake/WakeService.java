package cn.qwenwork.siriwake;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * 常驻后台的唤醒服务：
 * 前台服务持续调用系统语音识别，听到「嘿 Siri / Hi Siri」等唤醒词后，
 * 拉起 Siri 助手主应用（cn.qwenwork.siriassistant），把语音文本通过
 * wake_query 传给主页面输入框自动发送，回答直接显示在主页面。
 */
public class WakeService extends Service {

    public static volatile boolean running = false;

    private static final String CHANNEL_ID = "siri_wake";
    private static final String MAIN_PKG = "cn.qwenwork.siriassistant";
    private static final String MAIN_ACTIVITY = "cn.qwenwork.siriassistant.MainActivity";
    private static final long RESUME_DELAY_MS = 1500; // 留给主应用使用麦克风的间隙

    private SpeechRecognizer recognizer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ("stop".equals(intent != null ? intent.getAction() : null)) {
            stopListening();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            running = false;
            return START_NOT_STICKY;
        }

        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(1, n);
        }
        running = true;
        startListening();
        return START_STICKY;
    }

    // ---------------- 聆听循环 ----------------

    private void startListening() {
        if (!running) return;
        destroyRecognizer();

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "此设备没有可用的语音识别服务，唤醒插件已停止", Toast.LENGTH_LONG).show();
            stopSelf();
            running = false;
            return;
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> list = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (list == null || list.isEmpty()) ? "" : list.get(0);
                if (containsWakeWord(text)) {
                    onWake(text);
                } else {
                    restart(200); // 继续聆听
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> list = partialResults
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (list == null || list.isEmpty()) ? "" : list.get(0);
                if (containsWakeWord(text)) {
                    onWake(text); // 一听到唤醒词立即拉起，抢响应速度
                }
            }

            @Override
            public void onError(int error) {
                // ERROR_RECOGNIZER_BUSY: 主应用可能正在用麦克风，稍等再试
                restart(error == android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1200 : 350);
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizer.startListening(intent);
    }

    private void restart(long delayMs) {
        handler.postDelayed(() -> {
            if (running) startListening();
        }, delayMs);
    }

    private boolean containsWakeWord(String s) {
        if (s == null) return false;
        return s.toLowerCase(java.util.Locale.ROOT).contains("siri");
    }

    /** 命中唤醒词：拉起主应用并传递语音文本。 */
    private void onWake(String text) {
        destroyRecognizer(); // 先释放麦克风，避免与主应用冲突
        Intent it = new Intent();
        it.setComponent(new ComponentName(MAIN_PKG, MAIN_ACTIVITY));
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        it.putExtra("wake_query", text);
        try {
            startActivity(it);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "未安装 Siri 助手主应用，无法唤醒", Toast.LENGTH_LONG).show();
        }
        restart(RESUME_DELAY_MS); // 主应用显示完成后继续值守
    }

    private void destroyRecognizer() {
        if (recognizer != null) {
            try { recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
        }
    }

    private void stopListening() {
        handler.removeCallbacksAndMessages(null);
        destroyRecognizer();
    }

    @Override
    public void onDestroy() {
        running = false;
        stopListening();
        super.onDestroy();
    }

    // ---------------- 通知 ----------------

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID, "Siri 唤醒值守", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("后台聆听「嘿 Siri」唤醒词");
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
            this, 0, open, PendingIntent.FLAG_IMMUTABLE);

        Intent stop = new Intent(this, WakeService.class).setAction("stop");
        PendingIntent ps = PendingIntent.getService(
            this, 1, stop, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);
        b.setContentTitle("Siri 唤醒插件")
            .setContentText("正在聆听「嘿 Siri」…点按打开助手，下滑通知可停止")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(Notification.PRIORITY_LOW)
            .addAction(new Notification.Action.Builder(
                null, "停止值守", ps).build());
        return b.build();
    }
}
