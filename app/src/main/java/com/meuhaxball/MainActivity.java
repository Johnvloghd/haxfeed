package com.meuhaxball;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Cliente Android do Haxball: abre o site oficial num WebView e
 * adiciona controles virtuais (joystick + botão de chute) que enviam
 * teclas (setas e X) para a página. Não modifica o código do jogo.
 */
public class MainActivity extends Activity {

    private static final String URL = "https://www.haxball.com/play";

    private WebView webView;
    private boolean left, right, up, down;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int joySize = dp(150);
        JoystickView joystick = new JoystickView(this);
        FrameLayout.LayoutParams jlp = new FrameLayout.LayoutParams(joySize, joySize,
                Gravity.BOTTOM | Gravity.START);
        jlp.setMargins(dp(24), 0, 0, dp(24));
        root.addView(joystick, jlp);
        joystick.setListener((l, r, u, d) -> {
            update(KeyEvent.KEYCODE_DPAD_LEFT, left, l);
            update(KeyEvent.KEYCODE_DPAD_RIGHT, right, r);
            update(KeyEvent.KEYCODE_DPAD_UP, up, u);
            update(KeyEvent.KEYCODE_DPAD_DOWN, down, d);
            left = l; right = r; up = u; down = d;
        });

        int kickSize = dp(90);
        TextView kick = new TextView(this);
        kick.setText("X");
        kick.setTextSize(24);
        kick.setTextColor(0xFFFFFFFF);
        kick.setGravity(Gravity.CENTER);
        kick.setBackgroundColor(0x66FF5555);
        FrameLayout.LayoutParams klp = new FrameLayout.LayoutParams(kickSize, kickSize,
                Gravity.BOTTOM | Gravity.END);
        klp.setMargins(0, 0, dp(32), dp(40));
        root.addView(kick, klp);
        kick.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                send(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X);
                return true;
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                send(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_X);
                return true;
            }
            return true;
        });

        setContentView(root);
        webView.loadUrl(URL);
    }

    private void update(int keyCode, boolean was, boolean now) {
        if (now && !was) send(KeyEvent.ACTION_DOWN, keyCode);
        else if (!now && was) send(KeyEvent.ACTION_UP, keyCode);
    }

    private void send(int action, int keyCode) {
        webView.dispatchKeyEvent(new KeyEvent(action, keyCode));
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
