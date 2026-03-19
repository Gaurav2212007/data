package com.databottle;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Custom view that draws an animated water bottle.
 * Water level reflects real mobile data remaining (0–100%).
 * Color: green → yellow → red as data runs out.
 */
public class BottleView extends View {

    // ── State ──────────────────────────────────────────────────────────
    private float fillPercent = 1.0f;   // 1.0 = full, 0.0 = empty
    private float waveOffset  = 0f;
    private float targetFill  = 1.0f;
    private float animFill    = 1.0f;

    // ── Paints ─────────────────────────────────────────────────────────
    private final Paint bottleOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint capPaint           = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterPaint         = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint          = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubblePaint        = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint         = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Animators ──────────────────────────────────────────────────────
    private ValueAnimator waveAnimator;
    private ValueAnimator fillAnimator;

    // ── Bubbles ────────────────────────────────────────────────────────
    private final float[] bubbleX = {0.3f, 0.6f, 0.45f};
    private final float[] bubbleY = {0.7f, 0.55f, 0.8f};
    private final float[] bubbleR = {0.025f, 0.018f, 0.015f};
    private float[] bubbleAlpha   = {0f, 0f, 0f};
    private float[] bubbleProgress = {0.2f, 0.7f, 0.45f};

    public BottleView(Context context) {
        super(context);
        init();
    }
    public BottleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bottleOutlinePaint.setStyle(Paint.Style.STROKE);
        bottleOutlinePaint.setStrokeWidth(3f);
        bottleOutlinePaint.setColor(Color.argb(180, 255, 255, 255));

        capPaint.setStyle(Paint.Style.FILL);
        capPaint.setColor(Color.argb(200, 255, 255, 255));

        waterPaint.setStyle(Paint.Style.FILL);

        wavePaint.setStyle(Paint.Style.FILL);
        wavePaint.setColor(Color.argb(50, 255, 255, 255));

        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setColor(Color.argb(180, 255, 255, 255));

        shinePaint.setStyle(Paint.Style.FILL);
        shinePaint.setColor(Color.argb(30, 255, 255, 255));

        // Wave animation
        waveAnimator = ValueAnimator.ofFloat(0f, (float)(Math.PI * 2));
        waveAnimator.setDuration(2000);
        waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator.setInterpolator(new LinearInterpolator());
        waveAnimator.addUpdateListener(a -> {
            waveOffset = (float) a.getAnimatedValue();
            // animate bubbles
            for (int i = 0; i < bubbleProgress.length; i++) {
                bubbleProgress[i] += 0.004f * (i + 1);
                if (bubbleProgress[i] > 1f) bubbleProgress[i] = 0f;
                float inWater = 1f - bubbleY[i];
                float waterFraction = 1f - animFill;
                if (bubbleY[i] > waterFraction) {
                    bubbleAlpha[i] = (float) Math.sin(bubbleProgress[i] * Math.PI);
                } else {
                    bubbleAlpha[i] = 0f;
                }
            }
            invalidate();
        });
        waveAnimator.start();
    }

    /** Set data remaining fraction (0.0 = empty, 1.0 = full). Animates smoothly. */
    public void setFillFraction(float fraction) {
        targetFill = Math.max(0f, Math.min(1f, fraction));
        if (fillAnimator != null) fillAnimator.cancel();
        fillAnimator = ValueAnimator.ofFloat(animFill, targetFill);
        fillAnimator.setDuration(800);
        fillAnimator.setInterpolator(new LinearInterpolator());
        fillAnimator.addUpdateListener(a -> {
            animFill = (float) a.getAnimatedValue();
        });
        fillAnimator.start();
    }

    private int waterColor() {
        // green → yellow → red
        float f = animFill;
        int r, g, b;
        if (f > 0.5f) {
            float t = (f - 0.5f) / 0.5f;
            r = (int)(255 * (1f - t));
            g = 210;
            b = 50;
        } else {
            float t = f / 0.5f;
            r = 255;
            g = (int)(180 * t);
            b = 20;
        }
        return Color.argb(230, r, g, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        float cx = w / 2f;

        // Bottle geometry (proportional)
        float capH     = h * 0.07f;
        float neckTop  = h * 0.04f;
        float neckBot  = h * 0.22f;
        float neckW    = w * 0.32f;
        float bodyTop  = h * 0.22f;
        float bodyBot  = h * 0.96f;
        float bodyW    = w * 0.78f;
        float shoulder = h * 0.10f;  // shoulder curve height

        // ── Build bottle body path ──────────────────────────────────────
        Path bodyPath = new Path();
        bodyPath.moveTo(cx - neckW / 2, neckBot);
        bodyPath.cubicTo(
                cx - neckW / 2, neckBot + shoulder,
                cx - bodyW / 2, bodyTop + shoulder,
                cx - bodyW / 2, bodyTop + shoulder * 1.5f);
        bodyPath.lineTo(cx - bodyW / 2, bodyBot - w * 0.1f);
        bodyPath.arcTo(new RectF(cx - bodyW / 2, bodyBot - w * 0.2f,
                cx + bodyW / 2, bodyBot), 180, -180, false);
        bodyPath.lineTo(cx + bodyW / 2, bodyTop + shoulder * 1.5f);
        bodyPath.cubicTo(
                cx + bodyW / 2, bodyTop + shoulder,
                cx + neckW / 2, neckBot + shoulder,
                cx + neckW / 2, neckBot);
        bodyPath.close();

        // ── Clip region for water ──────────────────────────────────────
        float waterTop = bodyBot - (bodyBot - neckBot) * animFill;

        canvas.save();
        canvas.clipPath(bodyPath);

        // Fill water
        waterPaint.setColor(waterColor());
        canvas.drawRect(cx - bodyW / 2, waterTop, cx + bodyW / 2, bodyBot, waterPaint);

        // Draw wave on water surface
        Path wavePath = new Path();
        float waveAmp = h * 0.015f;
        wavePath.moveTo(0, waterTop);
        int steps = 40;
        for (int i = 0; i <= steps; i++) {
            float x = cx - bodyW / 2 + (bodyW * i / steps);
            float y = waterTop + (float)(waveAmp * Math.sin((i / (float)steps) * Math.PI * 4 + waveOffset));
            if (i == 0) wavePath.moveTo(x, y);
            else wavePath.lineTo(x, y);
        }
        wavePath.lineTo(cx + bodyW / 2, waterTop - waveAmp * 2);
        wavePath.lineTo(cx - bodyW / 2, waterTop - waveAmp * 2);
        wavePath.close();
        canvas.drawPath(wavePath, wavePaint);

        // Draw bubbles
        for (int i = 0; i < bubbleX.length; i++) {
            if (bubbleAlpha[i] > 0.01f) {
                float bx = cx - bodyW / 2 + bodyW * bubbleX[i];
                float by = bodyBot - (bodyBot - neckBot) * bubbleY[i];
                float br = w * bubbleR[i];
                bubblePaint.setAlpha((int)(bubbleAlpha[i] * 200));
                canvas.drawCircle(bx, by, br, bubblePaint);
            }
        }

        canvas.restore();

        // ── Draw bottle outline on top ─────────────────────────────────
        canvas.drawPath(bodyPath, bottleOutlinePaint);

        // ── Neck ────────────────────────────────────────────────────────
        RectF neck = new RectF(cx - neckW / 2, neckTop, cx + neckW / 2, neckBot);
        canvas.drawRoundRect(neck, 6, 6, bottleOutlinePaint);
        // neck fill (same as water if water is above neck)
        if (animFill > 0.95f) {
            waterPaint.setColor(waterColor());
            canvas.drawRoundRect(neck, 6, 6, waterPaint);
        }

        // ── Cap ─────────────────────────────────────────────────────────
        RectF cap = new RectF(cx - neckW / 2 - 3, neckTop - 3, cx + neckW / 2 + 3, neckTop + capH);
        canvas.drawRoundRect(cap, 5, 5, capPaint);

        // ── Shine stripe ────────────────────────────────────────────────
        RectF shine = new RectF(cx - bodyW / 2 + 4, bodyTop + shoulder * 2, cx - bodyW / 2 + 4 + w * 0.08f, bodyBot - h * 0.06f);
        canvas.drawRoundRect(shine, 4, 4, shinePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (waveAnimator != null) waveAnimator.cancel();
        if (fillAnimator != null) fillAnimator.cancel();
    }
}
