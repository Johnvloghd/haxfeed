package com.meuhaxball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/** Botão circular de toque que informa quando foi pressionado e solto. */
public class ActionButton extends View {

    public interface Listener {
        void onPress();

        void onRelease();
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String label;
    private final int color;
    private boolean pressed;
    private Listener listener;

    public ActionButton(Context c, String label, int color) {
        super(c);
        this.label = label;
        this.color = color;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(UiKit.dp(c, 2));
        ring.setColor(0xB3FFFFFF);
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        setHapticFeedbackEnabled(true);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    /** Evita uma camada offscreen a cada frame quando a transparência é aplicada. */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f - ring.getStrokeWidth();
        fill.setColor(color);
        fill.setAlpha(pressed ? 0xCC : 0x66);
        canvas.drawCircle(cx, cy, r, fill);
        canvas.drawCircle(cx, cy, r, ring);
        float size = Math.min(getWidth(), getHeight()) * (label.length() > 2 ? 0.26f : 0.42f);
        text.setTextSize(size);
        float baseline = cy - (text.descent() + text.ascent()) / 2f;
        canvas.drawText(label, cx, baseline, text);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                invalidate();
                if (listener != null) listener.onPress();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (pressed) {
                    pressed = false;
                    invalidate();
                    if (listener != null) listener.onRelease();
                }
                return true;
            default:
                return true;
        }
    }
}
