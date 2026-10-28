package com.zicao.aventura;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {
    private WebView web;
    private float lx, ly, lhx, lhy;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        web.setFocusable(false);
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        hideBars();
        web.loadUrl("file:///android_asset/index.html");
    }

    class Bridge {
        @JavascriptInterface public void exit() { runOnUiThread(new Runnable() { public void run() { finish(); } }); }
    }

    private void hideBars() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) hideBars(); }

    private void js(String code) { if (web != null) web.evaluateJavascript(code, null); }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int c = e.getKeyCode();
        if (c == KeyEvent.KEYCODE_VOLUME_UP || c == KeyEvent.KEYCODE_VOLUME_DOWN || c == KeyEvent.KEYCODE_VOLUME_MUTE
            || c == KeyEvent.KEYCODE_HOME || c == KeyEvent.KEYCODE_POWER) return super.dispatchKeyEvent(e);
        if (c == KeyEvent.KEYCODE_BACK && e.isLongPress()) { finish(); return true; }
        int a = e.getAction();
        if (a == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) js("window.__key&&__key(" + c + ",1)");
        else if (a == KeyEvent.ACTION_UP) js("window.__key&&__key(" + c + ",0)");
        if (c == KeyEvent.KEYCODE_BACK && a == KeyEvent.ACTION_DOWN) e.startTracking();
        return GAME_KEYS.contains(c) || super.dispatchKeyEvent(e);
    }
    private static final java.util.Set<Integer> GAME_KEYS = new java.util.HashSet<>(java.util.Arrays.asList(
        4,19,20,21,22,23,62,66,82,96,97,98,99,100,101,102,103,104,105,106,107,108,109,111,160));
    @Override protected void onDestroy() { super.onDestroy(); if (web != null) { web.destroy(); web = null; } }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent e) {
        int src = e.getSource();
        if (((src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK || (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
            && e.getAction() == MotionEvent.ACTION_MOVE) {
            float x = e.getAxisValue(MotionEvent.AXIS_X), y = e.getAxisValue(MotionEvent.AXIS_Y);
            float hx = e.getAxisValue(MotionEvent.AXIS_HAT_X), hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y);
            if (x != lx || y != ly || hx != lhx || hy != lhy) {
                lx = x; ly = y; lhx = hx; lhy = hy;
                js("window.__axis&&__axis(" + x + "," + y + "," + hx + "," + hy + ")");
            }
            return true;
        }
        return super.dispatchGenericMotionEvent(e);
    }

    @Override protected void onPause() { super.onPause(); js("window.__appPause&&__appPause()"); if (web != null) web.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); hideBars(); }
}
