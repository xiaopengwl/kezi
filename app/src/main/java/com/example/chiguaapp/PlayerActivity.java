package com.example.chiguaapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.HorizontalScrollView;
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
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.parseColor("#090B10"));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(12), dp(10), dp(12), dp(8));
        nav.setBackgroundColor(Color.parseColor("#0B0F18"));
        page.addView(nav, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextColor(Color.WHITE);
        back.setTextSize(30);
        back.setTypeface(Typeface.DEFAULT_BOLD);
        back.setGravity(Gravity.CENTER);
        back.setBackground(cardBg("#171D2B", "#2D3548", 18));
        nav.addView(back, new LinearLayout.LayoutParams(dp(42), dp(42)));
        back.setOnClickListener(v -> finish());

        LinearLayout navText = new LinearLayout(this);
        navText.setOrientation(LinearLayout.VERTICAL);
        navText.setPadding(dp(10), 0, dp(10), 0);
        nav.addView(navText, new LinearLayout.LayoutParams(0, -1, 1));

        titleView = new TextView(this);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setSingleLine(true);
        navText.addView(titleView, new LinearLayout.LayoutParams(-1, 0, 1));

        lineView = new TextView(this);
        lineView.setTextColor(Color.parseColor("#9EAFD6"));
        lineView.setTextSize(11);
        lineView.setSingleLine(true);
        navText.addView(lineView, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView sourceTag = new TextView(this);
        sourceTag.setText("移动版");
        sourceTag.setTextColor(Color.parseColor("#B8FFEA"));
        sourceTag.setTextSize(12);
        sourceTag.setTypeface(Typeface.DEFAULT_BOLD);
        sourceTag.setGravity(Gravity.CENTER);
        sourceTag.setPadding(dp(10), 0, dp(10), 0);
        sourceTag.setBackground(cardBg("#17322E", "#3DD2AC", 18));
        nav.addView(sourceTag, new LinearLayout.LayoutParams(-2, dp(36)));

        playerBox = new FrameLayout(this);
        playerBox.setBackground(cardBg("#05070B", "#151B2A", 0));
        page.addView(playerBox, new LinearLayout.LayoutParams(-1, dp(230)));

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

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(18));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(dp(15), dp(14), dp(15), dp(14));
        infoCard.setBackground(cardBg("#101521", "#222D42", 18));
        root.addView(infoCard, new LinearLayout.LayoutParams(-1, -2));

        TextView section = new TextView(this);
        section.setText("正在观看");
        section.setTextColor(Color.WHITE);
        section.setTextSize(17);
        section.setTypeface(Typeface.DEFAULT_BOLD);
        infoCard.addView(section);

        TextView tip = new TextView(this);
        tip.setText("顶部播放器 + 顶部导航 + 底部选集栏。非直链会自动解析/嗅探，不需要手动点外部播放器。");
        tip.setTextColor(Color.parseColor("#C9D4F4"));
        tip.setTextSize(13);
        tip.setPadding(0, dp(10), 0, 0);
        infoCard.addView(tip);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(dp(12), dp(8), dp(12), dp(10));
        bottom.setBackground(cardBg("#0F1420", "#28334D", 0));
        page.addView(bottom, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout bottomTitle = new LinearLayout(this);
        bottomTitle.setGravity(Gravity.CENTER_VERTICAL);
        bottom.addView(bottomTitle, new LinearLayout.LayoutParams(-1, dp(28)));

        TextView epTitle = new TextView(this);
        epTitle.setText("选集");
        epTitle.setTextColor(Color.WHITE);
        epTitle.setTextSize(15);
        epTitle.setTypeface(Typeface.DEFAULT_BOLD);
        bottomTitle.addView(epTitle, new LinearLayout.LayoutParams(0, -1, 1));

        TextView epHint = new TextView(this);
        epHint.setText("左右滑动切换");
        epHint.setTextColor(Color.parseColor("#8EA0C4"));
        epHint.setTextSize(11);
        epHint.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        bottomTitle.addView(epHint, new LinearLayout.LayoutParams(-2, -1));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        bottom.addView(hsv, new LinearLayout.LayoutParams(-1, dp(50)));

        episodeWrap = new LinearLayout(this);
        episodeWrap.setOrientation(LinearLayout.HORIZONTAL);
        episodeWrap.setPadding(0, dp(6), 0, 0);
        hsv.addView(episodeWrap, new HorizontalScrollView.LayoutParams(-2, -1));

        setContentView(page);
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
            empty.setText("暂无选集");
            empty.setTextColor(Color.parseColor("#8EA0C4"));
            empty.setTextSize(13);
            empty.setPadding(dp(12), 0, dp(12), 0);
            episodeWrap.addView(empty, new LinearLayout.LayoutParams(-2, dp(42)));
            return;
        }
        for (int i = 0; i < episodeInputs.size(); i++) {
            final int index = i;
            String name = episodeNames.get(i) == null || episodeNames.get(i).trim().length() == 0 ? ("第" + (i + 1) + "集") : episodeNames.get(i);
            TextView ep = new TextView(this);
            ep.setText(name);
            ep.setTextSize(13);
            ep.setTextColor(Color.parseColor(i == currentIndex ? "#FFFFFF" : "#DDE6FF"));
            ep.setTypeface(Typeface.DEFAULT_BOLD);
            ep.setGravity(Gravity.CENTER);
            ep.setSingleLine(true);
            ep.setPadding(dp(16), 0, dp(16), 0);
            ep.setBackground(i == currentIndex ? cardBg("#E50914", "#FF5260", 18) : cardBg("#182033", "#33415F", 18));
            LinearLayout.LayoutParams epLp = new LinearLayout.LayoutParams(-2, dp(40));
            epLp.rightMargin = dp(8);
            episodeWrap.addView(ep, epLp);
            ep.setOnClickListener(v -> switchEpisode(index));
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        ws.setAllowUniversalAccessFromFileURLs(true);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
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
                + "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,viewport-fit=cover,user-scalable=no'>"
                + "<style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden;}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#fff;}#mount{position:relative;width:100%;height:100%;background:#000;}#player{position:absolute;inset:0;}#centerPlay{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:66px;height:66px;border-radius:50%;background:rgba(255,255,255,.18);border:1px solid rgba(255,255,255,.26);backdrop-filter:blur(8px);display:flex;align-items:center;justify-content:center;z-index:9;transition:opacity .2s ease,transform .2s ease;}#centerPlay:after{content:'';margin-left:4px;border-left:18px solid rgba(255,255,255,.95);border-top:11px solid transparent;border-bottom:11px solid transparent;}#centerPlay.hide{opacity:0;pointer-events:none;transform:translate(-50%,-50%) scale(.92);}#titleFade{position:absolute;left:0;right:0;top:0;padding:14px 14px 28px;background:linear-gradient(180deg,rgba(0,0,0,.70),rgba(0,0,0,0));font-size:14px;font-weight:600;letter-spacing:.2px;z-index:8;pointer-events:none;text-shadow:0 1px 10px rgba(0,0,0,.35);} .art-video-player{background:#000!important;} .art-video-player .art-controls{background:linear-gradient(180deg,rgba(0,0,0,0),rgba(0,0,0,.78))!important;padding-bottom:2px!important;} .art-video-player .art-bottom{padding:0 10px 10px!important;} .art-video-player .art-progress{height:3px!important;} .art-video-player .art-control .art-icon{transform:scale(.96);} .art-video-player .art-setting-panel{background:rgba(13,17,23,.95)!important;border:1px solid rgba(120,140,180,.25);} </style>"
                + "</head><body><div id='mount'><div id='player'></div><div id='titleFade'>" + safeTitle + "</div><div id='centerPlay'></div></div>"
                + "<script>globalThis.CUSTOM_USER_AGENT='Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148';</script>"
                + "<script src='https://cdnjs.cloudflare.com/ajax/libs/hls.js/1.5.17/hls.min.js'></script>"
                + "<script src='https://cdn.jsdelivr.net/npm/artplayer/dist/artplayer.js'></script>"
                + "<script>"
                + "var url='" + safeUrl + "';"
                + "function postReady(){try{HermesPlayer.onReady();}catch(e){}}"
                + "function postError(msg){try{HermesPlayer.onError(String(msg||''));}catch(e){}}"
                + "function lower(u){return String(u||'').toLowerCase();}"
                + "function guessType(u){u=lower(u);if(u.indexOf('m3u8')>-1||u.indexOf('application/vnd.apple.mpegurl')>-1)return 'm3u8';if(u.indexOf('.flv')>-1)return 'flv';if(u.indexOf('.mpd')>-1)return 'mpd';if(u.indexOf('.mkv')>-1)return 'mkv';return '';}"
                + "var vtype=guessType(url);"
                + "var center=document.getElementById('centerPlay');"
                + "function hideCenter(){center.classList.add('hide');}function showCenter(){center.classList.remove('hide');}"
                + "function playM3u8(video, playUrl, art){if(art.hls){try{art.hls.destroy();}catch(e){}}var nativeOk=video.canPlayType('application/vnd.apple.mpegurl')||video.canPlayType('application/x-mpegURL');if(nativeOk){video.src=playUrl;video.addEventListener('loadedmetadata',function(){video.play().catch(function(){});},{once:true});return;}if(window.Hls&&Hls.isSupported()){var hls=new Hls({enableWorker:true,lowLatencyMode:false});hls.loadSource(playUrl);hls.attachMedia(video);art.hls=hls;art.on('destroy',function(){try{hls.destroy();}catch(e){}});hls.on(Hls.Events.MANIFEST_PARSED,function(){video.play().catch(function(){});});hls.on(Hls.Events.ERROR,function(evt,data){if(data&&data.fatal){postError('HLS '+data.type+' '+data.details);}});}else{postError('当前设备不支持 m3u8');}}"
                + "try{if(!window.Artplayer){throw new Error('ArtPlayer 脚本加载失败');}var opt={container:'#player',url:url,autoplay:true,type:vtype,autoSize:true,playsInline:true,setting:true,flip:false,pip:false,screenshot:false,fullscreen:true,fullscreenWeb:true,miniProgressBar:true,backdrop:true,lock:true,gesture:true,fastForward:true,autoOrientation:true,hotkey:false,playbackRate:true,aspectRatio:true,lang:'zh-cn',theme:'#4CCB89',mutex:true,volume:.7,moreVideoAttr:{preload:'auto','webkit-playsinline':true,playsInline:true,'x5-video-player-type':'h5-page','x5-video-player-fullscreen':'true',x5VideoPlayerType:'h5-page',x5VideoPlayerFullscreen:true,crossOrigin:'anonymous'},customType:{m3u8:playM3u8}};var art=new Artplayer(opt);center.addEventListener('click',function(){art.play();hideCenter();});art.on('ready',function(){postReady();try{art.play();}catch(e){}});art.on('play',function(){hideCenter();});art.on('pause',function(){showCenter();});art.on('video:ended',function(){showCenter();});art.on('video:error',function(err){postError(err&&err.message?err.message:'video error');});art.on('error',function(err){postError(err&&err.message?err.message:'art error');});window.addEventListener('error',function(e){postError(e&&e.message?e.message:'window error');});setTimeout(function(){hideCenter();},1200);}catch(e){postError(e&&e.message?e.message:'player init error');}"
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
