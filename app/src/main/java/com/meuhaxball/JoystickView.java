package com.meuhaxball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Joystick virtual simples: informa quais das 4 direções estão ativas. */
public class JoystickView extends View {

    public interface Listener {
        void onDirection(boolean left, boolean right, boolean up, boolean down);
    }

    private static final float DEADZONE = 0.30f;

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float thumbX, thumbY;
    private boolean touching = false;
    private Listener listener;

    public JoystickView(Context context) {
        super(context);
        basePaint.setColor(0x55FFFFFF);
        thumbPaint.setColor(0xAAFFFFFF);
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f;
        canvas.drawCircle(cx, cy, radius, basePaint);
        float tx = touching ? thumbX : cx;
        float ty = touching ? thumbY : cy;
        canvas.drawCircle(tx, ty, radius * 0.4f, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f;

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                touching = true;
                float dx = e.getX() - cx;
                float dy = e.getY() - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > radius) {
                    dx = dx / dist * radius;
                    dy = dy / dist * radius;
                }
                thumbX = cx + dx;
                thumbY = cy + dy;
                float nx = dx / radius;
                float ny = dy / radius;
                if (listener != null) {
                    listener.onDirection(nx < -DEADZONE, nx > DEADZONE, ny < -DEADZONE, ny > DEADZONE);
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touching = false;
                if (listener != null) listener.onDirection(false, false, false, false);
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }
}
