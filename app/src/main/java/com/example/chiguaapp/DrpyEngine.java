package com.example.chiguaapp;

import android.app.*;import android.os.*;import android.webkit.*;import java.io.*;import java.net.*;import java.util.*;import org.json.*;

public class DrpyEngine{
    Activity act; WebView web; SourceConfig source; String lastResult=""; String currentHost="";
    public interface Callback<T>{void done(T data,String err);}
    public DrpyEngine(Activity a, SourceConfig s){act=a; source=s; currentHost=s.host; web=new WebView(a); WebSettings ws=web.getSettings(); ws.setJavaScriptEnabled(true); ws.setDomStorageEnabled(true); web.addJavascriptInterface(new Bridge(),"Android"); web.loadData("<html><body>drpy</body></html>","text/html","utf-8");}
    public class Bridge{
        @JavascriptInterface public String request(String url,String opt){try{return http(abs(url));}catch(Exception e){return "";}}
        @JavascriptInterface public void setResult(String json){lastResult=json==null?"":json;}
        @JavascriptInterface public String log(String s){return s;}
    }
    String baseJs(String input){String raw=source.raw==null?"":source.raw; return "var input="+q(input)+";var MY_PAGE=1;var MY_PAGECOUNT=999;var MY_TOTAL=99999;"+
            "function request(u,o){return Android.request(String(u||''),JSON.stringify(o||{}));}"+
            "function setResult(v){Android.setResult(JSON.stringify(v||[]));}"+
            "function log(v){Android.log(String(v));}"+
            raw+"\n;"+
            "if(typeof rule==='undefined'&&typeof muban!=='undefined'){};";}
    String wrapRun(String input,String key){return "(function(){try{"+baseJs(input)+"var code=rule["+q(key)+"]||''; if(typeof code==='object'){Android.setResult(JSON.stringify(code));return 'ok';} code=String(code); if(code.indexOf('js:')===0)code=code.substring(3); var VOD=null; eval(code); if(typeof VOD!=='undefined'&&VOD){Android.setResult(JSON.stringify(VOD));} return 'ok';}catch(e){Android.setResult(JSON.stringify({error:String(e),stack:e.stack||''}));return 'err:'+e;}})()";}
    public void runList(String key,String input,Callback<ArrayList<VideoItem>> cb){lastResult=""; web.evaluateJavascript(wrapRun(input,key), v->{try{cb.done(parseItems(lastResult),"");}catch(Exception e){cb.done(new ArrayList<>(),e.toString()+" raw="+lastResult);}});}
    public void runDetail(String input,Callback<DetailData> cb){lastResult=""; web.evaluateJavascript(wrapRun(input,"二级"), v->{try{cb.done(parseDetail(lastResult),"");}catch(Exception e){cb.done(null,e.toString()+" raw="+lastResult);}});}
    public void runLazy(String input,Callback<String> cb){lastResult=""; String js="(function(){try{"+baseJs(input)+"var code=rule['lazy']||''; code=String(code); if(code.indexOf('js:')===0)code=code.substring(3); eval(code); if(typeof input==='object'){Android.setResult(JSON.stringify(input));}else{Android.setResult(JSON.stringify({url:String(input||'')}));} return 'ok';}catch(e){Android.setResult(JSON.stringify({url:"+q(input)+",error:String(e)}));return 'err';}})()"; web.evaluateJavascript(js,v->{try{JSONObject o=new JSONObject(lastResult); cb.done(o.optString("url",input),"");}catch(Exception e){cb.done(input,e.toString());}});}
    ArrayList<VideoItem> parseItems(String json)throws Exception{ArrayList<VideoItem> out=new ArrayList<>(); if(json==null||json.length()<1)return out; if(json.trim().startsWith("{")){JSONObject o=new JSONObject(json); if(o.has("error"))throw new RuntimeException(o.optString("error"));}
        JSONArray arr=new JSONArray(json); for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i); String t=first(o,"title","vod_name","name"); String u=first(o,"url","vod_id"); String pic=first(o,"img","vod_pic","pic_url"); String d=first(o,"desc","vod_remarks","remarks"); out.add(new VideoItem(t,u,pic,d));} return out;}
    DetailData parseDetail(String json)throws Exception{JSONObject o=new JSONObject(json); if(o.has("error"))throw new RuntimeException(o.optString("error")); DetailData d=new DetailData(); d.title=first(o,"vod_name","title"); d.content=first(o,"vod_content","content","desc"); d.pic=first(o,"vod_pic","img"); String from=o.optString("vod_play_from","道长在线"); String urls=o.optString("vod_play_url",""); String[] froms=from.length()>0?from.split("\\$\\$\\$"):new String[]{"道长在线"}; String[] groups=urls.length()>0?urls.split("\\$\\$\\$"):new String[0]; for(int gi=0;gi<Math.max(froms.length,groups.length);gi++){ String src=gi<froms.length&&froms[gi].trim().length()>0?froms[gi].trim():("线路"+(gi+1)); String block=gi<groups.length?groups[gi]:""; if(block.length()==0)continue; String[] ps=block.split("#"); for(String p:ps){ if(p==null||p.trim().length()==0)continue; int x=p.indexOf('$'); String name=x>0?p.substring(0,x):p; String input=x>0?p.substring(x+1):p; d.plays.add(new PlayItem(src,name,input)); }} if(d.plays.isEmpty())d.plays.add(new PlayItem("默认线路","打开原页",source.host)); return d;}
    static String first(JSONObject o,String...ks){for(String k:ks){String v=o.optString(k,""); if(v.length()>0)return v;}return "";}
    static String q(String s){return JSONObject.quote(s==null?"":s);}
    String abs(String u){if(u.startsWith("http://")||u.startsWith("https://"))return u; if(u.startsWith("//"))return "https:"+u; if(u.startsWith("/"))return currentHost+u; return currentHost+"/"+u;}
    String http(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(15000); c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"); c.setRequestProperty("Referer",currentHost+"/"); InputStream is=c.getResponseCode()>=400?c.getErrorStream():c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[8192]; int n; while((n=is.read(b))>0)out.write(b,0,n); return out.toString("UTF-8");}
    public static class DetailData{public String title="详情",content="",pic=""; public ArrayList<PlayItem> plays=new ArrayList<>();}
}
