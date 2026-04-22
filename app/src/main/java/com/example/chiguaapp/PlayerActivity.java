package com.example.chiguaapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

public class PlayerActivity extends Activity {
    private FrameLayout root;
    private WebView web;
    private LinearLayout topBar;
    private TextView titleView;
    private TextView stateView;
    private TextView retryBtn;
    private TextView externalBtn;
    private ProgressBar loading;

    private SourceConfig source;
    private DrpyEngine engine;
    private String title;
    private String line;
    private String input;
    private String playUrl;
    private boolean resolved;
    private boolean chromeFullscreen;
    private final Handler handler = new Handler();

    private final Runnable hideBars = new Runnable() {
        @Override public void run() {
            if (topBar != null && !chromeFullscreen) topBar.animate().alpha(0f).setDuration(220).start();
            if (stateView != null && resolved) stateView.animate().alpha(0f).setDuration(220).start();
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        keepAwake(true);
        immersive();

        input = getIntent().getStringExtra("input");
        if (input == null) input = getIntent().getStringExtra("url");
        if (input == null) input = "";
        title = getIntent().getStringExtra("title");
        if (title == null || title.trim().length() == 0) title = "晓鹏影视";
        line = getIntent().getStringExtra("line");
        if (line == null || line.trim().length() == 0) line = "默认线路";

        source = SourceConfig.load(this);
        Scraper.useSource(source);
        engine = new DrpyEngine(this, source);

        buildUi();
        setupWebView();
        resolveAndPlay();
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(8), dp(12), dp(8));
        topBar.setBackgroundColor(Color.parseColor("#CC050814"));

        TextView back = pill("返回", "#20284A");
        back.setOnClickListener(v -> finish());
        topBar.addView(back, new LinearLayout.LayoutParams(-2, dp(36)));

        titleView = new TextView(this);
        titleView.setText(title + "  ·  " + line);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(14);
        titleView.setSingleLine(true);
        titleView.setPadding(dp(12), 0, dp(12), 0);
        topBar.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));

        retryBtn = pill("重试", "#24315F");
        retryBtn.setOnClickListener(v -> resolveAndPlay());
        topBar.addView(retryBtn, new LinearLayout.LayoutParams(-2, dp(36)));

        externalBtn = pill("外部", "#6B7CFF");
        externalBtn.setOnClickListener(v -> openExternal());
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-2, dp(36));
        ep.leftMargin = dp(8);
        topBar.addView(externalBtn, ep);

        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        root.addView(topBar, tp);

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        loading = new ProgressBar(this);
        center.addView(loading, new LinearLayout.LayoutParams(dp(42), dp(42)));
        stateView = new TextView(this);
        stateView.setText("正在解析播放地址…");
        stateView.setTextColor(Color.parseColor("#C8D2F0"));
        stateView.setTextSize(14);
        stateView.setGravity(Gravity.CENTER);
        stateView.setPadding(dp(16), dp(12), dp(16), dp(12));
        center.addView(stateView, new LinearLayout.LayoutParams(-2, -2));
        root.addView(center, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));

        root.setOnClickListener(v -> showBarsTemporarily());
        setContentView(root);
    }

    private TextView pill(String text, String bg) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(13);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(14), 0, dp(14), 0);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(Color.parseColor(bg));
        g.setCornerRadius(dp(18));
        v.setBackground(g);
        return v;
    }

    private void setupWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        if (Build.VERSION.SDK_INT >= 21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        web.setBackgroundColor(Color.BLACK);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            private View custom;
            private CustomViewCallback cb;
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (custom != null) { callback.onCustomViewHidden(); return; }
                chromeFullscreen = true;
                custom = view; cb = callback;
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                topBar.setVisibility(View.GONE);
                stateView.setVisibility(View.GONE);
                immersive();
            }
            @Override public void onHideCustomView() {
                if (custom == null) return;
                root.removeView(custom);
                custom = null;
                chromeFullscreen = false;
                topBar.setVisibility(View.VISIBLE);
                stateView.setVisibility(View.VISIBLE);
                if (cb != null) cb.onCustomViewHidden();
                immersive();
                showBarsTemporarily();
            }
        });
    }

    private void resolveAndPlay() {
        resolved = false;
        playUrl = null;
        loading.setVisibility(View.VISIBLE);
        stateView.setVisibility(View.VISIBLE);
        stateView.setAlpha(1f);
        stateView.setText("正在解析播放地址…");
        showBarsTemporarily();

        if (source.raw != null && source.raw.length() > 0 && source.raw.contains("var rule")) {
            engine.runLazy(input, (u, err) -> {
                if (err != null && err.length() > 0 && (u == null || u.length() == 0)) showError("解析失败：" + err);
                else loadPlayer(u);
            });
            return;
        }
        new AsyncTask<Void, Void, String>() {
            Exception error;
            @Override protected String doInBackground(Void... v) {
                try { return Scraper.resolvePlay(input); }
                catch (Exception e) { error = e; return input; }
            }
            @Override protected void onPostExecute(String u) {
                if (error != null && (u == null || u.length() == 0)) showError("解析失败：" + error.getMessage());
                else loadPlayer(u);
            }
        }.execute();
    }

    private void loadPlayer(String url) {
        if (url == null || url.trim().length() == 0) url = input;
        playUrl = url.trim();
        resolved = true;
        loading.setVisibility(View.GONE);
        stateView.setText("播放器加载中… 如果黑屏可点右上角“外部”或返回换线路");
        stateView.setAlpha(1f);
        web.loadDataWithBaseURL("https://artplayer.org/", html(playUrl, title), "text/html", "UTF-8", null);
        handler.postDelayed(hideBars, 3500);
    }

    private void showError(String msg) {
        loading.setVisibility(View.GONE);
        resolved = false;
        stateView.setAlpha(1f);
        stateView.setVisibility(View.VISIBLE);
        stateView.setText(msg + "\n可点击“重试 / 外部”，或返回详情页切换线路");
        showBarsTemporarily();
    }

    private String q(String s) { return JSONObject.quote(s == null ? "" : s); }

    private String html(String url, String title) {
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>" +
                "<style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#03050b;color:#fff;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif}.artplayer-app{width:100vw;height:100vh;background:#000}.toast{position:fixed;left:50%;bottom:22px;transform:translateX(-50%);max-width:82%;padding:9px 14px;border-radius:999px;background:rgba(8,12,26,.72);color:#cbd5ff;font-size:13px;text-align:center;z-index:9;backdrop-filter:blur(8px)}.toast.hide{display:none}</style>" +
                "<script src='https://cdn.jsdelivr.net/npm/hls.js@latest'></script><script src='https://cdn.jsdelivr.net/npm/artplayer/dist/artplayer.js'></script></head>" +
                "<body><div class='artplayer-app'></div><div class='toast' id='toast'>晓鹏影视播放器加载中…</div><script>" +
                "const videoUrl=" + q(url) + ";const videoTitle=" + q(title) + ";" +
                "function toast(t){const e=document.getElementById('toast');e.className='toast';e.innerText=t;clearTimeout(window.__t);window.__t=setTimeout(()=>e.className='toast hide',2600)}" +
                "function playM3u8(video,url,art){if(window.Hls&&Hls.isSupported()){if(art.hls)art.hls.destroy();const hls=new Hls({enableWorker:true,lowLatencyMode:true,maxBufferLength:45});hls.loadSource(url);hls.attachMedia(video);art.hls=hls;art.on('destroy',()=>hls.destroy());hls.on(Hls.Events.ERROR,(e,d)=>{if(d&&d.fatal){toast('线路播放异常，请返回换线路');}});}else if(video.canPlayType('application/vnd.apple.mpegurl')){video.src=url;}else{video.src=url;}}" +
                "const isM3u8=/\\.m3u8(\\?|$)/i.test(videoUrl);" +
                "const art=new Artplayer({container:'.artplayer-app',url:videoUrl,type:isM3u8?'m3u8':'',title:videoTitle,autoplay:true,muted:false,autoSize:false,autoMini:false,pip:true,screenshot:true,setting:true,hotkey:true,fullscreen:true,fullscreenWeb:true,playbackRate:true,aspectRatio:true,lock:true,fastForward:true,autoPlayback:true,theme:'#6B7CFF',customType:{m3u8:playM3u8},settings:[{html:'画面比例',selector:[{html:'默认',value:'default'},{html:'16:9',value:'16:9'},{html:'铺满',value:'cover'}],onSelect:function(item){const v=art.video;if(item.value==='16:9')v.style.objectFit='contain';else if(item.value==='cover')v.style.objectFit='cover';else v.style.objectFit='contain';return item.html;}}]});" +
                "art.on('ready',()=>toast('已就绪'));art.on('video:waiting',()=>toast('缓冲中…'));art.on('video:playing',()=>document.getElementById('toast').className='toast hide');art.on('error',()=>toast('播放失败，请返回换线路或点外部播放'));" +
                "document.addEventListener('keydown',e=>{if(e.key==='Escape')art.fullscreenWeb=false});" +
                "</script></body></html>";
    }

    private void openExternal() {
        String u = playUrl != null && playUrl.length() > 0 ? playUrl : input;
        if (u == null || u.length() == 0) return;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("无法打开外部播放器").setMessage(e.getMessage()).setPositiveButton("知道了", null).show();
        }
    }

    private void showBarsTemporarily() {
        handler.removeCallbacks(hideBars);
        if (topBar != null) {
            topBar.setVisibility(View.VISIBLE);
            topBar.animate().alpha(1f).setDuration(120).start();
        }
        if (stateView != null) stateView.animate().alpha(1f).setDuration(120).start();
        handler.postDelayed(hideBars, 4000);
    }

    private void immersive() {
        int flags = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void keepAwake(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if (hasFocus) immersive(); }
    @Override protected void onResume() { super.onResume(); immersive(); if (web != null) web.onResume(); }
    @Override protected void onPause() { if (web != null) web.onPause(); super.onPause(); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); keepAwake(false); if (web != null) { web.loadUrl("about:blank"); web.stopLoading(); web.destroy(); } super.onDestroy(); }
    @Override public void onBackPressed() { finish(); }
}
