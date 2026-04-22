package com.example.chiguaapp;

import android.app.*;import android.os.*;import android.webkit.*;import android.view.*;import android.widget.*;import android.graphics.*;import org.json.JSONObject;

public class PlayerActivity extends Activity{
    WebView web;
    public void onCreate(Bundle b){
        super.onCreate(b);
        String url=getIntent().getStringExtra("url"); if(url==null)url="";
        String title=getIntent().getStringExtra("title"); if(title==null)title="晓鹏影视";
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.BLACK);
        TextView bar=new TextView(this); bar.setText("  晓鹏影视 · "+title); bar.setTextColor(Color.WHITE); bar.setTextSize(16); bar.setSingleLine(true); bar.setPadding(0,10,0,10); bar.setBackgroundColor(Color.parseColor("#0B1020")); root.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        web=new WebView(this); root.addView(web,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setMediaPlaybackRequiresUserGesture(false); s.setAllowFileAccess(true); s.setAllowContentAccess(true); if(Build.VERSION.SDK_INT>=21)s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient(){
            View custom; CustomViewCallback cb;
            public void onShowCustomView(View view, CustomViewCallback callback){ if(custom!=null){callback.onCustomViewHidden();return;} custom=view; cb=callback; setContentView(view); }
            public void onHideCustomView(){ if(custom==null)return; custom=null; setContentView(root); if(cb!=null)cb.onCustomViewHidden(); }
        });
        web.loadDataWithBaseURL("https://artplayer.org/", html(url,title), "text/html", "UTF-8", null);
    }
    String q(String s){ return JSONObject.quote(s==null?"":s); }
    String html(String url,String title){
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"+
        "<style>html,body{margin:0;width:100%;height:100%;background:#05070d;color:#fff;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif}.artplayer-app{width:100%;height:100vh}.hint{position:fixed;left:16px;right:16px;bottom:18px;color:#9aa6c5;font-size:13px;text-align:center;z-index:0}</style>"+
        "<script src='https://cdn.jsdelivr.net/npm/hls.js@latest'></script><script src='https://cdn.jsdelivr.net/npm/artplayer/dist/artplayer.js'></script></head>"+
        "<body><div class='artplayer-app'></div><div class='hint'>晓鹏影视播放器加载中...</div><script>"+
        "const videoUrl="+q(url)+";const videoTitle="+q(title)+";"+
        "function playM3u8(video,url,art){if(Hls.isSupported()){if(art.hls)art.hls.destroy();const hls=new Hls();hls.loadSource(url);hls.attachMedia(video);art.hls=hls;art.on('destroy',()=>hls.destroy());}else if(video.canPlayType('application/vnd.apple.mpegurl')){video.src=url;}else{video.src=url;}}"+
        "const art=new Artplayer({container:'.artplayer-app',url:videoUrl,type:videoUrl.includes('.m3u8')?'m3u8':'',title:videoTitle,poster:'',autoplay:true,pip:true,autoSize:false,autoMini:true,screenshot:true,setting:true,hotkey:true,fullscreen:true,fullscreenWeb:true,playbackRate:true,aspectRatio:true,theme:'#6B7CFF',customType:{m3u8:playM3u8}});"+
        "art.on('ready',()=>{document.querySelector('.hint').style.display='none'});art.on('error',()=>{document.querySelector('.hint').innerText='播放失败，可返回换线路或换源';});"+
        "</script></body></html>";
    }
    public void onBackPressed(){ if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
