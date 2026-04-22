package com.example.chiguaapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class PlayerActivity extends Activity {
    private LinearLayout root;
    private FrameLayout playerBox;
    private WebView playerWeb;
    private WebView sniffWeb;
    private ProgressBar loading;
    private TextView titleView;
    private TextView lineView;
    private TextView stateView;
    private LinearLayout episodeWrap;

    private SourceConfig source;
    private DrpyEngine engine;
    private String title;
    private String line;
    private String input;
    private String playUrl;
    private boolean sniffing = false;
    private boolean artReady = false;

    private ArrayList<String> episodeNames = new ArrayList<>();
    private ArrayList<String> episodeInputs = new ArrayList<>();
    private int currentIndex = 0;
    private String seriesTitle = "晓鹏影视";

    private final Handler handler = new Handler();
    private final Runnable hideState = () -> {
        if (stateView != null && artReady) {
            stateView.animate().alpha(0.35f).setDuration(220).start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        input = getIntent().getStringExtra("input");
        if (input == null) input = getIntent().getStringExtra("url");
        if (input == null) input = "";
        title = getIntent().getStringExtra("title");
        if (title == null || title.trim().length() == 0) title = "晓鹏影视";
        line = getIntent().getStringExtra("line");
        if (line == null || line.trim().length() == 0) line = "默认线路";
        seriesTitle = getIntent().getStringExtra("series_title");
        if (seriesTitle == null || seriesTitle.trim().length() == 0) {
            int split = title.indexOf(" · ");
            seriesTitle = split > 0 ? title.substring(0, split) : title;
        }
        ArrayList<String> names = getIntent().getStringArrayListExtra("episode_names");
        ArrayList<String> inputs = getIntent().getStringArrayListExtra("episode_inputs");
        if (names != null && inputs != null) {
            int n = Math.min(names.size(), inputs.size());
            for (int i = 0; i < n; i++) {
                episodeNames.add(names.get(i));
                episodeInputs.add(inputs.get(i));
            }
        }
        currentIndex = getIntent().getIntExtra("episode_index", 0);
        if (currentIndex < 0) currentIndex = 0;
        if (episodeInputs.isEmpty()) {
            episodeNames.add("播放");
            episodeInputs.add(input);
            currentIndex = 0;
        } else if (currentIndex >= episodeInputs.size()) {
            currentIndex = 0;
        }
        if (input.trim().length() == 0 && !episodeInputs.isEmpty()) input = episodeInputs.get(currentIndex);

        source = SourceConfig.load(this);
        Scraper.useSource(source);
        engine = new DrpyEngine(this, source);

        buildUi();
        updateHeader();
        buildEpisodeButtons();
        resolveAndPlay();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.parseColor("#090B10"));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(22));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        playerBox = new FrameLayout(this);
        playerBox.setBackground(cardBg("#0E1118", "#26324A", 22));
        LinearLayout.LayoutParams playerLp = new LinearLayout.LayoutParams(-1, dp(220));
        root.addView(playerBox, playerLp);

        playerWeb = createPlayerWebView();
        playerBox.addView(playerWeb, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setPadding(dp(18), dp(18), dp(18), dp(18));
        playerBox.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        loading = new ProgressBar(this);
        overlay.addView(loading, new LinearLayout.LayoutParams(dp(38), dp(38)));

        stateView = new TextView(this);
        stateView.setTextColor(Color.parseColor("#DDE5FF"));
        stateView.setTextSize(14);
        stateView.setGravity(Gravity.CENTER);
        stateView.setPadding(dp(14), dp(14), dp(14), 0);
        stateView.setText("正在解析播放地址…");
        overlay.addView(stateView, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        infoCard.setBackground(cardBg("#101521", "#222D42", 18));
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.topMargin = dp(14);
        root.addView(infoCard, infoLp);

        titleView = new TextView(this);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(17);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        infoCard.addView(titleView);

        lineView = new TextView(this);
        lineView.setTextColor(Color.parseColor("#9EAFD6"));
        lineView.setTextSize(12);
        lineView.setPadding(0, dp(8), 0, 0);
        infoCard.addView(lineView);

        TextView tip = new TextView(this);
        tip.setText("上面直接播放，下面直接切集。这里只保留最简单结构，不再放全屏、重试、外部、沉浸这些按钮。\n如果不是直链，会自动网页嗅探后再播。");
        tip.setTextColor(Color.parseColor("#C9D4F4"));
        tip.setTextSize(13);
        tip.setPadding(0, dp(14), 0, 0);
        infoCard.addView(tip);

        TextView epTitle = new TextView(this);
        epTitle.setText("选集按钮");
        epTitle.setTextColor(Color.WHITE);
        epTitle.setTextSize(17);
        epTitle.setTypeface(Typeface.DEFAULT_BOLD);
        epTitle.setPadding(dp(2), dp(18), dp(2), 0);
        root.addView(epTitle);

        LinearLayout epCard = new LinearLayout(this);
        epCard.setOrientation(LinearLayout.VERTICAL);
        epCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        epCard.setBackground(cardBg("#101521", "#23314E", 20));
        LinearLayout.LayoutParams epCardLp = new LinearLayout.LayoutParams(-1, -2);
        epCardLp.topMargin = dp(8);
        root.addView(epCard, epCardLp);

        episodeWrap = new LinearLayout(this);
        episodeWrap.setOrientation(LinearLayout.VERTICAL);
        epCard.addView(episodeWrap, new LinearLayout.LayoutParams(-1, -2));

        setContentView(scroll);
    }

    private void updateHeader() {
        String episodeName = currentIndex >= 0 && currentIndex < episodeNames.size() ? episodeNames.get(currentIndex) : "播放";
        titleView.setText(seriesTitle + " · " + episodeName);
        lineView.setText("线路：" + line + "   ·   源：" + source.title + "   ·   共 " + episodeInputs.size() + " 集");
        title = seriesTitle + " · " + episodeName;
    }

    private void buildEpisodeButtons() {
        episodeWrap.removeAllViews();
        if (episodeInputs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("当前没有可切换的选集");
            empty.setTextColor(Color.parseColor("#8EA0C4"));
            empty.setTextSize(14);
            empty.setPadding(dp(4), dp(8), dp(4), dp(8));
            episodeWrap.addView(empty);
            return;
        }
        LinearLayout row = null;
        int col = 0;
        for (int i = 0; i < episodeInputs.size(); i++) {
            if (row == null || col == 3) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                if (episodeWrap.getChildCount() > 0) rowLp.topMargin = dp(8);
                episodeWrap.addView(row, rowLp);
                col = 0;
            }
            final int index = i;
            String name = episodeNames.get(i) == null || episodeNames.get(i).trim().length() == 0 ? ("第" + (i + 1) + "集") : episodeNames.get(i);
            TextView ep = new TextView(this);
            ep.setText(name);
            ep.setTextSize(13);
            ep.setTextColor(Color.parseColor(i == currentIndex ? "#FFFFFF" : "#EAF0FF"));
            ep.setTypeface(Typeface.DEFAULT_BOLD);
            ep.setGravity(Gravity.CENTER);
            ep.setSingleLine(true);
            ep.setPadding(dp(6), dp(12), dp(6), dp(12));
            ep.setBackground(i == currentIndex ? cardBg("#5568FF", "#7384FF", 16) : cardBg("#151F35", "#31415F", 16));
            LinearLayout.LayoutParams epLp = new LinearLayout.LayoutParams(0, -2, 1);
            if (col < 2) epLp.rightMargin = dp(8);
            row.addView(ep, epLp);
            ep.setOnClickListener(v -> switchEpisode(index));
            col++;
        }
    }

    private void switchEpisode(int index) {
        if (index < 0 || index >= episodeInputs.size() || index == currentIndex) return;
        currentIndex = index;
        input = episodeInputs.get(index) == null ? "" : episodeInputs.get(index);
        updateHeader();
        buildEpisodeButtons();
        resolveAndPlay();
    }

    private WebView createPlayerWebView() {
        WebView web = new WebView(this);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
        web.setBackgroundColor(Color.BLACK);
        web.addJavascriptInterface(new PlayerBridge(), "HermesPlayer");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        return web;
    }

    private void resolveAndPlay() {
        releaseSniffer();
        artReady = false;
        playUrl = null;
        showState("正在解析播放地址…", true, 1f);

        if (source.raw != null && source.raw.length() > 0 && source.raw.contains("var rule")) {
            engine.runLazy(input, (u, err) -> {
                if (err != null && err.length() > 0 && (u == null || u.trim().length() == 0)) {
                    showError("解析失败：" + err);
                } else {
                    startPlayer(u);
                }
            });
            return;
        }

        new AsyncTask<Void, Void, String>() {
            Exception error;
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return Scraper.resolvePlay(input);
                } catch (Exception e) {
                    error = e;
                    return input;
                }
            }

            @Override
            protected void onPostExecute(String u) {
                if (error != null && (u == null || u.trim().length() == 0)) {
                    showError("解析失败：" + error.getMessage());
                } else {
                    startPlayer(u);
                }
            }
        }.execute();
    }

    private void startPlayer(String url) {
        if (url == null || url.trim().length() == 0) url = input;
        playUrl = url == null ? "" : url.trim();
        if (playUrl.length() == 0) {
            showError("未获取到可播放地址");
            return;
        }
        if (!looksLikeMedia(playUrl)) {
            startSniff(playUrl, "解析结果不是直链，正在网页嗅探…");
            return;
        }
        loadArtPlayer(playUrl);
    }

    private void loadArtPlayer(String mediaUrl) {
        sniffing = false;
        releaseSniffer();
        playUrl = mediaUrl;
        artReady = false;
        showState("播放器加载中…", true, 1f);
        playerWeb.loadDataWithBaseURL("https://artplayer.org/", buildPlayerHtml(mediaUrl), "text/html", "utf-8", null);
    }

    private void startSniff(String pageUrl, String message) {
        if (pageUrl == null || pageUrl.trim().length() == 0) {
            showError("没有可嗅探的页面地址");
            return;
        }
        sniffing = true;
        releaseSniffer();
        showState(message, true, 1f);
        sniffWeb = new WebView(this);
        sniffWeb.setVisibility(View.INVISIBLE);
        WebSettings ws = sniffWeb.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
        sniffWeb.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) captureSniff(request.getUrl().toString());
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                captureSniff(url);
                super.onLoadResource(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (sniffing) showState("嗅探中，正在抓取真实视频地址…", true, 1f);
            }
        });
        addContentView(sniffWeb, new ViewGroup.LayoutParams(1, 1));
        sniffWeb.loadUrl(pageUrl.trim());
        handler.postDelayed(() -> {
            if (sniffing) showError("嗅探超时，请换集或换线路再试");
        }, 18000);
    }

    private void captureSniff(String url) {
        if (!sniffing || !shouldSniffUrl(url)) return;
        runOnUiThread(() -> {
            if (!sniffing) return;
            sniffing = false;
            showState("已捕获真实视频地址，正在播放…", true, 1f);
            loadArtPlayer(url);
        });
    }

    private boolean looksLikeMedia(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.ROOT);
        if (u.startsWith("blob:") || u.startsWith("data:")) return false;
        return u.contains(".m3u8") || u.contains(".mp4") || u.contains(".flv") || u.contains(".mkv") || u.contains(".mpd") || u.contains("mime=video") || u.contains("/m3u8");
    }

    private boolean shouldSniffUrl(String url) {
        if (url == null || url.trim().length() == 0) return false;
        String u = url.toLowerCase(Locale.ROOT);
        if (u.startsWith("blob:") || u.startsWith("data:")) return false;
        if (u.contains(".jpg") || u.contains(".jpeg") || u.contains(".png") || u.contains(".gif") || u.contains(".webp") || u.contains(".css") || u.contains(".js") || u.contains("favicon")) return false;
        return looksLikeMedia(u) || u.contains("m3u8") || u.contains(".mp4") || u.contains(".flv");
    }

    private void showState(String text, boolean showLoading, float alpha) {
        handler.removeCallbacks(hideState);
        loading.setVisibility(showLoading ? View.VISIBLE : View.GONE);
        stateView.setVisibility(View.VISIBLE);
        stateView.setAlpha(alpha);
        stateView.setText(text);
    }

    private void showError(String text) {
        sniffing = false;
        releaseSniffer();
        artReady = false;
        showState(text, false, 1f);
    }

    private GradientDrawable cardBg(String color, String stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), Color.parseColor(stroke));
        return g;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String buildPlayerHtml(String mediaUrl) {
        String safeUrl = jsString(mediaUrl);
        String safeTitle = jsString(title);
        return "<!doctype html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"
                + "<style>html,body,#player{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden;}body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;} .art-video-player{background:#000!important;}</style>"
                + "</head><body><div id='player'></div>"
                + "<script src='https://cdn.jsdelivr.net/npm/hls.js@latest'></script>"
                + "<script src='https://cdn.jsdelivr.net/npm/artplayer/dist/artplayer.js'></script>"
                + "<script>"
                + "var url='" + safeUrl + "';"
                + "var title='" + safeTitle + "';"
                + "function postReady(){try{HermesPlayer.onReady();}catch(e){}}"
                + "function postError(msg){try{HermesPlayer.onError(String(msg||''));}catch(e){}}"
                + "try{var art=new Artplayer({container:'#player',url:url,title:title,autoplay:true,autoSize:true,fullscreen:false,fullscreenWeb:false,pip:false,screenshot:false,setting:true,hotkey:true,playbackRate:true,aspectRatio:true,theme:'#6D7CFF',moreVideoAttr:{crossorigin:'anonymous'},customType:{m3u8:function(video,playUrl){if(window.Hls&&Hls.isSupported()){var hls=new Hls();hls.loadSource(playUrl);hls.attachMedia(video);hls.on(Hls.Events.MANIFEST_PARSED,function(){video.play().catch(function(){});});hls.on(Hls.Events.ERROR,function(evt,data){if(data&&data.fatal){postError('HLS '+data.type+' '+data.details);}});art.hls=hls;}else if(video.canPlayType('application/vnd.apple.mpegurl')){video.src=playUrl;video.addEventListener('loadedmetadata',function(){video.play().catch(function(){});});}else{postError('当前设备不支持 m3u8');}}}});art.on('ready',function(){postReady();});art.on('video:error',function(err){postError(err&&err.message?err.message:'video error');});art.on('error',function(err){postError(err&&err.message?err.message:'art error');});window.addEventListener('error',function(e){postError(e&&e.message?e.message:'window error');});}catch(e){postError(e&&e.message?e.message:'player init error');}"
                + "</script></body></html>";
    }

    private String jsString(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private void releaseSniffer() {
        handler.removeCallbacks(hideState);
        if (sniffWeb != null) {
            try {
                ViewGroup parent = (ViewGroup) sniffWeb.getParent();
                if (parent != null) parent.removeView(sniffWeb);
            } catch (Exception ignored) {}
            try {
                sniffWeb.stopLoading();
                sniffWeb.loadUrl("about:blank");
                sniffWeb.destroy();
            } catch (Exception ignored) {}
            sniffWeb = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerWeb != null) playerWeb.onResume();
        if (sniffWeb != null) sniffWeb.onResume();
    }

    @Override
    protected void onPause() {
        if (playerWeb != null) playerWeb.onPause();
        if (sniffWeb != null) sniffWeb.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler.removeCallbacksAndMessages(null);
        releaseSniffer();
        if (playerWeb != null) {
            try {
                playerWeb.stopLoading();
                playerWeb.loadUrl("about:blank");
                playerWeb.destroy();
            } catch (Exception ignored) {}
            playerWeb = null;
        }
        super.onDestroy();
    }

    private final class PlayerBridge {
        @JavascriptInterface
        public void onReady() {
            runOnUiThread(() -> {
                artReady = true;
                showState("正在播放", false, 1f);
                handler.postDelayed(hideState, 1200);
            });
        }

        @JavascriptInterface
        public void onError(String message) {
            runOnUiThread(() -> showError("播放失败：" + (message == null ? "未知错误" : message)));
        }
    }
}
