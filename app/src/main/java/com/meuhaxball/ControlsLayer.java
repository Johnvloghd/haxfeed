package com.meuhaxball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Camada transparente por cima do jogo com joystick, botão de chute, botão de chat
 * e botão de menu. Fora dos controles, o toque passa direto para o WebView.
 * No modo de edição os controles podem ser arrastados e o layout é salvo.
 */
public class ControlsLayer extends FrameLayout {

    private final ControlSettings s;
    public final JoystickView joystick;
    public final ActionButton kick;
    public final ActionButton chat;
    public final ActionButton menu;
    private final View[] ctl = new View[4];
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean editMode;
    private boolean keyboardOpen;
    private int dragIndex = -1;
    private float offX, offY;

    public ControlsLayer(Context c, ControlSettings settings) {
        super(c);
        s = settings;
        joystick = new JoystickView(c);
        kick = new ActionButton(c, "X", 0xFFFF5A5A);
        chat = new ActionButton(c, "Chat", 0xFF4F8CFF);
        menu = new ActionButton(c, "☰", 0xFFFFFFFF);
        ctl[ControlSettings.JOY] = joystick;
        ctl[ControlSettings.KICK] = kick;
        ctl[ControlSettings.CHAT] = chat;
        ctl[ControlSettings.MENU] = menu;
        for (View v : ctl) addView(v, new LayoutParams(UiKit.dp(c, 48), UiKit.dp(c, 48)));

        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(UiKit.dp(c, 2));
        outline.setColor(0xCCFFFFFF);
        outline.setPathEffect(new DashPathEffect(
                new float[]{UiKit.dp(c, 8), UiKit.dp(c, 6)}, 0));
        setWillNotDraw(false);
        applySettings();
    }

    /** Reaplica tamanho, transparência e visibilidade conforme as configurações. */
    public void applySettings() {
        float a = s.alphaPercent / 100f;
        boolean showGameControls = (s.controlsVisible || editMode) && !keyboardOpen;
        for (int i = 0; i < 3; i++) {
            ctl[i].setVisibility(showGameControls ? VISIBLE : GONE);
            ctl[i].setAlpha(a);
        }
        ctl[ControlSettings.MENU].setVisibility(keyboardOpen && !editMode ? GONE : VISIBLE);
        ctl[ControlSettings.MENU].setAlpha(0.55f);
        requestLayout();
        invalidate();
    }

    public void setEditMode(boolean on) {
        editMode = on;
        dragIndex = -1;
        applySettings();
    }

    /** Esconde os controles enquanto o teclado do chat está aberto. */
    public void setKeyboardOpen(boolean open) {
        if (keyboardOpen == open) return;
        keyboardOpen = open;
        applySettings();
    }

    private int sizePx(int i) {
        Context c = getContext();
        switch (i) {
            case ControlSettings.JOY:
                return UiKit.dp(c, s.joySizeDp);
            case ControlSettings.KICK:
                return UiKit.dp(c, s.btnSizeDp);
            case ControlSettings.CHAT:
                return UiKit.dp(c, s.btnSizeDp * 0.6f);
            default:
                return UiKit.dp(c, 40);
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), MeasureSpec.getSize(heightSpec));
        for (int i = 0; i < ctl.length; i++) {
            if (ctl[i].getVisibility() == GONE) continue;
            int spec = MeasureSpec.makeMeasureSpec(sizePx(i), MeasureSpec.EXACTLY);
            ctl[i].measure(spec, spec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        for (int i = 0; i < ctl.length; i++) {
            View v = ctl[i];
            if (v.getVisibility() == GONE) continue;
            int px = v.getMeasuredWidth();
            int x = Math.round(s.cx[i] * w) - px / 2;
            int y = Math.round(s.cy[i] * h) - px / 2;
            x = Math.max(0, Math.min(x, Math.max(0, w - px)));
            y = Math.max(0, Math.min(y, Math.max(0, h - px)));
            v.layout(x, y, x + px, y + px);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!editMode) return;
        float pad = UiKit.dp(getContext(), 4);
        for (View v : ctl) {
            if (v.getVisibility() != VISIBLE) continue;
            float rad = v.getWidth() / 2f;
            canvas.drawCircle(v.getLeft() + rad, v.getTop() + rad, rad + pad, outline);
        }
    }

    // ---- modo de edição: arrastar controles ----

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return editMode;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!editMode) return false; // fora dos controles, o toque vai para o jogo
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragIndex = findControl(x, y);
                if (dragIndex >= 0) {
                    View v = ctl[dragIndex];
                    offX = x - (v.getLeft() + v.getWidth() / 2f);
                    offY = y - (v.getTop() + v.getHeight() / 2f);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragIndex >= 0) moveTo(dragIndex, x - offX, y - offY);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragIndex >= 0) s.save();
                dragIndex = -1;
                return true;
            default:
                return true;
        }
    }

    private int findControl(float x, float y) {
        float slop = UiKit.dp(getContext(), 8);
        for (int i = ctl.length - 1; i >= 0; i--) {
            View v = ctl[i];
            if (v.getVisibility() != VISIBLE) continue;
            if (x >= v.getLeft() - slop && x <= v.getRight() + slop
                    && y >= v.getTop() - slop && y <= v.getBottom() + slop) {
                return i;
            }
        }
        return -1;
    }

    private void moveTo(int i, float centerX, float centerY) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        s.cx[i] = Math.max(0f, Math.min(1f, centerX / w));
        s.cy[i] = Math.max(0f, Math.min(1f, centerY / h));
        requestLayout();
        invalidate();
    }
}
