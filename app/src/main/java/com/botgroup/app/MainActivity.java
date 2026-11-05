package com.botgroup.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * BotGroup Chat 安卓客户端（WebView 壳应用）
 * 主页面直接嵌入 https://botgroup.chat/
 * 支持站内 OAuth 登录（Google / GitHub / Linux.do）会话持久化
 */
public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;

    private static final String HOME_URL = "https://botgroup.chat/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);

        // ---- WebView 基础配置 ----
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);                 // 网站为 SPA，必须开 JS
        settings.setDomStorageEnabled(true);                 // 本地存储（登录态缓存）
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // ---- Cookie 持久化：登录会话不随应用退出丢失 ----
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // ---- 页面加载回调 ----
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 所有跳转（含 OAuth 登录跳转到 google/github/linux.do）都在 WebView 内完成，
                // 避免跳到外部浏览器导致登录会话断裂。
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    // 网络异常时加载内置提示页
                    view.loadData(
                            "<html><body style='text-align:center;padding-top:80px;color:#666;font-family:sans-serif'>"
                                    + "<h3>网络连接失败</h3>"
                                    + "<p>请检查网络后点击下方按钮重试</p>"
                                    + "<button onclick='location.reload()' style='padding:12px 24px;border:none;background:#4F6EF7;color:#fff;border-radius:8px;font-size:16px'>重新加载</button>"
                                    + "</body></html>",
                            "text/html; charset=utf-8", "utf-8");
                }
            }
        });

        // ---- 进度条 ----
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        // 加载主页
        webView.loadUrl(HOME_URL);
    }

    // ---- 返回键：先返回网页历史，再退出应用 ----
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
