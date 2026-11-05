package com.qwenwork.app;

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
 * QwenWork 千问办公安卓客户端（WebView 壳应用）
 * 主页面直接嵌入 https://qwenwork.cn/app
 * 登录会话（Cookie）持久化保存
 */
public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;

    private static final String HOME_URL = "https://qwenwork.cn/app";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);

        // ---- WebView 基础配置 ----
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // ---- Cookie 持久化：登录会话不丢失 ----
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // ---- 页面加载回调 ----
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 所有跳转（含 OAuth 登录）在 WebView 内完成，保证登录会话不断链
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

        // ---- 顶部进度条 ----
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

    // ---- 返回键：先返回网页历史 ----
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
