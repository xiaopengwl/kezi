package com.example.chiguaapp;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class DrpyEngine {
    Activity act;
    WebView web;
    SourceConfig source;
    String lastResult = "";
    String currentHost = "";

    public interface Callback<T> { void done(T data, String err); }

    public DrpyEngine(Activity a, SourceConfig s) {
        act = a;
        source = s;
        currentHost = s.host;
        web = new WebView(a);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadData("<html><body>drpy</body></html>", "text/html", "utf-8");
    }

    public class Bridge {
        @JavascriptInterface
        public String request(String url, String opt) {
            try {
                return requestRaw(abs(url), parseHttpOptions(opt, abs(url))).body;
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public void setResult(String json) {
            lastResult = json == null ? "" : json;
        }

        @JavascriptInterface
        public String log(String s) {
            return s;
        }
    }

    String baseJs(String input) {
        String raw = source.raw == null ? "" : source.raw;
        return "var input=" + q(input) + ";var MY_PAGE=1;var MY_PAGECOUNT=999;var MY_TOTAL=99999;"
                + "var MOBILE_UA='Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36';"
                + "var PC_UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36';"
                + "var HOST='" + js(source.host) + "';var rule_fetch_params={headers:{}};var fetch_params={headers:{}};"
                + "var document={html:''};var localStore={};function getItem(k){return localStore[k]||'';}function setItem(k,v){localStore[k]=String(v||'');}"
                + "var jsp={};var $={};var $js={toString:function(fn){var s=String(fn);var a=s.indexOf('{'),b=s.lastIndexOf('}');return 'js:'+(a>=0&&b>a?s.substring(a+1,b):s);}};"
                + "function mergeHeaders(){var out={},arr=[rule&&rule.headers,rule_fetch_params&&rule_fetch_params.headers,fetch_params&&fetch_params.headers];for(var i=0;i<arr.length;i++){var hs=arr[i]||{};for(var k in hs){if(Object.prototype.hasOwnProperty.call(hs,k)&&hs[k]!=null&&String(hs[k]).length>0)out[k]=String(hs[k]);}}return out;}"
                + "function mergeReqOpt(opt){var cfg=opt&&typeof opt==='object'?opt:{};var merged={};for(var k in cfg){if(Object.prototype.hasOwnProperty.call(cfg,k))merged[k]=cfg[k];}merged.headers=mergeHeaders();if(cfg.headers){for(var hk in cfg.headers){if(Object.prototype.hasOwnProperty.call(cfg.headers,hk))merged.headers[hk]=String(cfg.headers[hk]);}}if(!merged.headers['User-Agent'])merged.headers['User-Agent']=MOBILE_UA;return merged;}"
                + "function request(url,opt){var cfg=mergeReqOpt(opt||{});var r=Android.request(String(url||''),JSON.stringify(cfg||{}));document.html=r;return r;}"
                + "function requestRaw(url,opt){return request(url,opt);}function fetch(u,o){return request(u,o);}function post(u,o){return request(u,o);}function getHtml(u,o){return request(u,o);}"
                + "function setResult(v){Android.setResult(JSON.stringify(v||[]));}function setResult2(v){setResult(v);}function log(v){Android.log(String(v));}"
                + jsRuntime() + raw + "\n;";
    }

    String wrapRun(String input, String key) {
        return "(function(){try{" + baseJs(input)
                + "var code=rule[" + q(key) + "]||'';var real=realInput(" + q(key) + ");"
                + "if(typeof code==='object'){var html=request(real,{headers:(rule.headers||{})});Android.setResult(JSON.stringify(parseDetailObj(html,code)));return 'ok';}"
                + "code=String(code);"
                + "if(code.indexOf('js:')===0){code=code.substring(3);eval(code);if(typeof VODS!=='undefined')Android.setResult(JSON.stringify(VODS));else if(typeof VOD!=='undefined'&&VOD)Android.setResult(JSON.stringify(VOD));return 'ok';}"
                + "var html=request(real,{headers:(rule.headers||{})});Android.setResult(JSON.stringify(parseListSel(html,code)));return 'ok';"
                + "}catch(e){Android.setResult(JSON.stringify({error:String(e),stack:e.stack||''}));return 'err:'+e;}})()";
    }

    public void runList(String key, String input, Callback<ArrayList<VideoItem>> cb) {
        lastResult = "";
        web.evaluateJavascript(wrapRun(input, key), v -> {
            try {
                cb.done(parseItems(lastResult), "");
            } catch (Exception e) {
                cb.done(new ArrayList<>(), e.toString() + " raw=" + lastResult);
            }
        });
    }

    public void runDetail(String input, Callback<DetailData> cb) {
        lastResult = "";
        web.evaluateJavascript(wrapRun(input, "二级"), v -> {
            try {
                cb.done(parseDetail(lastResult, input), "");
            } catch (Exception e) {
                cb.done(null, e.toString() + " raw=" + lastResult);
            }
        });
    }

    public void runLazy(String input, Callback<String> cb) {
        lastResult = "";
        String js = "(function(){try{" + baseJs(input)
                + "document.html=request(input,{headers:(rule.headers||{})});var code=rule['lazy']||'';code=String(code);if(code.indexOf('js:')===0)code=code.substring(3);eval(code);"
                + "if(typeof input==='object'){Android.setResult(JSON.stringify(input));}else{Android.setResult(JSON.stringify({url:String(input||'')}));}return 'ok';"
                + "}catch(e){Android.setResult(JSON.stringify({url:" + q(input) + ",error:String(e)}));return 'err';}})()";
        web.evaluateJavascript(js, v -> {
            try {
                JSONObject o = new JSONObject(lastResult);
                cb.done(o.optString("url", input), o.optString("error", ""));
            } catch (Exception e) {
                cb.done(input, e.toString());
            }
        });
    }

    static String jsRuntime() {
        return "if(!String.prototype.strip){String.prototype.strip=function(){return String(this).trim();};}"
                + "function jsonParseSafe(s,d){try{return JSON.parse(s);}catch(e){return d||{};}}"
                + "function normalizeUrl(rel,base){try{return new URL(String(rel||''), String(base||rule.host||HOST)).toString();}catch(e){rel=String(rel||'');if(/^https?:/i.test(rel))return rel;if(rel.indexOf('//')===0)return 'https:'+rel;if(rel.charAt(0)==='/')return String(base||rule.host||HOST).replace(/\\/$/,'')+rel;return String(base||rule.host||HOST).replace(/\\/$/,'')+'/'+rel.replace(/^\\//,'');}}"
                + "function absu(u,base){u=String(u||'');if(!u)return '';return normalizeUrl(u, base||rule.host||HOST);}"
                + "function buildUrl(u,base){return absu(u,base);}function urljoin(a,b){return absu(b||a,a||rule.host||HOST);}function getHome(){return rule.host||HOST;}function getHost(){return rule.host||HOST;}"
                + "function getHtml(url,headers){return request(url,{headers:headers||rule.headers||{}});}"
                + "function realInput(k){var p=input,m;if(k==='搜索'){var su=rule.searchUrl||'';m=p.match(/\\/search\\/([^\\/]+)\\/(\\d+)/);var kw=m?decodeURIComponent(m[1]):'';var pg=m?m[2]:'1';return absu(su.replace('**',encodeURIComponent(kw)).replace('fypage',pg), rule.host||HOST);}if(k==='一级'){m=p.match(/\\/category\\/([^\\/]+)\\/(\\d+)/);var cl=m?m[1]:'';var pg=m?m[2]:'1';return absu((rule.url||'/').replace('fyclass',cl).replace('fypage',pg).replace('fyfilter',''), rule.host||HOST);}return absu(p&&p!=='/'?p:(rule.homeUrl||rule.host), rule.host||HOST);}"
                + "function parseDom(html){return new DOMParser().parseFromString(String(html||''),'text/html');}var documentObj=parseDom('');"
                + "function qsa(ctx,sel){try{sel=String(sel||'').replace(/:eq\\(([-\\d#]+)\\)/g,function(_,n){return n==='#id'?'':':nth-child('+(parseInt(n)+1)+')'});return Array.prototype.slice.call((ctx||documentObj).querySelectorAll(sel));}catch(e){return[];}}"
                + "function one(ctx,sel){var m=String(sel||'').match(/:eq\\((-?\\d+)\\)/);var clean=String(sel||'').replace(/:eq\\(-?\\d+\\)/g,'');var a=qsa(ctx,clean);if(m){var i=parseInt(m[1]);if(i<0)i=a.length+i;return a[i]||null;}return a[0]||null;}"
                + "function nodeText(node){return node?(node.textContent||'').replace(/\\s+/g,' ').trim():'';}"
                + "function resolveExpr(base, expr){var parts=String(expr||'').split('&&');var node=base||documentObj;var attr=parts.length>1?parts[parts.length-1]:'Text';var sel=parts.length>1?parts[0]:'body';if(sel&&sel!=='body'&&sel!=='a'&&sel!=='img'&&sel!=='script')node=one(base||documentObj,sel)||node;if(attr==='Text')return nodeText(node);if(attr==='Html')return node?node.innerHTML||'':'';return node&&node.getAttribute?node.getAttribute(attr)||'':'';}"
                + "function pdfh(html,expr){var doc=(typeof html==='string')?parseDom(html):html;return resolveExpr(doc,expr||'body&&Text');}"
                + "function pdfa(html,sel){var doc=(typeof html==='string')?parseDom(html):html;return qsa(doc,sel||'body');}"
                + "function pd(html,expr,nurl){var v=pdfh(html,expr);return nurl?urljoin(nurl,v):absu(v, rule.host||HOST);}"
                + "function pq(html){documentObj=(typeof html==='string')?parseDom(html):html;return documentObj;}"
                + "function parseListSel(html,ruleStr){documentObj=parseDom(html);var p=String(ruleStr||'').split(';');var nodes=qsa(documentObj,p[0]||'body');return nodes.map(function(n){return{title:resolveExpr(n,p[1]),img:absu(resolveExpr(n,p[2]), rule.host||HOST),desc:resolveExpr(n,p[3]),url:absu(resolveExpr(n,p[4]), rule.host||HOST)};}).filter(function(x){return x.title&&x.url;});}"
                + "function parseDetailObj(html,o){documentObj=parseDom(html);var title=resolveExpr(documentObj,o.title||'')||'';var img=absu(resolveExpr(documentObj,o.img||''), rule.host||HOST);var desc=resolveExpr(documentObj,o.desc||'');var content=resolveExpr(documentObj,o.content||'');var tabs=qsa(documentObj,o.tabs||'body');var from=[],groups=[];for(var i=0;i<tabs.length;i++){from.push(resolveExpr(tabs[i],o.tab_text||'body&&Text')||('线路'+(i+1)));var listSel=String(o.lists||'').replace('#id',i);var its=qsa(documentObj,listSel),arr=[];for(var j=0;j<its.length;j++){var nm=resolveExpr(its[j],o.list_text||'body&&Text')||('第'+(j+1)+'集');var u=absu(resolveExpr(its[j],o.list_url||'a&&href')||resolveExpr(its[j],'a&&href'), rule.host||HOST);arr.push(nm+'$'+u);}groups.push(arr.join('#'));}return{vod_name:title,vod_pic:img,vod_remarks:desc,vod_content:content,vod_play_from:from.join('$$$'),vod_play_url:groups.join('$$$')};}";
    }

    ArrayList<VideoItem> parseItems(String json) throws Exception {
        ArrayList<VideoItem> out = new ArrayList<>();
        if (json == null || json.length() < 1) return out;
        if (json.trim().startsWith("{")) {
            JSONObject o = new JSONObject(json);
            if (o.has("error")) throw new RuntimeException(o.optString("error") + " " + o.optString("stack"));
        }
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String t = first(o, "title", "vod_name", "name");
            String u = first(o, "url", "vod_id");
            String pic = first(o, "img", "vod_pic", "pic_url");
            String d = first(o, "desc", "vod_remarks", "remarks");
            out.add(new VideoItem(t, u, pic, d));
        }
        return out;
    }

    DetailData parseDetail(String json, String fallbackInput) throws Exception {
        json = json == null ? "" : json.trim();
        JSONObject o;
        if (json.startsWith("[")) {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) throw new RuntimeException("详情为空");
            o = arr.getJSONObject(0);
        } else {
            o = new JSONObject(json);
        }
        if (o.has("error")) throw new RuntimeException(o.optString("error") + " " + o.optString("stack"));
        DetailData d = new DetailData();
        d.title = first(o, "vod_name", "title");
        d.content = first(o, "vod_content", "content", "desc");
        d.pic = first(o, "vod_pic", "img");
        String from = o.optString("vod_play_from", "道长在线");
        String urls = o.optString("vod_play_url", "");
        String[] froms = from.length() > 0 ? from.split("\\$\\$\\$") : new String[]{"道长在线"};
        String[] groups = urls.length() > 0 ? urls.split("\\$\\$\\$") : new String[0];
        for (int gi = 0; gi < Math.max(froms.length, groups.length); gi++) {
            String src = gi < froms.length && froms[gi].trim().length() > 0 ? froms[gi].trim() : ("线路" + (gi + 1));
            String block = gi < groups.length ? groups[gi] : "";
            if (block.length() == 0) continue;
            String[] ps = block.split("#");
            for (String p : ps) {
                if (p == null || p.trim().length() == 0) continue;
                int x = p.indexOf('$');
                String name = x > 0 ? p.substring(0, x) : p;
                String playInput = x > 0 ? p.substring(x + 1) : p;
                d.plays.add(new PlayItem(src, name, playInput));
            }
        }
        if (d.plays.isEmpty()) d.plays.add(new PlayItem("网页嗅探", "嗅探播放", fallbackInput));
        return d;
    }

    static String first(JSONObject o, String... ks) {
        for (String k : ks) {
            String v = o.optString(k, "");
            if (v.length() > 0) return v;
        }
        return "";
    }

    static String q(String s) { return JSONObject.quote(s == null ? "" : s); }
    static String js(String s) { return (s == null ? "" : s).replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n"); }

    String abs(String u) {
        if (u == null || u.length() == 0) return currentHost;
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        if (u.startsWith("//")) return "https:" + u;
        if (u.startsWith("/")) return currentHost + u;
        return currentHost + "/" + u;
    }

    HttpOptions parseHttpOptions(String opt, String requestUrl) {
        HttpOptions out = new HttpOptions();
        out.referer = currentHost + "/";
        if (opt == null || opt.trim().length() == 0) return out;
        try {
            JSONObject o = new JSONObject(opt);
            out.method = o.optString("method", out.method);
            out.body = first(o, "body", "data", "postData");
            out.userAgent = first(o, "ua", "userAgent", "User-Agent");
            out.contentType = first(o, "contentType", "mime", "Content-Type");
            int timeout = o.optInt("timeout", 0);
            if (timeout > 0) {
                out.connectTimeout = timeout;
                out.readTimeout = timeout;
            }
            String referer = first(o, "referer", "Referer");
            if (referer.length() > 0) out.referer = referer;
            JSONObject headers = o.optJSONObject("headers");
            if (headers != null) {
                Iterator<String> keys = headers.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = headers.optString(key, "");
                    if (value.length() > 0) out.headers.put(key, value);
                }
            }
            if (out.userAgent.length() == 0 && out.headers.containsKey("User-Agent")) out.userAgent = out.headers.get("User-Agent");
            if (out.contentType.length() == 0 && out.headers.containsKey("Content-Type")) out.contentType = out.headers.get("Content-Type");
        } catch (Exception ignored) {}
        if (out.referer.length() == 0) out.referer = currentHost + "/";
        return out;
    }

    HttpResult requestRaw(String u, HttpOptions options) throws Exception {
        HttpOptions opt = options == null ? new HttpOptions() : options;
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(opt.connectTimeout);
        c.setReadTimeout(opt.readTimeout);
        String ua = opt.userAgent.length() > 0 ? opt.userAgent : "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36";
        c.setRequestProperty("User-Agent", ua);
        c.setRequestProperty("Referer", opt.referer.length() > 0 ? opt.referer : currentHost + "/");
        for (Map.Entry<String, String> entry : opt.headers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().length() == 0) continue;
            c.setRequestProperty(entry.getKey(), entry.getValue());
        }
        String method = opt.method == null || opt.method.trim().length() == 0 ? "GET" : opt.method.trim().toUpperCase();
        if ((opt.body != null && opt.body.length() > 0) || "POST".equals(method)) {
            c.setDoOutput(true);
            c.setRequestMethod("POST".equals(method) ? "POST" : method);
            if (opt.contentType.length() > 0) c.setRequestProperty("Content-Type", opt.contentType);
            if (opt.body != null && opt.body.length() > 0) c.getOutputStream().write(opt.body.getBytes("UTF-8"));
        } else {
            c.setRequestMethod(method);
        }
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (is != null) {
            byte[] b = new byte[8192];
            int n;
            while ((n = is.read(b)) > 0) out.write(b, 0, n);
            is.close();
        }
        HttpResult result = new HttpResult();
        result.body = out.toString("UTF-8");
        result.finalUrl = c.getURL().toString();
        result.contentType = c.getContentType() == null ? "" : c.getContentType();
        return result;
    }

    String http(String u) throws Exception {
        return requestRaw(u, new HttpOptions()).body;
    }

    static class HttpOptions {
        String method = "GET";
        String body = "";
        String referer = "";
        String userAgent = "";
        String contentType = "";
        int connectTimeout = 15000;
        int readTimeout = 15000;
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
    }

    static class HttpResult {
        String body = "";
        String finalUrl = "";
        String contentType = "";
    }

    public static class DetailData {
        public String title = "详情", content = "", pic = "";
        public ArrayList<PlayItem> plays = new ArrayList<>();
    }
}
