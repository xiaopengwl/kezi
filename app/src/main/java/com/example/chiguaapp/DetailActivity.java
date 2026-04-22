package com.example.chiguaapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DetailActivity extends Activity {
    String url, title, img;
    SourceConfig source;
    DrpyEngine engine;

    LinearLayout root;
    LinearLayout lineTabs;
    LinearLayout episodeWrap;
    ProgressBar progress;
    TextView titleTv;
    TextView sourceTv;
    TextView statTv;
    TextView descTv;
    ImageView poster;

    ArrayList<PlayItem> plays = new ArrayList<>();
    String activeLine = "";

    int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    TextView tv(String s, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        return t;
    }

    GradientDrawable fill(String color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(dp(radius));
        return g;
    }

    GradientDrawable stroke(String color, String strokeColor, float radius) {
        GradientDrawable g = fill(color, radius);
        g.setStroke(dp(1), Color.parseColor(strokeColor));
        return g;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        url = getIntent().getStringExtra("url");
        title = getIntent().getStringExtra("title");
        img = getIntent().getStringExtra("img");
        if (title == null || title.trim().length() == 0) title = "详情";
        source = SourceConfig.load(this);
        Scraper.useSource(source);
        engine = new DrpyEngine(this, source);
        buildUi();
        loadDetail();
    }

    void buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Color.parseColor("#0B1020"));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(22));
        sv.addView(root, new ScrollView.LayoutParams(-1, -2));
        setContentView(sv);

        LinearLayout topCard = new LinearLayout(this);
        topCard.setOrientation(LinearLayout.VERTICAL);
        topCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        topCard.setBackground(stroke("#131A2E", "#233150", 22));
        root.addView(topCard, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        topCard.addView(nav, new LinearLayout.LayoutParams(-1, -2));

        TextView back = tv("‹ 返回", 16, Color.WHITE, Typeface.BOLD);
        back.setPadding(dp(12), dp(8), dp(12), dp(8));
        back.setBackground(stroke("#18213A", "#324261", 18));
        back.setOnClickListener(v -> finish());
        nav.addView(back);

        View spacer = new View(this);
        nav.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        TextView switchSource = tv("切换源", 13, Color.parseColor("#DDE6FF"), Typeface.BOLD);
        switchSource.setGravity(Gravity.CENTER);
        switchSource.setPadding(dp(14), dp(8), dp(14), dp(8));
        switchSource.setBackground(stroke("#1D2745", "#51658F", 18));
        switchSource.setOnClickListener(v -> showSourceDialog());
        nav.addView(switchSource);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams headLp = new LinearLayout.LayoutParams(-1, -2);
        headLp.topMargin = dp(14);
        topCard.addView(head, headLp);

        poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(fill("#1A2440", 18));
        head.addView(poster, new LinearLayout.LayoutParams(dp(110), dp(152)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(14), 0, 0, 0);
        head.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        titleTv = tv(title, 21, Color.WHITE, Typeface.BOLD);
        titleTv.setMaxLines(3);
        info.addView(titleTv);

        TextView badge = tv("详情页 · 稳定版", 11, Color.parseColor("#B8FFEA"), Typeface.BOLD);
        badge.setPadding(0, dp(6), 0, 0);
        info.addView(badge);

        sourceTv = tv("当前源：" + source.title, 13, Color.parseColor("#9FB0D6"), Typeface.NORMAL);
        sourceTv.setPadding(0, dp(8), 0, 0);
        info.addView(sourceTv);

        statTv = tv("正在加载详情...", 12, Color.parseColor("#7E90B7"), Typeface.NORMAL);
        statTv.setPadding(0, dp(8), 0, 0);
        info.addView(statTv);

        TextView playNow = tv("▶ 立即播放", 15, Color.WHITE, Typeface.BOLD);
        playNow.setGravity(Gravity.CENTER);
        playNow.setPadding(0, dp(10), 0, dp(10));
        playNow.setBackground(fill("#5568FF", 18));
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(-1, -2);
        playLp.topMargin = dp(16);
        info.addView(playNow, playLp);
        playNow.setOnClickListener(v -> {
            if (plays.isEmpty()) {
                Toast.makeText(this, "当前还没有可播放选集", Toast.LENGTH_SHORT).show();
            } else {
                openPlay(plays.get(0));
            }
        });

        if (img != null && img.startsWith("http")) loadImage(img, poster);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(-1, dp(4));
        progressLp.topMargin = dp(14);
        root.addView(progress, progressLp);

        LinearLayout descCard = sectionCard("剧情简介");
        descTv = tv("暂无简介", 13, Color.parseColor("#B9C4DE"), Typeface.NORMAL);
        descTv.setLineSpacing(dp(2), 1f);
        descTv.setPadding(0, dp(8), 0, 0);
        descCard.addView(descTv);

        TextView lineTitle = sectionTitle("播放线路");
        root.addView(lineTitle);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        lineTabs = new LinearLayout(this);
        lineTabs.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(lineTabs);
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(-1, -2);
        hsvLp.topMargin = dp(6);
        root.addView(hsv, hsvLp);

        TextView epTitle = sectionTitle("选集");
        root.addView(epTitle);

        LinearLayout epCard = new LinearLayout(this);
        epCard.setOrientation(LinearLayout.VERTICAL);
        epCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        epCard.setBackground(stroke("#11192C", "#23314E", 20));
        LinearLayout.LayoutParams epCardLp = new LinearLayout.LayoutParams(-1, -2);
        epCardLp.topMargin = dp(6);
        root.addView(epCard, epCardLp);

        episodeWrap = new LinearLayout(this);
        episodeWrap.setOrientation(LinearLayout.VERTICAL);
        epCard.addView(episodeWrap, new LinearLayout.LayoutParams(-1, -2));
    }

    LinearLayout sectionCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(stroke("#11192C", "#23314E", 20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        root.addView(card, lp);
        TextView titleView = tv(title, 17, Color.WHITE, Typeface.BOLD);
        card.addView(titleView);
        return card;
    }

    TextView sectionTitle(String text) {
        TextView t = tv(text, 17, Color.WHITE, Typeface.BOLD);
        t.setPadding(dp(2), dp(18), dp(2), 0);
        return t;
    }

    void loadDetail() {
        progress.setVisibility(View.VISIBLE);
        if (source.raw != null && source.raw.length() > 0 && source.raw.contains("var rule")) {
            engine.runDetail(url, (d, err) -> {
                progress.setVisibility(View.GONE);
                if (d == null) {
                    showFallbackDetail(err);
                    return;
                }
                show(d.title, d.content, d.pic, d.plays);
            });
            return;
        }
        new AsyncTask<Void, Void, Scraper.Detail>() {
            String err = "";

            @Override
            protected Scraper.Detail doInBackground(Void... v) {
                try {
                    return Scraper.detail(url);
                } catch (Exception e) {
                    err = e.toString();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Scraper.Detail d) {
                progress.setVisibility(View.GONE);
                if (d == null) {
                    showFallbackDetail(err);
                    return;
                }
                show(d.title, d.content, d.pic, d.plays);
            }
        }.execute();
    }

    void showFallbackDetail(String err) {
        ArrayList<PlayItem> ps = new ArrayList<>();
        if (url != null && url.length() > 0) ps.add(new PlayItem("网页嗅探", "嗅探播放", url));
        String text = "详情解析暂未完全适配，已保留原页面嗅探播放入口。";
        if (err != null && err.length() > 0) text += "\n\n错误：" + err;
        show(title, text, img, ps);
    }

    void show(String t, String content, String pic, ArrayList<PlayItem> ps) {
        if (t != null && t.trim().length() > 0) {
            title = t.trim();
            titleTv.setText(title);
        }
        if (pic != null && pic.startsWith("http")) loadImage(pic, poster);
        descTv.setText(content != null && content.trim().length() > 0 ? content.trim() : "暂无简介");
        plays = ps == null ? new ArrayList<>() : ps;
        if (plays.isEmpty() && url != null && url.length() > 0) {
            plays.add(new PlayItem("网页嗅探", "嗅探播放", url));
        }
        statTv.setText("共 " + plays.size() + " 个播放入口");
        buildLineTabs();
    }

    ArrayList<String> lines() {
        ArrayList<String> out = new ArrayList<>();
        for (PlayItem p : plays) {
            String line = (p.source == null || p.source.trim().length() == 0) ? "默认线路" : p.source.trim();
            if (!out.contains(line)) out.add(line);
        }
        if (out.isEmpty()) out.add("默认线路");
        return out;
    }

    void buildLineTabs() {
        lineTabs.removeAllViews();
        ArrayList<String> ls = lines();
        if (activeLine == null || activeLine.length() == 0 || !ls.contains(activeLine)) {
            activeLine = ls.get(0);
        }
        for (String line : ls) {
            TextView chip = tv(line, 13, Color.WHITE, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(9), dp(14), dp(9));
            chip.setBackground(line.equals(activeLine)
                    ? fill("#5568FF", 18)
                    : stroke("#151F35", "#324261", 18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            lineTabs.addView(chip, lp);
            chip.setOnClickListener(v -> {
                activeLine = line;
                buildLineTabs();
            });
        }
        buildEpisodes();
    }

    void buildEpisodes() {
        episodeWrap.removeAllViews();
        ArrayList<PlayItem> current = new ArrayList<>();
        for (PlayItem p : plays) {
            String line = (p.source == null || p.source.trim().length() == 0) ? "默认线路" : p.source.trim();
            if (line.equals(activeLine)) current.add(p);
        }
        if (current.isEmpty()) {
            TextView empty = tv("暂无可播放选集，可尝试切换源", 14, Color.parseColor("#8EA0C4"), Typeface.NORMAL);
            empty.setPadding(dp(4), dp(8), dp(4), dp(8));
            episodeWrap.addView(empty);
            return;
        }

        LinearLayout row = null;
        int col = 0;
        for (PlayItem p : current) {
            if (row == null || col == 3) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                if (episodeWrap.getChildCount() > 0) rowLp.topMargin = dp(8);
                episodeWrap.addView(row, rowLp);
                col = 0;
            }
            TextView ep = tv((p.name == null || p.name.length() == 0) ? "播放" : p.name, 13, Color.parseColor("#EAF0FF"), Typeface.BOLD);
            ep.setGravity(Gravity.CENTER);
            ep.setSingleLine(true);
            ep.setPadding(dp(6), dp(12), dp(6), dp(12));
            ep.setBackground(stroke("#151F35", "#31415F", 16));
            LinearLayout.LayoutParams epLp = new LinearLayout.LayoutParams(0, -2, 1);
            if (col < 2) epLp.rightMargin = dp(8);
            row.addView(ep, epLp);
            ep.setOnClickListener(v -> openPlay(p));
            col++;
        }
    }

    void openPlay(PlayItem p) {
        Intent in = new Intent(this, PlayerActivity.class);
        in.putExtra("input", p.input);
        in.putExtra("title", title + " · " + p.name);
        in.putExtra("series_title", title);
        in.putExtra("line", p.source);

        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> inputs = new ArrayList<>();
        int currentIndex = 0;
        for (PlayItem item : plays) {
            String sourceName = (item.source == null || item.source.trim().length() == 0) ? "默认线路" : item.source.trim();
            if (!sourceName.equals(activeLine)) continue;
            names.add(item.name == null ? "" : item.name);
            inputs.add(item.input == null ? "" : item.input);
            if (item == p) currentIndex = names.size() - 1;
        }
        in.putStringArrayListExtra("episode_names", names);
        in.putStringArrayListExtra("episode_inputs", inputs);
        in.putExtra("episode_index", currentIndex);
        startActivity(in);
    }

    void showSourceDialog() {
        ArrayList<SourceConfig> all = SourceConfig.loadAll(this);
        String[] names = new String[all.size()];
        for (int i = 0; i < all.size(); i++) names[i] = all.get(i).title;
        new AlertDialog.Builder(this)
                .setTitle("切换数据源")
                .setItems(names, (d, which) -> {
                    SourceConfig.setActive(this, which);
                    Toast.makeText(this, "已切换到：" + names[which], Toast.LENGTH_SHORT).show();
                    Intent in = new Intent(this, MainActivity.class);
                    in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(in);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    void loadImage(String u, ImageView iv) {
        iv.setTag(u);
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... v) {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    c.setRequestProperty("User-Agent", "Mozilla/5.0");
                    return BitmapFactory.decodeStream(c.getInputStream());
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap b) {
                if (b != null && u.equals(iv.getTag())) iv.setImageBitmap(b);
            }
        }.execute();
    }
}
