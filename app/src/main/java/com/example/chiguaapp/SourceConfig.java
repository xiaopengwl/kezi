package com.example.chiguaapp;

import android.content.*;
import java.util.regex.*;

public class SourceConfig {
    public String raw="";
    public String title="吃瓜网t3-js-drpy-v32-playurl-fix";
    public String host="https://band.nnfndyhn.cc";
    public String className="今日吃瓜&学生校园&网红黑料&热门大瓜&吃瓜榜单&必看大瓜&看片娱乐&每日大赛&伦理道德&国产剧情&网黄合集&探花精选&免费短剧&骚男骚女&明星黑料&海外吃瓜&人人吃瓜&领导干部&吃瓜看戏&擦边聊骚&51涨知识&51品茶&原创博主&51剧场";
    public String classUrl="wpcz&xsxy&whhl&rdsj&mrdg&bkdg&ysyl&mrds&lldd&gcjq&whhj&thjx&cbdj&snsn&whmx&hwcg&rrcg&ldcg&qubk&dcbq&zzs&51by&yczq&51djc";
    public String[] hosts = new String[]{"https://band.nnfndyhn.cc","https://band.bdleshd.cc","https://band.azoxidnu.cc","https://band.atvfvqjv.cc","http://band.atvfvqjv.cc","http://chigua.com","https://chigua.com"};

    public String[] names(){ return className.split("&"); }
    public String[] urls(){ return classUrl.split("&"); }

    public static SourceConfig load(Context c){
        SourceConfig sc=new SourceConfig();
        SharedPreferences sp=c.getSharedPreferences("sources",0);
        String raw=sp.getString("raw","");
        if(raw.length()>0) sc.applyRaw(raw);
        return sc;
    }
    public void save(Context c){
        c.getSharedPreferences("sources",0).edit().putString("raw",raw).apply();
    }
    public void applyRaw(String text){
        raw=text==null?"":text;
        String v;
        v=pick(raw,"title"); if(v.length()>0) title=v;
        v=pick(raw,"host"); if(v.length()>0) host=v;
        v=pick(raw,"class_name"); if(v.length()>0) className=v;
        v=pick(raw,"class_url"); if(v.length()>0) classUrl=v;
        hosts = buildHosts(host, raw);
    }
    static String pick(String s,String key){
        Pattern p=Pattern.compile(key+"\\s*:\\s*(['\"])(.*?)\\1",Pattern.DOTALL);
        Matcher m=p.matcher(s); return m.find()?m.group(2).trim():"";
    }
    static String[] buildHosts(String main,String raw){
        java.util.LinkedHashSet<String> hs=new java.util.LinkedHashSet<>();
        if(main!=null&&main.startsWith("http"))hs.add(main);
        Matcher m=Pattern.compile("https?://band\\.[a-zA-Z0-9.-]+|https?://chigua\\.com").matcher(raw==null?"":raw);
        while(m.find())hs.add(m.group());
        if(hs.isEmpty())hs.add("https://band.nnfndyhn.cc");
        return hs.toArray(new String[0]);
    }
}
