package com.example.siriassistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * 波形动画视图：多条彩色竖条上下波动，模拟语音波形
 * start() 启动动画，stop() 停止
 */
public class WaveView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private static final int BAR_COUNT = 17;
    private static final int[] COLORS = {
            0xFF5AC8FA, 0xFF34C759, 0xFFFFCC00, 0xFFFF9500,
            0xFFFF2D55, 0xFFAF52DE, 0xFF00C7BE
    };

    private float[] heights = new float[BAR_COUNT];
    private float[] speeds = new float[BAR_COUNT];
    private boolean running = false;
    private long lastTick = 0;

    public WaveView(Context context) {
        super(context);
        init();
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        for (int i = 0; i < BAR_COUNT; i++) {
            heights[i] = 0.3f + random.nextFloat() * 0.3f;
            speeds[i] = 0.03f + random.nextFloat() * 0.05f;
        }
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    /** 开始波动动画 */
    public void start() {
        if (!running) {
            running = true;
            lastTick = System.currentTimeMillis();
            postInvalidateOnAnimation();
        }
    }

    /** 停止动画并归零 */
    public void stop() {
        running = false;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float mid = h / 2f;
        float barW = w / (BAR_COUNT * 1.8f);
        float gap = (w - barW * BAR_COUNT) / (BAR_COUNT + 1);

        for (int i = 0; i < BAR_COUNT; i++) {
            float amp = running ? heights[i] : 0.08f;
            float barH = h * 0.85f * amp;
            paint.setColor(COLORS[i % COLORS.length]);
            float x = gap + i * (barW + gap);
            canvas.drawRoundRect(x, mid - barH / 2, x + barW, mid + barH / 2,
                    barW / 2, barW / 2, paint);
        }

        if (running) {
            long now = System.currentTimeMillis();
            float dt = (now - lastTick) / 1000f;
            lastTick = now;
            for (int i = 0; i < BAR_COUNT; i++) {
                heights[i] += (random.nextFloat() - 0.5f) * speeds[i] * dt * 20f;
                heights[i] = Math.max(0.15f, Math.min(1.0f, heights[i]));
            }
            postInvalidateOnAnimation();
        }
    }
}
