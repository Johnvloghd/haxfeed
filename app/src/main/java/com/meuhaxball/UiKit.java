package com.meuhaxball;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Pequenos helpers para montar a interface (tema escuro, cantos arredondados). */
public final class UiKit {

    private UiKit() {}

    public static final int CARD = 0xF2141A24;
    public static final int SURFACE = 0xFF263143;
    public static final int ACCENT = 0xFF4F8CFF;
    public static final int TEXT = 0xFFE8EDF5;
    public static final int MUTED = 0xFF93A4BC;

    public interface IntListener {
        void onValue(int value);
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        GradientDrawable g = rounded(c, CARD, 18);
        g.setStroke(dp(c, 1), 0x33FFFFFF);
        l.setBackground(g);
        l.setClickable(true); // não deixa o toque "vazar" para o jogo
        return l;
    }

    public static TextView label(Context c, String text, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static TextView pill(Context c, String text, boolean accent, View.OnClickListener l) {
        TextView t = label(c, text, 14, accent ? 0xFFFFFFFF : TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(c, 44)); // alvo de toque confortável
        int ph = dp(c, 12);
        t.setPadding(ph, 0, ph, 0);
        t.setBackground(rounded(c, accent ? ACCENT : SURFACE, 22));
        t.setOnClickListener(l);
        return t;
    }

    public static SeekBar slider(Context c, int max, int progress, IntListener l) {
        SeekBar sb = new SeekBar(c);
        sb.setMax(max);
        sb.setProgress(progress);
        sb.setProgressTintList(ColorStateList.valueOf(ACCENT));
        sb.setThumbTintList(ColorStateList.valueOf(ACCENT));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) l.onValue(p);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {}
        });
        return sb;
    }
}
