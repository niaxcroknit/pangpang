package com.pangparapang.game;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedDispatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** 게임 전체는 assets/index.html(HTML+JS)에 들어 있고, 이 화면은 그걸 꽉 차게 띄워 줍니다. */
public class MainActivity extends Activity {
    private WebView web;
    private FrameLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 휴대폰은 세로로 고정, 태블릿(가장 짧은 변 600dp 이상)은 가로·세로 모두 돼요.
        setRequestedOrientation(getResources().getConfiguration().smallestScreenWidthDp >= 600
                ? ActivityInfo.SCREEN_ORIENTATION_FULL_USER : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // 진행 단계·최고 점수·하던 판 저장(localStorage)
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);                    // 휴대폰 글자 크기 설정 때문에 판이 깨지지 않게
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        // 게임 파일(assets)을 https 주소로 열어요. 결과 그림 만들기·복사 같은 기능은 https에서만 제대로 돼요.
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (!ASSET_HOST.equals(u.getHost()) || u.getPath() == null || !u.getPath().startsWith("/assets/")) return null;
                String path = u.getPath().substring("/assets/".length());
                if (path.contains("..")) return null;
                try {
                    InputStream in = getAssets().open(path);
                    return new WebResourceResponse(mimeOf(path), path.endsWith(".html") || path.endsWith(".css") ? "utf-8" : null, in);
                } catch (Exception e) {
                    return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null, new java.io.ByteArrayInputStream(new byte[0]));
                }
            }
        });
        web.addJavascriptInterface(new Bridge(), "PangApp");
        web.setBackgroundColor(Color.WHITE);

        // Android 15부터는 화면이 상태바·내비게이션바 밑까지 깔려요(Android 16을 대상으로 하면 끌 수 없어요).
        // 그래서 시스템 바 높이만큼 여백을 줘서 게임이 가리지 않게 해요.
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        root.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            });
        }
        setContentView(root);

        // 뒤로가기(Android 13+ 새 방식): 창이 떠 있으면 닫고, 게임 중이면 첫 화면으로, 첫 화면에서만 앱을 닫아요.
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        // 폰의 다크 모드와 글자 크기 설정을 게임에 알려 줘요(글자를 크게 쓰는 분은 '큰 글씨'가 처음부터 켜져요).
        else web.loadUrl("https://" + ASSET_HOST + "/assets/index.html?sys=" + (isSystemDark() ? "dark" : "light")
                + "&font=" + getResources().getConfiguration().fontScale);
    }

    private static final String ASSET_HOST = "appassets.androidplatform.net";

    private static String mimeOf(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "text/javascript";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    private boolean isSystemDark() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void handleBack() {
        web.evaluateJavascript("window.pangBack?window.pangBack():'exit'", value -> {
            if (value == null || value.contains("exit")) finish();
        });
    }

    /** Android 12 이하에서 쓰는 예전 방식의 뒤로가기. */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= 33) super.onBackPressed();
        else handleBack();
    }

    /** 폰의 다크 모드를 바꾸면 게임도 따라가요(게임 안에서 직접 고른 경우는 그대로). */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        web.evaluateJavascript("window.pangSystemTheme&&window.pangSystemTheme('" + (isSystemDark() ? "dark" : "light") + "')", null);
    }

    /** 앱을 잠깐 나갈 때 하던 판을 저장해요. */
    @Override
    protected void onPause() {
        super.onPause();
        web.evaluateJavascript("document.dispatchEvent(new Event('visibilitychange'))", null);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    private class Bridge {
        /** 게임 안 밝기 모드에 맞춰 상태바·내비게이션바 색과 아이콘 색을 바꿔요. */
        @JavascriptInterface
        @SuppressWarnings("deprecation")
        public void setDark(boolean dark) {
            runOnUiThread(() -> {
                Window w = getWindow();
                int bg = dark ? Color.parseColor("#191D2B") : Color.WHITE;
                root.setBackgroundColor(bg);
                web.setBackgroundColor(bg);
                w.getDecorView().setBackgroundColor(bg);
                if (Build.VERSION.SDK_INT < 35) {
                    w.setStatusBarColor(bg);
                    w.setNavigationBarColor(bg);
                }
                if (Build.VERSION.SDK_INT >= 30) {
                    WindowInsetsController c = w.getInsetsController();
                    if (c != null) {
                        int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                        c.setSystemBarsAppearance(dark ? 0 : light, light);
                    }
                } else {
                    int flags = w.getDecorView().getSystemUiVisibility();
                    int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    w.getDecorView().setSystemUiVisibility(dark ? (flags & ~light) : (flags | light));
                }
            });
        }

        /** 자랑하기: 결과 카드 그림(PNG, base64)과 글을 카카오톡으로 바로 보내요. 카톡이 없으면 공유 목록을 띄워요. */
        @JavascriptInterface
        public void shareImage(String base64Png, String text) {
            try {
                File dir = new File(getCacheDir(), "share");
                if (!dir.exists() && !dir.mkdirs()) return;
                File f = new File(dir, "pangpang.png");
                try (FileOutputStream out = new FileOutputStream(f)) {
                    out.write(Base64.decode(base64Png, Base64.DEFAULT));
                }
                Uri uri = Uri.parse("content://" + ShareProvider.AUTHORITY + "/pangpang.png");
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("image/png");
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.putExtra(Intent.EXTRA_TEXT, text);
                send.setClipData(ClipData.newRawUri("", uri));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> {
                    Intent kakao = new Intent(send).setPackage("com.kakao.talk");
                    try {
                        startActivity(kakao);
                    } catch (ActivityNotFoundException e) {
                        startActivity(Intent.createChooser(send, "자랑하기"));
                    }
                });
            } catch (Exception ignored) {
                // 공유에 실패해도 게임은 계속돼요.
            }
        }

        /** 의견 보내기: 메일 앱을 열어 받는 사람·제목·내용을 채워 줘요. */
        @JavascriptInterface
        public void sendMail(String to, String subject, String body) {
            Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
            mail.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
            mail.putExtra(Intent.EXTRA_SUBJECT, subject);
            mail.putExtra(Intent.EXTRA_TEXT, body);
            runOnUiThread(() -> {
                try {
                    startActivity(Intent.createChooser(mail, "의견 보내기"));
                } catch (ActivityNotFoundException e) {
                    web.evaluateJavascript("window.pangToast&&pangToast('메일 앱이 없어요. " + to + " 으로 보내 주세요')", null);
                }
            });
        }

        @JavascriptInterface
        public boolean isApp() {
            return true;
        }
    }
}
