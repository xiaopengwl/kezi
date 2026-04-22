package com.example.chiguaapp;

import android.app.*;import android.os.*;import android.content.*;import android.net.*;import android.view.*;import android.widget.*;import java.util.*;

public class DetailActivity extends Activity{
    TextView title,content,status; ProgressBar progress; ListView list; ArrayAdapter<String> adapter; ArrayList<PlayItem> plays=new ArrayList<>(); String url; SourceConfig source; DrpyEngine engine;
    public void onCreate(Bundle b){ super.onCreate(b); url=getIntent().getStringExtra("url");
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16,16,16,16);
        title=new TextView(this); title.setText(getIntent().getStringExtra("title")); title.setTextSize(18); root.addView(title);
        content=new TextView(this); content.setPadding(0,8,0,8); root.addView(content);
        progress=new ProgressBar(this); root.addView(progress); status=new TextView(this); root.addView(status);
        list=new ListView(this); adapter=new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>()); list.setAdapter(adapter); root.addView(list,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        list.setOnItemClickListener((p,v,pos,id)->resolve(plays.get(pos)));
        load(); }
    void render(String t,String c,ArrayList<PlayItem> ps,String prefix){progress.setVisibility(View.GONE); title.setText(t); content.setText(c); plays=ps; ArrayList<String> ns=new ArrayList<>(); for(PlayItem p:plays)ns.add(p.name); adapter.clear(); adapter.addAll(ns); adapter.notifyDataSetChanged(); status.setText(prefix+"播放节点："+plays.size());}
    void load(){ source=SourceConfig.load(this); engine=new DrpyEngine(this,source); if(source.raw!=null&&source.raw.length()>0&&source.raw.contains("var rule")){ engine.runDetail(url,(d,err)->{ if(d==null){progress.setVisibility(View.GONE);status.setText(err);return;} render(d.title,d.content,d.plays,"JS源");}); return;} new AsyncTask<Void,Void,Scraper.Detail>(){ String err=""; protected Scraper.Detail doInBackground(Void...v){try{return Scraper.detail(url);}catch(Exception e){err=e.toString();return null;}} protected void onPostExecute(Scraper.Detail d){ if(d==null){progress.setVisibility(View.GONE);status.setText(err);return;} render(d.title,d.content,d.plays,"内置");}}.execute();}
    void resolve(PlayItem p){ progress.setVisibility(View.VISIBLE); status.setText("解析播放地址..."); source=SourceConfig.load(this); engine=new DrpyEngine(this,source); if(source.raw!=null&&source.raw.length()>0&&source.raw.contains("var rule")){ engine.runLazy(p.input,(u,err)->open(u)); return;} new AsyncTask<Void,Void,String>(){protected String doInBackground(Void...v){try{return Scraper.resolvePlay(p.input);}catch(Exception e){return p.input;}} protected void onPostExecute(String u){open(u);}}.execute(); }
    void open(String u){progress.setVisibility(View.GONE); if(u==null||u.length()<1)u=url; Intent in=new Intent(DetailActivity.this, PlayerActivity.class); in.putExtra("url",u); startActivity(in);}    
}
