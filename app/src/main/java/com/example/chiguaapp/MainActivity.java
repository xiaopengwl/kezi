package com.example.chiguaapp;

import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private Spinner spinner;
    private EditText searchBox;
    private Button loadBtn, searchBtn, prevBtn, nextBtn;
    private ListView listView;
    private ProgressBar progress;
    private TextView status;
    private SourceConfig source;
    private DrpyEngine engine;
    private ArrayAdapter<String> adapter;
    private ArrayList<VideoItem> items = new ArrayList<>();
    private int page = 1;
    private String currentClass = "home";
    private String currentKeyword = "";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16,16,16,16);
        source = SourceConfig.load(this); Scraper.useSource(source); engine=new DrpyEngine(this,source);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this); title.setText(source.title); title.setTextSize(20); title.setPadding(0,0,0,12); top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button srcBtn = new Button(this); srcBtn.setText("源管理"); top.addView(srcBtn); root.addView(top);
        spinner = new Spinner(this); spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, source.names())); root.addView(spinner);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        loadBtn = new Button(this); loadBtn.setText("加载分类"); row.addView(loadBtn, new LinearLayout.LayoutParams(0, -2, 1));
        prevBtn = new Button(this); prevBtn.setText("上一页"); row.addView(prevBtn, new LinearLayout.LayoutParams(0, -2, 1));
        nextBtn = new Button(this); nextBtn.setText("下一页"); row.addView(nextBtn, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(row);
        LinearLayout srow = new LinearLayout(this); srow.setOrientation(LinearLayout.HORIZONTAL);
        searchBox = new EditText(this); searchBox.setHint("搜索关键词"); srow.addView(searchBox, new LinearLayout.LayoutParams(0, -2, 1));
        searchBtn = new Button(this); searchBtn.setText("搜索"); srow.addView(searchBtn); root.addView(srow);
        progress = new ProgressBar(this); progress.setVisibility(View.GONE); root.addView(progress);
        status = new TextView(this); status.setText("准备加载首页"); status.setPadding(0,8,0,8); root.addView(status);
        listView = new ListView(this); adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, android.R.id.text1, new ArrayList<String>());
        listView.setAdapter(adapter); root.addView(listView, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        loadBtn.setOnClickListener(v -> { page=1; currentKeyword=""; currentClass=source.urls()[spinner.getSelectedItemPosition()]; load(); });
        searchBtn.setOnClickListener(v -> { page=1; currentKeyword=searchBox.getText().toString().trim(); currentClass="search"; load(); });
        prevBtn.setOnClickListener(v -> { if(page>1){ page--; load(); } });
        nextBtn.setOnClickListener(v -> { page++; load(); });
        srcBtn.setOnClickListener(v -> startActivityForResult(new Intent(this, SourceManagerActivity.class), 9));
        listView.setOnItemClickListener((p,v,pos,id) -> {
            VideoItem it = items.get(pos);
            Intent in = new Intent(this, DetailActivity.class);
            in.putExtra("url", it.url); in.putExtra("title", it.title); startActivity(in);
        });
        loadHome();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==9){ source=SourceConfig.load(this); Scraper.useSource(source); engine=new DrpyEngine(this,source); spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, source.names())); page=1; loadHome(); }
    }

    private void loadHome(){ page=1; currentClass="home"; currentKeyword=""; load(); }
    private void load(){
        progress.setVisibility(View.VISIBLE); status.setText("加载中，第 "+page+" 页...");
        if(source.raw!=null && source.raw.length()>0 && source.raw.contains("var rule")){
            String key="home".equals(currentClass)?"推荐":("search".equals(currentClass)?"搜索":"一级");
            String input="/";
            if("search".equals(currentClass)) input="/search/"+currentKeyword+"/"+page+"/";
            else if(!"home".equals(currentClass)) input="/category/"+currentClass+"/"+page+"/";
            engine.runList(key,input,(rs,err)->{ progress.setVisibility(View.GONE); items=rs; ArrayList<String> lines=new ArrayList<>(); for(VideoItem it:rs) lines.add(it.title+"\n"+it.desc); adapter.clear(); adapter.addAll(lines); adapter.notifyDataSetChanged(); status.setText(err.length()>0?err:("JS源 · 共 "+rs.size()+" 条 · 第 "+page+" 页")); });
            return;
        }
        new AsyncTask<Void,Void,ArrayList<VideoItem>>(){ String err="";
            protected ArrayList<VideoItem> doInBackground(Void... v){ try{
                if("home".equals(currentClass)) return Scraper.home();
                if("search".equals(currentClass)) return Scraper.search(currentKeyword, page);
                return Scraper.category(currentClass, page);
            }catch(Exception e){ err=e.toString(); return new ArrayList<>(); }}
            protected void onPostExecute(ArrayList<VideoItem> rs){ progress.setVisibility(View.GONE); items=rs; ArrayList<String> lines=new ArrayList<>(); for(VideoItem it:rs) lines.add(it.title+"\n"+it.desc); adapter.clear(); adapter.addAll(lines); adapter.notifyDataSetChanged(); status.setText(err.length()>0?err:("内置解析 · 共 "+rs.size()+" 条 · 第 "+page+" 页")); }
        }.execute();
    }
}
