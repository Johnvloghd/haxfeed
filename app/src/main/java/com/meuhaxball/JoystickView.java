package com.meuhaxball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Joystick virtual de 8 direções. A cada toque informa quais das 4 teclas de
 * direção devem estar pressionadas (diagonal = duas teclas ao mesmo tempo).
 */
public class JoystickView extends View {

    public interface Listener {
        void onDirection(boolean left, boolean right, boolean up, boolean down);
    }

    private static final float DEADZONE = 0.28f;

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float thumbX, thumbY;
    private boolean touching;
    private Listener listener;

    public JoystickView(Context context) {
        super(context);
        basePaint.setColor(0x40FFFFFF);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(UiKit.dp(context, 2));
        ringPaint.setColor(0x80FFFFFF);
        thumbPaint.setColor(0xB3FFFFFF);
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - ringPaint.getStrokeWidth();
        canvas.drawCircle(cx, cy, radius, basePaint);
        canvas.drawCircle(cx, cy, radius, ringPaint);
        float tx = touching ? thumbX : cx;
        float ty = touching ? thumbY : cy;
        canvas.drawCircle(tx, ty, radius * 0.38f, thumbPaint);
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
                float limited = Math.min(dist, radius);
                if (dist > 0f) {
                    thumbX = cx + dx / dist * limited;
                    thumbY = cy + dy / dist * limited;
                } else {
                    thumbX = cx;
                    thumbY = cy;
                }
                if (dist / radius < DEADZONE) {
                    emit(false, false, false, false);
                } else {
                    // 8 setores de 45 graus: 0=direita, 1=baixo-dir., 2=baixo, 3=baixo-esq.,
                    // 4=esquerda, 5=cima-esq., 6=cima, 7=cima-dir. (eixo y cresce para baixo)
                    int sector = (int) Math.round(Math.atan2(dy, dx) / (Math.PI / 4));
                    if (sector < 0) sector += 8;
                    if (sector == 8) sector = 0;
                    boolean right = sector == 7 || sector == 0 || sector == 1;
                    boolean down = sector == 1 || sector == 2 || sector == 3;
                    boolean left = sector == 3 || sector == 4 || sector == 5;
                    boolean up = sector == 5 || sector == 6 || sector == 7;
                    emit(left, right, up, down);
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touching = false;
                emit(false, false, false, false);
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private void emit(boolean l, boolean r, boolean u, boolean d) {
        if (listener != null) listener.onDirection(l, r, u, d);
    }
}
