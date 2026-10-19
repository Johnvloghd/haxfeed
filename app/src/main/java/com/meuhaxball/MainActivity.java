package com.meuhaxball;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Cliente Android do Haxball. Abre o site oficial (salas online, multiplayer, chat,
 * times e placar continuam sendo os do jogo original) num WebView e adiciona:
 * controles touch personalizáveis, menu, tratamento de tela cheia/notch/teclado,
 * volume de mídia e recuperação de conexão. Não altera o código do jogo.
 */
public class MainActivity extends Activity {

    private static final String HOME = "https://www.haxball.com/play";

    /** Só garante um viewport 1:1 sem zoom; não mexe em nada do jogo. */
    private static final String VIEWPORT_JS =
            "(function(){try{var m=document.querySelector('meta[name=viewport]');"
                    + "if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}"
                    + "m.content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover';"
                    + "}catch(e){}})();";

    private FrameLayout root;
    private FrameLayout content;
    private WebView webView;
    private KeyInjector keys;
    private ControlSettings settings;
    private ControlsLayer controls;
    private View menuPanel;
    private View editPanel;
    private View errorOverlay;
    private TextView errorText;
    private TextView btnControls;
    private TextView btnDesktop;
    private TextView btnLock;
    private SeekBar volumeBar;
    private SeekBar sbJoy;
    private SeekBar sbBtn;
    private SeekBar sbAlpha;
    private AudioManager audio;
    private ConnectivityManager.NetworkCallback netCallback;

    private String defaultUA;
    private String lastUrl = HOME;
    private boolean pageFailed;
    private boolean editMode;
    private boolean menuOpen;

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = new ControlSettings(this);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC); // botões de volume mexem no som do jogo
        setupWindow();

        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF1A212C);
        content = new FrameLayout(this);
        content.setMotionEventSplittingEnabled(true); // joystick + chute ao mesmo tempo
        root.setMotionEventSplittingEnabled(true);
        root.addView(content, matchParent());

        setupWebView();
        content.addView(webView, matchParent());
        keys = new KeyInjector(webView);

        errorOverlay = buildErrorOverlay();
        content.addView(errorOverlay, matchParent());

        controls = new ControlsLayer(this, settings);
        content.addView(controls, matchParent());
        wireControls();

        editPanel = buildEditPanel();
        content.addView(editPanel, editPanelParams());
        editPanel.setVisibility(View.GONE);

        menuPanel = buildMenuPanel();
        content.addView(menuPanel, matchParent());
        menuPanel.setVisibility(View.GONE);

        setupInsets();
        setupKeyboardHeuristic();
        registerNetworkCallback();

        setContentView(root);
        hideSystemBars();

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(HOME);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        webView.onResume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
    }

    @Override
    protected void onStop() {
        keys.releaseAll();
        webView.onPause(); // economiza bateria/memória quando o app sai da tela
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            if (netCallback != null) {
                ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
                        .unregisterNetworkCallback(netCallback);
            }
        } catch (Exception ignored) {
        }
        content.removeView(webView);
        webView.destroy();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    public void onBackPressed() {
        // Voltar não pode sair da sala sem querer: abre/fecha o menu.
        if (editMode) exitEditMode();
        else if (menuOpen) closeMenu();
        else openMenu();
    }

    // ------------------------------------------------------------------ janela e tela

    private void setupWindow() {
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false); // nós tratamos notch, barras e teclado
        } else {
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(lp);
        }
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    /** Mantém o jogo dentro da área segura (notch, cantos) e acima do teclado. */
    @SuppressWarnings("deprecation")
    private void setupInsets() {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                l = bars.left;
                t = bars.top;
                r = bars.right;
                b = Math.max(bars.bottom, ime.bottom);
                setKeyboardOpen(insets.isVisible(WindowInsets.Type.ime()));
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = 0;
                if (Build.VERSION.SDK_INT >= 28) {
                    DisplayCutout dc = insets.getDisplayCutout();
                    if (dc != null) {
                        l = Math.max(l, dc.getSafeInsetLeft());
                        t = Math.max(t, dc.getSafeInsetTop());
                        r = Math.max(r, dc.getSafeInsetRight());
                        b = Math.max(b, dc.getSafeInsetBottom());
                    }
                }
            }
            content.setPadding(l, t, r, b);
            return insets;
        });
        root.requestApplyInsets();
    }

    /** Android &lt; 11: detecta o teclado pela área visível da janela. */
    private void setupKeyboardHeuristic() {
        if (Build.VERSION.SDK_INT >= 30) return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            root.getWindowVisibleDisplayFrame(r);
            int hidden = root.getRootView().getHeight() - r.bottom;
            setKeyboardOpen(hidden > UiKit.dp(this, 140));
        });
    }

    private void setKeyboardOpen(boolean open) {
        if (open) keys.releaseAll();
        controls.setKeyboardOpen(open); // esconde os controles para não cobrir o chat
    }

    // ------------------------------------------------------------------ WebView

    private void setupWebView() {
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF1A212C);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true); // guarda nick e preferências do jogo
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); // sons do jogo
        s.setLoadsImagesAutomatically(true);
        s.setBlockNetworkImage(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(false);
        s.setLoadWithOverviewMode(false);
        s.setTextZoom(100);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        defaultUA = s.getUserAgentString();
        applyUserAgent();

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setHapticFeedbackEnabled(false);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        // Rolagem acidental da página: volta para o topo (não afeta rolagem do chat).
        webView.setOnScrollChangeListener((v, x, y, ox, oy) -> {
            if (settings.lockScroll && (x != 0 || y != 0)) v.scrollTo(0, 0);
        });

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);
        if (Build.VERSION.SDK_INT >= 26) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);
        }
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                if (!req.isForMainFrame()) return false;
                Uri u = req.getUrl();
                String host = u.getHost();
                if (host != null && (host.equals("haxball.com") || host.endsWith(".haxball.com"))) {
                    return false; // links de sala continuam dentro do app
                }
                String scheme = u.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, u)); // links externos no navegador
                    } catch (Exception ignored) {
                    }
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (url != null && url.startsWith("http")) lastUrl = url;
                pageFailed = false;
                hideError();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(VIEWPORT_JS, null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    showError("Não foi possível carregar o Haxball.\nVerifique sua conexão.");
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                rebuildWebView(); // o navegador interno travou/foi encerrado: recria sem fechar o app
                return true;
            }
        });
    }

    private void rebuildWebView() {
        keys.releaseAll();
        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent != null) parent.removeView(webView);
        webView.destroy();
        setupWebView();
        content.addView(webView, 0, matchParent());
        keys.setWebView(webView);
        webView.loadUrl(lastUrl);
    }

    private void applyUserAgent() {
        WebSettings s = webView.getSettings();
        if (settings.desktopMode) {
            String ua = defaultUA.replace("; wv", "").replace(" Mobile", "");
            ua = ua.replaceAll("\\(Linux; Android [^)]*\\)", "(X11; Linux x86_64)");
            s.setUserAgentString(ua);
        } else {
            s.setUserAgentString(defaultUA);
        }
    }

    // ------------------------------------------------------------------ conexão

    private void registerNetworkCallback() {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            netCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(() -> {
                        if (pageFailed) { // voltou a internet depois de uma falha: recarrega sozinho
                            hideError();
                            webView.loadUrl(lastUrl);
                        }
                    });
                }
            };
            cm.registerDefaultNetworkCallback(netCallback);
        } catch (Exception e) {
            netCallback = null;
        }
    }

    private void showError(String msg) {
        pageFailed = true;
        errorText.setText(msg);
        errorOverlay.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorOverlay.setVisibility(View.GONE);
    }

    private View buildErrorOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xFF1A212C);
        overlay.setClickable(true);
        LinearLayout card = UiKit.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = UiKit.label(this, "Sem conexão", 18, UiKit.TEXT, true);
        errorText = UiKit.label(this, "", 14, UiKit.MUTED, false);
        errorText.setGravity(Gravity.CENTER);
        errorText.setPadding(0, UiKit.dp(this, 8), 0, UiKit.dp(this, 12));
        TextView retry = UiKit.pill(this, "Tentar novamente", true, v -> {
            hideError();
            webView.loadUrl(lastUrl);
        });
        card.addView(title);
        card.addView(errorText);
        card.addView(retry);
        overlay.addView(card, new FrameLayout.LayoutParams(
                UiKit.dp(this, 320), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        overlay.setVisibility(View.GONE);
        return overlay;
    }

    // ------------------------------------------------------------------ controles touch

    private void wireControls() {
        controls.joystick.setListener((l, r, u, d) -> {
            keys.set(KeyEvent.KEYCODE_DPAD_LEFT, l);
            keys.set(KeyEvent.KEYCODE_DPAD_RIGHT, r);
            keys.set(KeyEvent.KEYCODE_DPAD_UP, u);
            keys.set(KeyEvent.KEYCODE_DPAD_DOWN, d);
        });
        controls.kick.setListener(keyListener(KeyEvent.KEYCODE_X)); // X = chutar no Haxball
        controls.chat.setListener(keyListener(KeyEvent.KEYCODE_ENTER)); // Enter = chat
        controls.menu.setListener(new ActionButton.Listener() {
            @Override
            public void onPress() {
                openMenu();
            }

            @Override
            public void onRelease() {
            }
        });
    }

    private ActionButton.Listener keyListener(final int keyCode) {
        return new ActionButton.Listener() {
            @Override
            public void onPress() {
                keys.press(keyCode);
            }

            @Override
            public void onRelease() {
                keys.release(keyCode);
            }
        };
    }

    // ------------------------------------------------------------------ menu

    private View buildMenuPanel() {
        FrameLayout scrim = new FrameLayout(this);
        scrim.setBackgroundColor(0xAA000000);
        scrim.setOnClickListener(v -> closeMenu());

        LinearLayout card = UiKit.card(this);

        LinearLayout header = row();
        TextView title = UiKit.label(this, "Meu Haxball", 18, UiKit.TEXT, true);
        header.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(UiKit.pill(this, "Fechar", false, v -> closeMenu()));
        card.addView(header);

        TextView volLabel = UiKit.label(this, "Volume do som (mídia do aparelho)", 12,
                UiKit.MUTED, false);
        volLabel.setPadding(0, UiKit.dp(this, 10), 0, 0);
        card.addView(volLabel);
        volumeBar = UiKit.slider(this,
                audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                audio.getStreamVolume(AudioManager.STREAM_MUSIC),
                v -> {
                    try {
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
                    } catch (SecurityException ignored) {
                    }
                });
        card.addView(volumeBar);

        LinearLayout r1 = row();
        addCell(r1, UiKit.pill(this, "Personalizar controles", true, v -> enterEditMode()));
        btnControls = UiKit.pill(this, "", false, v -> {
            settings.controlsVisible = !settings.controlsVisible;
            settings.save();
            keys.releaseAll();
            controls.applySettings();
            refreshMenuLabels();
        });
        addCell(r1, btnControls);
        card.addView(r1);

        LinearLayout r2 = row();
        btnDesktop = UiKit.pill(this, "", false, v -> {
            settings.desktopMode = !settings.desktopMode;
            settings.save();
            applyUserAgent();
            closeMenu();
            webView.loadUrl(lastUrl);
        });
        addCell(r2, btnDesktop);
        btnLock = UiKit.pill(this, "", false, v -> {
            settings.lockScroll = !settings.lockScroll;
            settings.save();
            refreshMenuLabels();
        });
        addCell(r2, btnLock);
        card.addView(r2);

        LinearLayout r3 = row();
        addCell(r3, UiKit.pill(this, "Recarregar / reconectar", false, v -> {
            closeMenu();
            webView.loadUrl(lastUrl);
        }));
        addCell(r3, UiKit.pill(this, "Sair do app", false, v -> finish()));
        card.addView(r3);

        ScrollView sv = new ScrollView(this);
        sv.addView(card);
        int w = Math.min(UiKit.dp(this, 460),
                getResources().getDisplayMetrics().widthPixels - UiKit.dp(this, 32));
        scrim.addView(sv, new FrameLayout.LayoutParams(
                w, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        return scrim;
    }

    private void refreshMenuLabels() {
        btnControls.setText(settings.controlsVisible ? "Controles: ligados" : "Controles: ocultos");
        btnDesktop.setText(settings.desktopMode ? "Modo desktop: ligado" : "Modo desktop: desligado");
        btnLock.setText(settings.lockScroll ? "Travar rolagem: ligado" : "Travar rolagem: desligado");
    }

    private void openMenu() {
        keys.releaseAll();
        volumeBar.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
        refreshMenuLabels();
        menuOpen = true;
        menuPanel.setVisibility(View.VISIBLE);
    }

    private void closeMenu() {
        menuOpen = false;
        menuPanel.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------ edição dos controles

    private View buildEditPanel() {
        LinearLayout card = UiKit.card(this);
        card.addView(UiKit.label(this, "Personalizar controles — arraste para mover", 13,
                UiKit.MUTED, true));

        LinearLayout sliders = new LinearLayout(this);
        sliders.setOrientation(LinearLayout.HORIZONTAL);
        sbJoy = UiKit.slider(this, 150, settings.joySizeDp - 90, v -> {
            settings.joySizeDp = 90 + v;
            controls.applySettings();
        });
        sbBtn = UiKit.slider(this, 84, settings.btnSizeDp - 56, v -> {
            settings.btnSizeDp = 56 + v;
            controls.applySettings();
        });
        sbAlpha = UiKit.slider(this, 85, settings.alphaPercent - 15, v -> {
            settings.alphaPercent = 15 + v;
            controls.applySettings();
        });
        sliders.addView(sliderColumn("Joystick", sbJoy));
        sliders.addView(sliderColumn("Botões", sbBtn));
        sliders.addView(sliderColumn("Opacidade", sbAlpha));
        card.addView(sliders);

        LinearLayout buttons = row();
        addCell(buttons, UiKit.pill(this, "Restaurar padrão", false, v -> {
            settings.resetLayout();
            sbJoy.setProgress(settings.joySizeDp - 90);
            sbBtn.setProgress(settings.btnSizeDp - 56);
            sbAlpha.setProgress(settings.alphaPercent - 15);
            controls.applySettings();
        }));
        addCell(buttons, UiKit.pill(this, "Concluir", true, v -> exitEditMode()));
        card.addView(buttons);
        return card;
    }

    private LinearLayout sliderColumn(String label, SeekBar bar) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(UiKit.label(this, label, 12, UiKit.TEXT, false));
        col.addView(bar);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(UiKit.dp(this, 4), UiKit.dp(this, 6), UiKit.dp(this, 4), 0);
        col.setLayoutParams(lp);
        return col;
    }

    private FrameLayout.LayoutParams editPanelParams() {
        int w = Math.min(UiKit.dp(this, 560),
                getResources().getDisplayMetrics().widthPixels - UiKit.dp(this, 24));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                w, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = UiKit.dp(this, 8);
        return lp;
    }

    private void enterEditMode() {
        keys.releaseAll();
        closeMenu();
        editMode = true;
        controls.setEditMode(true);
        editPanel.setVisibility(View.VISIBLE);
    }

    private void exitEditMode() {
        settings.save();
        editMode = false;
        controls.setEditMode(false);
        editPanel.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------ helpers de layout

    private static FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = UiKit.dp(this, 8);
        l.setLayoutParams(lp);
        return l;
    }

    private void addCell(LinearLayout row, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(UiKit.dp(this, 3), 0, UiKit.dp(this, 3), 0);
        row.addView(v, lp);
    }
}
