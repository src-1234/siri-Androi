package com.wukong.ai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView wv;
    private ValueCallback<Uri[]> fpCallback;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        wv = new WebView(this);
        setContentView(wv);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setTextZoom(100);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url == null || url.startsWith("file://")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { }
                return true;
            }
        });
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                fpCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(i, "选择图片"), 1);
                return true;
            }
        });
        wv.addJavascriptInterface(new Bridge(), "WukongBridge");
        wv.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    protected void onActivityResult(int rc, int rr, Intent d) {
        if (rc == 1 && fpCallback != null) {
            Uri[] r = null;
            if (rr == RESULT_OK && d != null && d.getData() != null) r = new Uri[]{d.getData()};
            fpCallback.onReceiveValue(r);
            fpCallback = null;
        }
    }

    class Bridge {
        @JavascriptInterface
        public String aiRequest(String json) {
            return Net.post(json);
        }

        @JavascriptInterface
        public String appInfo() {
            return "{\"version\":\"1.0.0\",\"platform\":\"android\"}";
        }
    }
}
