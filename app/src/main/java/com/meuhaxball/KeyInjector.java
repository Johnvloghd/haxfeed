package com.meuhaxball;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Envia teclas nativas do Android para o WebView, como se viessem de um teclado.
 * Guarda quais teclas estão pressionadas para não repetir eventos e para poder
 * soltar tudo de uma vez (evita personagem "preso" andando sozinho).
 */
public class KeyInjector {

    private WebView webView;
    private final HashSet<Integer> held = new HashSet<>();

    public KeyInjector(WebView webView) {
        this.webView = webView;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
        held.clear();
    }

    public void press(int keyCode) {
        if (held.add(keyCode)) send(KeyEvent.ACTION_DOWN, keyCode);
    }

    public void release(int keyCode) {
        if (held.remove(keyCode)) send(KeyEvent.ACTION_UP, keyCode);
    }

    public void set(int keyCode, boolean down) {
        if (down) press(keyCode);
        else release(keyCode);
    }

    public void releaseAll() {
        for (Integer code : new ArrayList<>(held)) release(code);
    }

    private void send(int action, int keyCode) {
        if (webView == null) return;
        if (!webView.hasFocus()) webView.requestFocus();
        long now = SystemClock.uptimeMillis();
        KeyEvent e = new KeyEvent(now, now, action, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
        webView.dispatchKeyEvent(e);
    }
}
