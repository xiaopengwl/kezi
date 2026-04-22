package com.example.chiguaapp;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout categoryBar;
    private EditText searchBox;
    private Button prevBtn, nextBtn, sourceBtn, homeBtn;
    private ListView listView;
    private ProgressBar progress;
    private TextView status, subTitle, pageBadge;
    private SourceConfig source;
    private DrpyEngine engine;
    private VideoAdapter adapter;
    private ArrayList<VideoItem> items = new ArrayList<>();
    private int page = 1;
    private String currentClass = "home";
    private String currentKeyword = "";

    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    TextView tv(String text,int sp,int color,int style){ TextView t=new TextView(this); t.setText(text); t.setTextSize(sp); t.setTextColor(color); t.setTypeface(Typeface.DEFAULT,style); return t; }
    GradientDrawable bg(int color,float radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    GradientDrawable strokeBg(int color,int stroke,String strokeColor,float radius){ GradientDrawable g=bg(color,radius); g.setStroke(dp(stroke), Color.parseColor(strokeColor)); return g; }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        source = SourceConfig.load(this); Scraper.useSource(source); engine = new DrpyEngine(this, source);

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.parseColor("#0B1020"));

        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setPadding(dp(18),dp(18),dp(18),dp(14));
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.parseColor("#202A55"), Color.parseColor("#111827"), Color.parseColor("#090D18")});
        hero.setBackground(heroBg);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = tv("晓鹏影视",28,Color.WHITE,Typeface.BOLD); top.addView(logo,new LinearLayout.LayoutParams(0,-2,1));
        sourceBtn = new Button(this); sourceBtn.setText("源管理"); sourceBtn.setTextColor(Color.WHITE); sourceBtn.setBackground(strokeBg(Color.parseColor("#243053"),1,"#6B7CFF",22)); top.addView(sourceBtn,new LinearLayout.LayoutParams(dp(96),dp(42)));
        hero.addView(top);
        subTitle = tv("当前源："+source.title,13,Color.parseColor("#B9C2D9"),Typeface.NORMAL); subTitle.setPadding(0,dp(8),0,0); hero.addView(subTitle);
        TextView slogan = tv("聚合视频源 · 精致封面流 · 在线解析播放",13,Color.parseColor("#8EA0C4"),Typeface.NORMAL); slogan.setPadding(0,dp(4),0,0); hero.addView(slogan);
        root.addView(hero,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout search = new LinearLayout(this); search.setOrientation(LinearLayout.HORIZONTAL); search.setPadding(dp(14),dp(12),dp(14),dp(6)); search.setGravity(Gravity.CENTER_VERTICAL);
        searchBox = new EditText(this); searchBox.setSingleLine(true); searchBox.setHint("搜索片名 / 关键词"); searchBox.setTextColor(Color.WHITE); searchBox.setHintTextColor(Color.parseColor("#7C88A8")); searchBox.setPadding(dp(14),0,dp(14),0); searchBox.setBackground(strokeBg(Color.parseColor("#151B2E"),1,"#293552",18));
        search.addView(searchBox,new LinearLayout.LayoutParams(0,dp(46),1));
        Button searchBtn = new Button(this); searchBtn.setText("搜索"); searchBtn.setTextColor(Color.WHITE); searchBtn.setBackground(bg(Color.parseColor("#5B6CFF"),18)); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(82),dp(46)); sp.leftMargin=dp(8); search.addView(searchBtn,sp);
        root.addView(search);

        HorizontalScrollView hsv = new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false); categoryBar = new LinearLayout(this); categoryBar.setOrientation(LinearLayout.HORIZONTAL); categoryBar.setPadding(dp(14),dp(4),dp(14),dp(8)); hsv.addView(categoryBar); root.addView(hsv);
        buildCategories();

        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setGravity(Gravity.CENTER_VERTICAL); nav.setPadding(dp(14),0,dp(14),dp(8));
        homeBtn = new Button(this); homeBtn.setText("推荐"); homeBtn.setTextColor(Color.WHITE); homeBtn.setBackground(strokeBg(Color.parseColor("#151B2E"),1,"#34415F",16)); nav.addView(homeBtn,new LinearLayout.LayoutParams(0,dp(42),1));
        prevBtn = new Button(this); prevBtn.setText("上一页"); prevBtn.setTextColor(Color.WHITE); prevBtn.setBackground(strokeBg(Color.parseColor("#151B2E"),1,"#34415F",16)); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(42),1); bp.leftMargin=dp(8); nav.addView(prevBtn,bp);
        pageBadge = tv("第 1 页",14,Color.parseColor("#D5DCF2"),Typeface.BOLD); pageBadge.setGravity(Gravity.CENTER); LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(74),dp(42)); pp.leftMargin=dp(8); nav.addView(pageBadge,pp);
        nextBtn = new Button(this); nextBtn.setText("下一页"); nextBtn.setTextColor(Color.WHITE); nextBtn.setBackground(bg(Color.parseColor("#5B6CFF"),16)); LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,dp(42),1); np.leftMargin=dp(8); nav.addView(nextBtn,np);
        root.addView(nav);

        progress = new ProgressBar(this); progress.setVisibility(View.GONE); root.addView(progress,new LinearLayout.LayoutParams(-1,dp(4)));
        status = tv("准备加载推荐内容",13,Color.parseColor("#9BA8C8"),Typeface.NORMAL); status.setPadding(dp(16),dp(4),dp(16),dp(8)); root.addView(status);

        listView = new ListView(this); listView.setDivider(null); listView.setCacheColorHint(Color.TRANSPARENT); listView.setSelector(new ColorDrawable(Color.TRANSPARENT)); adapter = new VideoAdapter(); listView.setAdapter(adapter); root.addView(listView, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        homeBtn.setOnClickListener(v -> loadHome());
        searchBtn.setOnClickListener(v -> { page=1; currentKeyword=searchBox.getText().toString().trim(); currentClass="search"; load(); });
        prevBtn.setOnClickListener(v -> { if(page>1){ page--; load(); } });
        nextBtn.setOnClickListener(v -> { page++; load(); });
        sourceBtn.setOnClickListener(v -> startActivityForResult(new Intent(this, SourceManagerActivity.class), 9));
        listView.setOnItemClickListener((p,v,pos,id) -> {
            VideoItem it = items.get(pos);
            Intent in = new Intent(this, DetailActivity.class);
            in.putExtra("url", it.url); in.putExtra("title", it.title); in.putExtra("img", it.img); startActivity(in);
        });
        loadHome();
    }

    void buildCategories(){
        categoryBar.removeAllViews();
        String[] names=source.names(); String[] urls=source.urls();
        int n=Math.min(names.length, urls.length);
        for(int i=0;i<n;i++){
            final String name=names[i]; final String url=urls[i];
            Button b=new Button(this); b.setText(name); b.setTextSize(13); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setBackground(strokeBg(Color.parseColor("#151B2E"),1,"#2D3A5E",18));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40)); lp.rightMargin=dp(8); categoryBar.addView(b,lp);
            b.setOnClickListener(v->{ page=1; currentKeyword=""; currentClass=url; load(); });
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==9){ source=SourceConfig.load(this); Scraper.useSource(source); engine=new DrpyEngine(this,source); subTitle.setText("当前源："+source.title); buildCategories(); page=1; loadHome(); }
    }

    private void loadHome(){ page=1; currentClass="home"; currentKeyword=""; load(); }
    private void load(){
        progress.setVisibility(View.VISIBLE); pageBadge.setText("第 "+page+" 页"); status.setText("加载中...");
        if(source.raw!=null && source.raw.length()>0 && source.raw.contains("var rule")){
            String key="home".equals(currentClass)?"推荐":("search".equals(currentClass)?"搜索":"一级");
            String input="/";
            if("search".equals(currentClass)) input="/search/"+currentKeyword+"/"+page+"/";
            else if(!"home".equals(currentClass)) input="/category/"+currentClass+"/"+page+"/";
            engine.runList(key,input,(rs,err)->{ progress.setVisibility(View.GONE); items=rs; adapter.notifyDataSetChanged(); status.setText(err.length()>0?err:("共 "+rs.size()+" 部 · "+label()+" · 第 "+page+" 页")); });
            return;
        }
        new AsyncTask<Void,Void,ArrayList<VideoItem>>(){ String err="";
            protected ArrayList<VideoItem> doInBackground(Void... v){ try{
                if("home".equals(currentClass)) return Scraper.home();
                if("search".equals(currentClass)) return Scraper.search(currentKeyword, page);
                return Scraper.category(currentClass, page);
            }catch(Exception e){ err=e.toString(); return new ArrayList<>(); }}
            protected void onPostExecute(ArrayList<VideoItem> rs){ progress.setVisibility(View.GONE); items=rs; adapter.notifyDataSetChanged(); status.setText(err.length()>0?err:("共 "+rs.size()+" 部 · "+label()+" · 第 "+page+" 页")); }
        }.execute();
    }
    String label(){ if("home".equals(currentClass))return "推荐"; if("search".equals(currentClass))return "搜索："+currentKeyword; return "分类"; }

    class VideoAdapter extends BaseAdapter{
        public int getCount(){ return items.size(); }
        public Object getItem(int p){ return items.get(p); }
        public long getItemId(int p){ return p; }
        public View getView(int pos, View convert, ViewGroup parent){
            LinearLayout card; ImageView cover; TextView title,desc;
            if(convert==null){
                card=new LinearLayout(MainActivity.this); card.setOrientation(LinearLayout.HORIZONTAL); card.setPadding(dp(12),dp(10),dp(12),dp(10)); card.setBackgroundColor(Color.TRANSPARENT);
                cover=new ImageView(MainActivity.this); cover.setId(1001); cover.setScaleType(ImageView.ScaleType.CENTER_CROP); cover.setBackground(placeholderBg()); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(96),dp(136)); card.addView(cover,cp);
                LinearLayout info=new LinearLayout(MainActivity.this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(14),dp(4),dp(8),dp(4));
                title=tv("",17,Color.WHITE,Typeface.BOLD); title.setId(1002); title.setMaxLines(2); info.addView(title);
                desc=tv("",13,Color.parseColor("#AAB5D1"),Typeface.NORMAL); desc.setId(1003); desc.setPadding(0,dp(8),0,0); desc.setMaxLines(4); info.addView(desc,new LinearLayout.LayoutParams(-1,0,1));
                TextView tag=tv("立即查看  ›",13,Color.parseColor("#8EA0FF"),Typeface.BOLD); info.addView(tag);
                card.addView(info,new LinearLayout.LayoutParams(0,dp(136),1));
                convert=card;
            }
            cover=convert.findViewById(1001); title=convert.findViewById(1002); desc=convert.findViewById(1003);
            VideoItem it=items.get(pos); title.setText(it.title); desc.setText((it.desc==null||it.desc.length()==0)?"暂无简介，点击进入详情播放。":it.desc);
            cover.setImageDrawable(null); cover.setBackground(placeholderBg()); cover.setTag(it.img);
            if(it.img!=null && it.img.startsWith("http")) loadImage(it.img, cover);
            return convert;
        }
        Drawable placeholderBg(){ return new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.parseColor("#26345D"), Color.parseColor("#12182B")}); }
        void loadImage(String url, ImageView iv){
            new AsyncTask<Void,Void,Bitmap>(){ protected Bitmap doInBackground(Void...v){ try{ HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(8000); c.setReadTimeout(8000); c.setRequestProperty("User-Agent","Mozilla/5.0"); InputStream in=c.getInputStream(); return BitmapFactory.decodeStream(in); }catch(Exception e){ return null; }} protected void onPostExecute(Bitmap b){ if(b!=null && url.equals(iv.getTag())) iv.setImageBitmap(b); }}.execute();
        }
    }
}
