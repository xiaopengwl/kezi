package com.example.chiguaapp;

import android.app.*;import android.os.*;import android.webkit.*;import android.view.*;import android.widget.*;import android.graphics.*;import android.content.pm.ActivityInfo;import org.json.JSONObject;

public class PlayerActivity extends Activity{
    WebView web; LinearLayout root; TextView bar,hint; SourceConfig source; DrpyEngine engine; String title,line,input;
    public void onCreate(Bundle b){
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        input=getIntent().getStringExtra("input"); if(input==null)input=getIntent().getStringExtra("url"); if(input==null)input="";
        title=getIntent().getStringExtra("title"); if(title==null)title="晓鹏影视"; line=getIntent().getStringExtra("line"); if(line==null)line="默认线路";
        source=SourceConfig.load(this); Scraper.useSource(source); engine=new DrpyEngine(this,source);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.BLACK);
        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(12,6,12,6); top.setBackgroundColor(Color.parseColor("#0B1020"));
        bar=new TextView(this); bar.setText("晓鹏影视 · "+title+" · "+line); bar.setTextColor(Color.WHITE); bar.setTextSize(14); bar.setSingleLine(true); top.addView(bar,new LinearLayout.LayoutParams(0,-2,1));
        TextView close=new TextView(this); close.setText("退出"); close.setTextColor(Color.WHITE); close.setTextSize(14); close.setGravity(Gravity.CENTER); close.setPadding(24,8,24,8); close.setOnClickListener(v->finish()); top.addView(close,new LinearLayout.LayoutParams(-2,-2));
        root.addView(top,new LinearLayout.LayoutParams(-1,-2));
        web=new WebView(this); root.addView(web,new LinearLayout.LayoutParams(-1,0,1)); hint=new TextView(this); hint.setText("正在解析播放地址..."); hint.setTextColor(Color.parseColor("#9AA6C5")); hint.setTextSize(13); hint.setGravity(Gravity.CENTER); root.addView(hint,new LinearLayout.LayoutParams(-1,-2)); setContentView(root);
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setMediaPlaybackRequiresUserGesture(false); s.setAllowFileAccess(true); s.setAllowContentAccess(true); if(Build.VERSION.SDK_INT>=21)s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.setWebViewClient(new WebViewClient()); web.setWebChromeClient(new WebChromeClient(){ View custom; CustomViewCallback cb; public void onShowCustomView(View view,CustomViewCallback callback){ if(custom!=null){callback.onCustomViewHidden();return;} custom=view; cb=callback; setContentView(view);} public void onHideCustomView(){ if(custom==null)return; custom=null; setContentView(root); if(cb!=null)cb.onCustomViewHidden(); }});
        resolveAndPlay();
    }
    void resolveAndPlay(){ if(source.raw!=null&&source.raw.length()>0&&source.raw.contains("var rule")){ engine.runLazy(input,(u,err)->loadPlayer(u)); return; } new AsyncTask<Void,Void,String>(){protected String doInBackground(Void...v){try{return Scraper.resolvePlay(input);}catch(Exception e){return input;}} protected void onPostExecute(String u){loadPlayer(u);}}.execute(); }
    void loadPlayer(String url){ if(url==null||url.length()==0)url=input; hint.setText("播放器加载中... 横屏模式"); web.loadDataWithBaseURL("https://artplayer.org/", html(url,title), "text/html", "UTF-8", null); }
    String q(String s){ return JSONObject.quote(s==null?"":s); }
    String html(String url,String title){
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"+
        "<style>html,body{margin:0;width:100%;height:100%;background:#05070d;color:#fff;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif}.artplayer-app{width:100%;height:100vh}.hint{position:fixed;left:16px;right:16px;bottom:18px;color:#9aa6c5;font-size:13px;text-align:center;z-index:0}</style>"+
        "<script src='https://cdn.jsdelivr.net/npm/hls.js@latest'></script><script src='https://cdn.jsdelivr.net/npm/artplayer/dist/artplayer.js'></script></head>"+
        "<body><div class='artplayer-app'></div><div class='hint'>晓鹏影视播放器加载中...</div><script>"+
        "const videoUrl="+q(url)+";const videoTitle="+q(title)+";"+
        "function playM3u8(video,url,art){if(window.Hls&&Hls.isSupported()){if(art.hls)art.hls.destroy();const hls=new Hls();hls.loadSource(url);hls.attachMedia(video);art.hls=hls;art.on('destroy',()=>hls.destroy());}else if(video.canPlayType('application/vnd.apple.mpegurl')){video.src=url;}else{video.src=url;}}"+
        "const art=new Artplayer({container:'.artplayer-app',url:videoUrl,type:videoUrl.includes('.m3u8')?'m3u8':'',title:videoTitle,autoplay:true,pip:true,autoSize:false,autoMini:false,screenshot:true,setting:true,hotkey:true,fullscreen:true,fullscreenWeb:true,playbackRate:true,aspectRatio:true,theme:'#6B7CFF',customType:{m3u8:playM3u8}});"+
        "art.on('ready',()=>{document.querySelector('.hint').style.display='none'});art.on('error',()=>{document.querySelector('.hint').innerText='播放失败，可返回详情页换线路或换数据源';});"+
        "</script></body></html>";
    }
    public void onBackPressed(){ if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
