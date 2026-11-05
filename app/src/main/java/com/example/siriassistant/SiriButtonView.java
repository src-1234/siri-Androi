package com.example.siriassistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Siri 风格多彩球按钮：多层渐变圆 + 高光 + 环绕彩色小点
 * 点击触发语音识别
 */
public class SiriButtonView extends View {

    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int[] COLORS = {
            0xFF5AC8FA, 0xFF34C759, 0xFFFFCC00, 0xFFFF9500,
            0xFFFF2D55, 0xFFAF52DE, 0xFF00C7BE
    };

    private float radius;

    public SiriButtonView(Context context) {
        super(context);
        init();
    }

    public SiriButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        radius = Math.min(w, h) * 0.30f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        // 1. 外层光晕
        glowPaint.setShader(new RadialGradient(cx, cy, radius * 1.9f,
                new int[]{0x664DC8FF, 0x00FFFFFF}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 1.9f, glowPaint);

        // 2. 主球体：多彩渐变
        ballPaint.setShader(new LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius,
                new int[]{0xFF5AC8FA, 0xFFAF52DE, 0xFFFF2D55, 0xFFFFCC00, 0xFF34C759},
                new float[]{0f, 0.3f, 0.55f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, ballPaint);

        // 3. 顶部高光
        Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlight.setShader(new RadialGradient(cx - radius * 0.3f, cy - radius * 0.35f,
                radius * 1.1f, new int[]{0xCCFFFFFF, 0x00FFFFFF},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 0.92f, highlight);

        // 4. 周围环绕彩色小点（Siri 特色）
        int dotCount = 7;
        for (int i = 0; i < dotCount; i++) {
            double angle = Math.toRadians(360.0 * i / dotCount);
            float d = radius * 1.55f;
            float dx = cx + (float) Math.cos(angle) * d;
            float dy = cy + (float) Math.sin(angle) * d;
            dotPaint.setColor(COLORS[i % COLORS.length]);
            canvas.drawCircle(dx, dy, radius * 0.10f, dotPaint);
        }
    }
}
