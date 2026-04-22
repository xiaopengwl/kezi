package com.example.chiguaapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import java.util.Locale;

public class PlayerActivity extends Activity {
    private FrameLayout root;
    private PlayerView playerView;
    private View gestureLayer;
    private LinearLayout topBar;
    private TextView titleView;
    private TextView stateView;
    private TextView gestureView;
    private ProgressBar loading;
    private TextView retryBtn;
    private TextView externalBtn;
    private TextView fullscreenBtn;

    private SourceConfig source;
    private DrpyEngine engine;
    private ExoPlayer player;
    private AudioManager audioManager;
    private String title;
    private String line;
    private String input;
    private String playUrl;
    private boolean resolved;
    private final Handler handler = new Handler();

    private static final int GESTURE_NONE = 0;
    private static final int GESTURE_SEEK = 1;
    private static final int GESTURE_VOLUME = 2;
    private static final int GESTURE_BRIGHTNESS = 3;

    private int gestureMode = GESTURE_NONE;
    private float downX;
    private float downY;
    private long seekStartPosition;
    private long seekPreviewPosition;
    private int startVolume;
    private int maxVolume;
    private float startBrightness = -1f;
    private boolean gestureLocked;

    private final Runnable hideBars = new Runnable() {
        @Override public void run() {
            if (topBar != null && resolved) {
                topBar.animate().alpha(0f).setDuration(220).withEndAction(() -> topBar.setVisibility(View.GONE)).start();
            }
            if (stateView != null && resolved) {
                stateView.animate().alpha(0f).setDuration(220).withEndAction(() -> stateView.setVisibility(View.GONE)).start();
            }
            if (gestureView != null) {
                gestureView.animate().alpha(0f).setDuration(180).withEndAction(() -> gestureView.setVisibility(View.GONE)).start();
            }
            if (playerView != null) playerView.hideController();
            immersive();
        }
    };

    private final Runnable hideGestureTip = new Runnable() {
        @Override public void run() {
            if (gestureView != null) {
                gestureView.animate().alpha(0f).setDuration(180).withEndAction(() -> gestureView.setVisibility(View.GONE)).start();
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        keepAwake(true);
        immersive();

        input = getIntent().getStringExtra("input");
        if (input == null) input = getIntent().getStringExtra("url");
        if (input == null) input = "";
        title = getIntent().getStringExtra("title");
        if (title == null || title.trim().length() == 0) title = "晓鹏影视";
        line = getIntent().getStringExtra("line");
        if (line == null || line.trim().length() == 0) line = "默认线路";

        source = SourceConfig.load(this);
        Scraper.useSource(source);
        engine = new DrpyEngine(this, source);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        } else {
            maxVolume = 15;
        }

        buildUi();
        resolveAndPlay();
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        playerView = new PlayerView(this);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        playerView.setControllerHideOnTouch(false);
        playerView.setKeepContentOnPlayerReset(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);
        playerView.setShutterBackgroundColor(Color.BLACK);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));

        gestureLayer = new View(this);
        gestureLayer.setBackgroundColor(Color.TRANSPARENT);
        gestureLayer.setClickable(true);
        gestureLayer.setOnTouchListener((v, event) -> handleGesture(event));
        root.addView(gestureLayer, new FrameLayout.LayoutParams(-1, -1));

        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(8), dp(12), dp(8));
        topBar.setBackgroundColor(Color.parseColor("#CC050814"));

        TextView back = pill("返回", "#20284A");
        back.setOnClickListener(v -> finish());
        topBar.addView(back, new LinearLayout.LayoutParams(-2, dp(36)));

        titleView = new TextView(this);
        titleView.setText(title + "  ·  " + line);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(14);
        titleView.setSingleLine(true);
        titleView.setPadding(dp(12), 0, dp(12), 0);
        topBar.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));

        retryBtn = pill("重试", "#24315F");
        retryBtn.setOnClickListener(v -> resolveAndPlay());
        topBar.addView(retryBtn, new LinearLayout.LayoutParams(-2, dp(36)));

        externalBtn = pill("外部", "#6B7CFF");
        externalBtn.setOnClickListener(v -> openExternal());
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-2, dp(36));
        ep.leftMargin = dp(8);
        topBar.addView(externalBtn, ep);

        fullscreenBtn = pill("全屏", "#0E8F6A");
        fullscreenBtn.setOnClickListener(v -> forceFullscreen());
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-2, dp(36));
        fp.leftMargin = dp(8);
        topBar.addView(fullscreenBtn, fp);

        root.addView(topBar, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        loading = new ProgressBar(this);
        center.addView(loading, new LinearLayout.LayoutParams(dp(42), dp(42)));
        stateView = new TextView(this);
        stateView.setText("正在解析播放地址…");
        stateView.setTextColor(Color.parseColor("#D9E2FF"));
        stateView.setTextSize(14);
        stateView.setGravity(Gravity.CENTER);
        stateView.setPadding(dp(16), dp(12), dp(16), dp(12));
        center.addView(stateView, new LinearLayout.LayoutParams(-2, -2));
        root.addView(center, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));

        gestureView = new TextView(this);
        gestureView.setTextColor(Color.WHITE);
        gestureView.setTextSize(18);
        gestureView.setGravity(Gravity.CENTER);
        gestureView.setPadding(dp(20), dp(14), dp(20), dp(14));
        GradientDrawable tipBg = new GradientDrawable();
        tipBg.setColor(Color.parseColor("#CC101626"));
        tipBg.setCornerRadius(dp(16));
        gestureView.setBackground(tipBg);
        gestureView.setVisibility(View.GONE);
        root.addView(gestureView, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        root.setOnClickListener(v -> showBarsTemporarily());
        playerView.setOnClickListener(v -> showBarsTemporarily());

        setContentView(root);
    }

    private TextView pill(String text, String bg) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(13);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(14), 0, dp(14), 0);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(bg));
        g.setCornerRadius(dp(18));
        v.setBackground(g);
        return v;
    }

    private void resolveAndPlay() {
        releasePlayer();
        resolved = false;
        playUrl = null;
        loading.setVisibility(View.VISIBLE);
        stateView.setVisibility(View.VISIBLE);
        stateView.setAlpha(1f);
        stateView.setText("正在解析播放地址…");
        showBarsTemporarily();

        if (source.raw != null && source.raw.length() > 0 && source.raw.contains("var rule")) {
            engine.runLazy(input, (u, err) -> {
                if (err != null && err.length() > 0 && (u == null || u.length() == 0)) {
                    showError("解析失败：" + err);
                } else {
                    startPlayer(u);
                }
            });
            return;
        }

        new AsyncTask<Void, Void, String>() {
            Exception error;
            @Override protected String doInBackground(Void... v) {
                try {
                    return Scraper.resolvePlay(input);
                } catch (Exception e) {
                    error = e;
                    return input;
                }
            }

            @Override protected void onPostExecute(String u) {
                if (error != null && (u == null || u.length() == 0)) {
                    showError("解析失败：" + error.getMessage());
                } else {
                    startPlayer(u);
                }
            }
        }.execute();
    }

    private void startPlayer(String url) {
        if (url == null || url.trim().length() == 0) url = input;
        playUrl = url == null ? "" : url.trim();
        if (playUrl.length() == 0) {
            showError("未获取到可播放地址");
            return;
        }

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
        MediaItem mediaItem = buildMediaItem(playUrl);
        MediaSource mediaSource = buildMediaSource(dataSourceFactory, mediaItem, playUrl);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaSource(mediaSource);
        player.setPlayWhenReady(true);
        player.prepare();
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    loading.setVisibility(View.VISIBLE);
                    stateView.setVisibility(View.VISIBLE);
                    stateView.setAlpha(1f);
                    stateView.setText("缓冲中… 左侧上下调音量，右侧上下调亮度，左右滑动调进度");
                } else if (state == Player.STATE_READY) {
                    resolved = true;
                    loading.setVisibility(View.GONE);
                    stateView.setVisibility(View.VISIBLE);
                    stateView.setAlpha(1f);
                    stateView.setText("正在播放 · 点按唤出控制栏 · 左侧音量 / 右侧亮度 / 左右滑动快进快退");
                    handler.postDelayed(() -> {
                        if (stateView != null) {
                            stateView.animate().alpha(0f).setDuration(220).withEndAction(() -> stateView.setVisibility(View.GONE)).start();
                        }
                    }, 1800);
                    showBarsTemporarily();
                } else if (state == Player.STATE_ENDED) {
                    stateView.setVisibility(View.VISIBLE);
                    stateView.setAlpha(1f);
                    stateView.setText("播放结束");
                    topBar.setVisibility(View.VISIBLE);
                    topBar.setAlpha(1f);
                }
            }

            @Override public void onPlayerError(PlaybackException error) {
                showError("播放失败：" + error.getMessage());
            }
        });
    }

    private MediaItem buildMediaItem(String url) {
        Uri uri = Uri.parse(url);
        MediaItem.Builder builder = new MediaItem.Builder().setUri(uri).setMediaId(url);
        String lower = url.toLowerCase();
        if (lower.contains(".m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8);
        else if (lower.contains(".mpd")) builder.setMimeType(MimeTypes.APPLICATION_MPD);
        else if (lower.contains(".mp4")) builder.setMimeType(MimeTypes.VIDEO_MP4);
        return builder.build();
    }

    private MediaSource buildMediaSource(DefaultDataSource.Factory factory, MediaItem item, String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".m3u8")) {
            return new HlsMediaSource.Factory(factory).createMediaSource(item);
        }
        return new ProgressiveMediaSource.Factory(factory).createMediaSource(item);
    }

    private boolean handleGesture(MotionEvent event) {
        if (event == null) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gestureLocked = false;
                gestureMode = GESTURE_NONE;
                downX = event.getX();
                downY = event.getY();
                seekStartPosition = player != null ? Math.max(player.getCurrentPosition(), 0) : 0;
                seekPreviewPosition = seekStartPosition;
                startVolume = audioManager != null ? audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) : 0;
                startBrightness = currentBrightness();
                handler.removeCallbacks(hideBars);
                handler.removeCallbacks(hideGestureTip);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (!gestureLocked) {
                    if (Math.abs(dx) < dp(12) && Math.abs(dy) < dp(12)) return true;
                    gestureLocked = true;
                    if (Math.abs(dx) >= Math.abs(dy)) {
                        gestureMode = GESTURE_SEEK;
                    } else if (downX < root.getWidth() / 2f) {
                        gestureMode = GESTURE_VOLUME;
                    } else {
                        gestureMode = GESTURE_BRIGHTNESS;
                    }
                }
                if (gestureMode == GESTURE_SEEK) {
                    updateSeek(dx);
                } else if (gestureMode == GESTURE_VOLUME) {
                    updateVolume(dy);
                } else if (gestureMode == GESTURE_BRIGHTNESS) {
                    updateBrightness(dy);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                finishGesture();
                if (!gestureLocked) {
                    showBarsTemporarily();
                }
                return true;
            default:
                return false;
        }
    }

    private void updateSeek(float dx) {
        if (player == null) return;
        long duration = player.getDuration();
        if (duration <= 0) duration = 0;
        long delta = (long) ((dx / Math.max(root.getWidth(), 1)) * 600000L);
        seekPreviewPosition = seekStartPosition + delta;
        if (duration > 0) {
            seekPreviewPosition = Math.max(0, Math.min(duration, seekPreviewPosition));
        } else {
            seekPreviewPosition = Math.max(0, seekPreviewPosition);
        }
        String direction = delta >= 0 ? "快进" : "快退";
        showGestureTip(direction + "\n" + formatTime(seekPreviewPosition) + " / " + formatTime(duration));
    }

    private void updateVolume(float dy) {
        if (audioManager == null || maxVolume <= 0) return;
        float percentDelta = (-dy) / Math.max(root.getHeight(), 1);
        int target = startVolume + Math.round(percentDelta * maxVolume * 1.6f);
        target = Math.max(0, Math.min(maxVolume, target));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
        int percent = Math.round(target * 100f / maxVolume);
        showGestureTip("音量\n" + percent + "%");
    }

    private void updateBrightness(float dy) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float base = startBrightness;
        float percentDelta = (-dy) / Math.max(root.getHeight(), 1);
        float target = base + percentDelta * 1.2f;
        if (target < 0.05f) target = 0.05f;
        if (target > 1f) target = 1f;
        lp.screenBrightness = target;
        getWindow().setAttributes(lp);
        int percent = Math.round(target * 100f);
        showGestureTip("亮度\n" + percent + "%");
    }

    private void finishGesture() {
        if (gestureMode == GESTURE_SEEK && player != null) {
            player.seekTo(Math.max(0, seekPreviewPosition));
            showGestureTip("已定位到\n" + formatTime(seekPreviewPosition));
            handler.postDelayed(hideGestureTip, 700);
        } else if (gestureMode == GESTURE_VOLUME || gestureMode == GESTURE_BRIGHTNESS) {
            handler.postDelayed(hideGestureTip, 500);
        }
        gestureMode = GESTURE_NONE;
        gestureLocked = false;
        showBarsTemporarily();
    }

    private float currentBrightness() {
        float b = getWindow().getAttributes().screenBrightness;
        if (b <= 0f) b = 0.5f;
        return b;
    }

    private void showGestureTip(String text) {
        if (gestureView == null) return;
        handler.removeCallbacks(hideGestureTip);
        gestureView.setText(text);
        gestureView.setVisibility(View.VISIBLE);
        gestureView.setAlpha(1f);
    }

    private String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long total = ms / 1000;
        long s = total % 60;
        long m = (total / 60) % 60;
        long h = total / 3600;
        if (h > 0) return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s);
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    private void forceFullscreen() {
        immersive();
        if (playerView != null) playerView.hideController();
        if (topBar != null) {
            topBar.setVisibility(View.GONE);
            topBar.setAlpha(0f);
        }
        if (stateView != null) {
            stateView.setVisibility(View.GONE);
            stateView.setAlpha(0f);
        }
        if (gestureView != null) {
            gestureView.setVisibility(View.GONE);
            gestureView.setAlpha(0f);
        }
    }

    private void showError(String msg) {
        releasePlayer();
        loading.setVisibility(View.GONE);
        resolved = false;
        stateView.setVisibility(View.VISIBLE);
        stateView.setAlpha(1f);
        stateView.setText(msg + "\n可点“重试 / 外部”，或返回详情页切换线路");
        topBar.setVisibility(View.VISIBLE);
        topBar.setAlpha(1f);
        showBarsTemporarily();
    }

    private void openExternal() {
        String u = playUrl != null && playUrl.length() > 0 ? playUrl : input;
        if (u == null || u.length() == 0) return;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("无法打开外部播放器")
                    .setMessage(e.getMessage())
                    .setPositiveButton("知道了", null)
                    .show();
        }
    }

    private void releasePlayer() {
        handler.removeCallbacks(hideBars);
        handler.removeCallbacks(hideGestureTip);
        if (playerView != null) playerView.setPlayer(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void showBarsTemporarily() {
        handler.removeCallbacks(hideBars);
        if (topBar != null) {
            topBar.setVisibility(View.VISIBLE);
            topBar.animate().alpha(1f).setDuration(120).start();
        }
        if (stateView != null && (stateView.getText() != null && stateView.getText().length() > 0)) {
            stateView.setVisibility(View.VISIBLE);
            stateView.animate().alpha(1f).setDuration(120).start();
        }
        if (playerView != null) playerView.showController();
        handler.postDelayed(hideBars, resolved ? 3500 : 6000);
        immersive();
    }

    private void immersive() {
        int flags = View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void keepAwake(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    @Override protected void onResume() {
        super.onResume();
        immersive();
        if (playerView != null) playerView.onResume();
        if (player != null) player.play();
    }

    @Override protected void onPause() {
        if (player != null) player.pause();
        if (playerView != null) playerView.onPause();
        super.onPause();
    }

    @Override protected void onStop() {
        if (Build.VERSION.SDK_INT <= 23) releasePlayer();
        super.onStop();
    }

    @Override protected void onDestroy() {
        releasePlayer();
        keepAwake(false);
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        finish();
    }
}
