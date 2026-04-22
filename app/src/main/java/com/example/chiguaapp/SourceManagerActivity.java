package com.example.chiguaapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class SourceManagerActivity extends Activity {
    EditText editor;
    TextView status;
    TextView repoBadge;
    ListView list;
    ArrayAdapter<String> adapter;
    ArrayList<SourceConfig> sources;
    int active;

    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);} 
    GradientDrawable bg(int color,float r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    GradientDrawable stroke(int color,String sc,float r){GradientDrawable g=bg(color,r);g.setStroke(dp(1),Color.parseColor(sc));return g;}
    TextView tv(String s,int sp,int c,int style){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setTypeface(Typeface.DEFAULT,style);return t;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(16),dp(16),dp(16));
        root.setBackgroundColor(Color.parseColor("#0B1020"));

        LinearLayout titleRow=new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=tv("源管理",26,Color.WHITE,Typeface.BOLD);
        titleRow.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        repoBadge=tv("xiaomaojs 仓库源 · "+RepoSourceCatalog.ITEMS.length,12,Color.parseColor("#DDE5FF"),Typeface.BOLD);
        repoBadge.setGravity(Gravity.CENTER);
        repoBadge.setPadding(dp(12),0,dp(12),0);
        repoBadge.setBackground(stroke(Color.parseColor("#1A2340"),"#5B6CFF",18));
        titleRow.addView(repoBadge,new LinearLayout.LayoutParams(-2,dp(34)));
        root.addView(titleRow);

        TextView tip=tv("支持手动粘贴，也支持直接从 xiaopengwl/xiaomaojs 仓库一键导入常用 drpy/t3-js 源。",13,Color.parseColor("#AEB9D6"),Typeface.NORMAL);
        tip.setPadding(0,dp(6),0,dp(12));
        root.addView(tip);

        list=new ListView(this);
        list.setDivider(null);
        list.setCacheColorHint(Color.TRANSPARENT);
        list.setSelector(android.R.color.transparent);
        root.addView(list,new LinearLayout.LayoutParams(-1,dp(164)));

        editor=new EditText(this);
        editor.setMinLines(10);
        editor.setGravity(Gravity.TOP|Gravity.START);
        editor.setHint("粘贴 var rule = {...} 源内容，或点下方按钮从仓库导入");
        editor.setTextColor(Color.WHITE);
        editor.setHintTextColor(Color.parseColor("#7785A6"));
        editor.setTextSize(13);
        editor.setPadding(dp(12),dp(12),dp(12),dp(12));
        editor.setBackground(stroke(Color.parseColor("#151B2E"),"#2E3B5D",16));
        root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout row0=new LinearLayout(this);
        row0.setOrientation(LinearLayout.HORIZONTAL);
        row0.setPadding(0,dp(10),0,0);
        Button importRepo=btn("导入仓库源", "#5B6CFF");
        row0.addView(importRepo,new LinearLayout.LayoutParams(0,dp(46),1));
        Button restoreRepo=btn("恢复仓库默认", "#24315F");
        LinearLayout.LayoutParams lp0=new LinearLayout.LayoutParams(0,dp(46),1); lp0.leftMargin=dp(8);
        row0.addView(restoreRepo,lp0);
        root.addView(row0);

        LinearLayout row1=new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0,dp(8),0,0);
        Button add=btn("新增源", "#2B355A"); row1.addView(add,new LinearLayout.LayoutParams(0,dp(46),1));
        Button save=btn("保存当前", "#2B355A"); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1); lp.leftMargin=dp(8); row1.addView(save,lp);
        root.addView(row1);

        LinearLayout row2=new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0,dp(8),0,0);
        Button use=btn("设为当前", "#2B355A"); row2.addView(use,new LinearLayout.LayoutParams(0,dp(44),1));
        Button del=btn("删除当前", "#4B2331"); LinearLayout.LayoutParams lp2=new LinearLayout.LayoutParams(0,dp(44),1); lp2.leftMargin=dp(8); row2.addView(del,lp2);
        Button reset=btn("恢复默认", "#2B355A"); LinearLayout.LayoutParams lp3=new LinearLayout.LayoutParams(0,dp(44),1); lp3.leftMargin=dp(8); row2.addView(reset,lp3);
        root.addView(row2);

        status=tv("",13,Color.parseColor("#9BA8C8"),Typeface.NORMAL);
        status.setPadding(0,dp(10),0,0);
        root.addView(status);
        setContentView(root);

        refreshList();

        list.setOnItemClickListener((p,v,pos,id)->{
            active=pos;
            SourceConfig.setActive(this,active);
            refreshList();
            editor.setText(sources.get(active).raw);
            status.setText("已选择："+sources.get(active).title);
            setResult(RESULT_OK);
        });
        importRepo.setOnClickListener(v->showRepoPicker());
        restoreRepo.setOnClickListener(v->importRepoItem(0,false,true));
        add.setOnClickListener(v->{ String raw=editor.getText().toString().trim(); if(raw.length()<10){ status.setText("请先粘贴源内容再新增"); return;} SourceConfig.addSource(this,raw); setResult(RESULT_OK); refreshList(); status.setText("已新增源"); });
        save.setOnClickListener(v->{ String raw=editor.getText().toString().trim(); if(raw.length()<10){ status.setText("源内容为空，未保存"); return;} SourceConfig.updateActive(this,raw); setResult(RESULT_OK); refreshList(); status.setText("已保存当前源"); });
        use.setOnClickListener(v->{ SourceConfig.setActive(this,active); setResult(RESULT_OK); refreshList(); status.setText("已设为当前源："+sources.get(active).title); });
        del.setOnClickListener(v->{ SourceConfig.deleteActive(this); setResult(RESULT_OK); refreshList(); status.setText("已删除；如只剩一个源则自动恢复默认源"); });
        reset.setOnClickListener(v->{ getSharedPreferences(SourceConfig.PREF,0).edit().clear().putInt("count",1).putInt("active",0).putString("src_0",SourceConfig.defaultRaw()).apply(); setResult(RESULT_OK); refreshList(); status.setText("已恢复默认源"); });
    }

    Button btn(String s,String color){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setAllCaps(false); b.setBackground(bg(Color.parseColor(color),16)); return b; }

    void refreshList(){
        sources=SourceConfig.loadAll(this);
        active=SourceConfig.activeIndex(this);
        if(active<0||active>=sources.size())active=0;
        ArrayList<String> names=new ArrayList<>();
        for(int i=0;i<sources.size();i++){
            SourceConfig sc=sources.get(i);
            names.add((i==active?"✓ 当前  ":"   ")+sc.title+"\n"+sc.host);
        }
        adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, names){
            @Override public View getView(int pos,View convert,android.view.ViewGroup parent){
                View v=super.getView(pos,convert,parent);
                TextView t1=v.findViewById(android.R.id.text1);
                TextView t2=v.findViewById(android.R.id.text2);
                t1.setTextColor(Color.WHITE);
                t1.setTextSize(15);
                t2.setTextColor(Color.parseColor("#95A3C4"));
                t2.setTextSize(12);
                v.setBackgroundColor(Color.TRANSPARENT);
                return v;
            }
        };
        list.setAdapter(adapter);
        editor.setText(sources.get(active).raw);
        status.setText("当前共有 "+sources.size()+" 个源，可直接导入仓库源");
    }

    void showRepoPicker(){
        String[] names=new String[RepoSourceCatalog.ITEMS.length];
        for(int i=0;i<RepoSourceCatalog.ITEMS.length;i++) names[i]=RepoSourceCatalog.ITEMS[i].toString();
        new AlertDialog.Builder(this)
                .setTitle("导入 xiaomaojs 仓库源")
                .setItems(names,(d,which)->importRepoItem(which,true,false))
                .setNegativeButton("取消",null)
                .show();
    }

    void importRepoItem(int which, boolean addNew, boolean replaceAll){
        if(which<0||which>=RepoSourceCatalog.ITEMS.length) return;
        RepoSourceCatalog.Item item=RepoSourceCatalog.ITEMS[which];
        status.setText("正在拉取："+item.title);
        new AsyncTask<Void,Void,String>(){
            String err="";
            @Override protected String doInBackground(Void... v){
                try{return http(item.rawUrl);}catch(Exception e){err=e.toString();return "";}
            }
            @Override protected void onPostExecute(String raw){
                if(raw==null||raw.trim().length()<10){
                    status.setText("导入失败："+(err.length()>0?err:"内容为空"));
                    return;
                }
                editor.setText(raw);
                if(replaceAll){
                    getSharedPreferences(SourceConfig.PREF,0).edit().clear().putInt("count",1).putInt("active",0).putString("src_0",raw).apply();
                    active=0;
                    status.setText("已恢复为仓库默认源："+item.title);
                }else if(addNew){
                    SourceConfig.addSource(SourceManagerActivity.this,raw);
                    status.setText("已导入仓库源："+item.title);
                }else{
                    SourceConfig.updateActive(SourceManagerActivity.this,raw);
                    status.setText("已替换当前源为："+item.title);
                }
                setResult(RESULT_OK,new Intent());
                refreshList();
            }
        }.execute();
    }

    String http(String url)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent","Mozilla/5.0");
        InputStream in=c.getResponseCode()>=400?c.getErrorStream():c.getInputStream();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buf=new byte[8192];
        int n;
        while((n=in.read(buf))>0) out.write(buf,0,n);
        return out.toString("UTF-8");
    }
}
