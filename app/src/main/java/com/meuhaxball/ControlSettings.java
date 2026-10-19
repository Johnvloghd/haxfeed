package com.meuhaxball;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferências do app: layout dos controles touch e opções do WebView. */
public class ControlSettings {

    public static final int JOY = 0, KICK = 1, CHAT = 2, MENU = 3;

    private final SharedPreferences sp;

    public int joySizeDp;
    public int btnSizeDp;
    public int alphaPercent;
    public boolean controlsVisible;
    public boolean desktopMode;
    public boolean lockScroll;

    /** Centro de cada controle como fração (0..1) da área útil da tela. */
    public final float[] cx = new float[4];
    public final float[] cy = new float[4];

    public ControlSettings(Context c) {
        sp = c.getSharedPreferences("controls", Context.MODE_PRIVATE);
        load();
    }

    public void resetLayout() {
        joySizeDp = 150;
        btnSizeDp = 84;
        alphaPercent = 60;
        cx[JOY] = 0.14f;  cy[JOY] = 0.74f;
        cx[KICK] = 0.88f; cy[KICK] = 0.72f;
        cx[CHAT] = 0.77f; cy[CHAT] = 0.88f;
        cx[MENU] = 0.96f; cy[MENU] = 0.08f;
    }

    private void load() {
        resetLayout();
        joySizeDp = sp.getInt("joySizeDp", joySizeDp);
        btnSizeDp = sp.getInt("btnSizeDp", btnSizeDp);
        alphaPercent = sp.getInt("alphaPercent", alphaPercent);
        for (int i = 0; i < 4; i++) {
            cx[i] = sp.getFloat("cx" + i, cx[i]);
            cy[i] = sp.getFloat("cy" + i, cy[i]);
        }
        controlsVisible = sp.getBoolean("controlsVisible", true);
        desktopMode = sp.getBoolean("desktopMode", false);
        lockScroll = sp.getBoolean("lockScroll", true);
    }

    public void save() {
        SharedPreferences.Editor e = sp.edit();
        e.putInt("joySizeDp", joySizeDp);
        e.putInt("btnSizeDp", btnSizeDp);
        e.putInt("alphaPercent", alphaPercent);
        for (int i = 0; i < 4; i++) {
            e.putFloat("cx" + i, cx[i]);
            e.putFloat("cy" + i, cy[i]);
        }
        e.putBoolean("controlsVisible", controlsVisible);
        e.putBoolean("desktopMode", desktopMode);
        e.putBoolean("lockScroll", lockScroll);
        e.apply();
    }
}
