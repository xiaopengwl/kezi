package com.example.chiguaapp;

import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.util.HashMap;
import java.util.Map;

import cn.jzvd.JZMediaInterface;
import cn.jzvd.Jzvd;

/**
 * JZVideo 播放内核：用 Media3 ExoPlayer 承接 m3u8/mp4 等直链播放。
 * 网页解析/嗅探仍由 PlayerActivity 负责，拿到真实媒体地址后交给本类播放。
 */
public class JZMediaExoMedia3 extends JZMediaInterface implements Player.Listener {
    public static String USER_AGENT = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36";
    public static String REFERER = "";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExoPlayer player;
    private Surface surface;
    private Runnable bufferingCallback;

    public static void setDefaultHeaders(String userAgent, String referer) {
        if (userAgent != null && userAgent.trim().length() > 0) USER_AGENT = userAgent.trim();
        REFERER = referer == null ? "" : referer.trim();
    }

    public JZMediaExoMedia3(Jzvd jzvd) {
        super(jzvd);
    }

    @Override
    public void start() {
        mainHandler.post(() -> { if (player != null) player.play(); });
    }

    @Override
    public void prepare() {
        mainHandler.post(() -> {
            releasePlayerOnly();
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", USER_AGENT);
            if (REFERER != null && REFERER.startsWith("http")) headers.put("Referer", REFERER);

            DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent(USER_AGENT)
                    .setAllowCrossProtocolRedirects(true)
                    .setDefaultRequestProperties(headers);
            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(jzvd.getContext(), httpFactory);

            player = new ExoPlayer.Builder(jzvd.getContext())
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                    .build();
            player.addListener(this);
            if (surface != null) player.setVideoSurface(surface);
            String url = jzvd.jzDataSource == null || jzvd.jzDataSource.getCurrentUrl() == null
                    ? "" : jzvd.jzDataSource.getCurrentUrl().toString();
            player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
            player.setRepeatMode(jzvd.jzDataSource != null && jzvd.jzDataSource.looping ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
            player.prepare();
            player.play();
            bufferingCallback = new BufferingUpdateRunnable();
        });
    }

    @Override
    public void pause() {
        mainHandler.post(() -> { if (player != null) player.pause(); });
    }

    @Override
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    @Override
    public void seekTo(long time) {
        mainHandler.post(() -> {
            if (player != null) {
                player.seekTo(time);
                jzvd.seekToInAdvance = time;
            }
        });
    }

    @Override
    public void release() {
        mainHandler.post(this::releasePlayerOnly);
        JZMediaInterface.SAVED_SURFACE = null;
    }

    private void releasePlayerOnly() {
        if (bufferingCallback != null) mainHandler.removeCallbacks(bufferingCallback);
        bufferingCallback = null;
        if (player != null) {
            player.removeListener(this);
            player.release();
            player = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }

    @Override
    public long getCurrentPosition() {
        return player == null ? 0 : player.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        if (player == null) return 0;
        long duration = player.getDuration();
        return duration == C.TIME_UNSET ? 0 : duration;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mainHandler.post(() -> { if (player != null) player.setVolume(Math.max(leftVolume, rightVolume)); });
    }

    @Override
    public void setSpeed(float speed) {
        mainHandler.post(() -> { if (player != null) player.setPlaybackParameters(new PlaybackParameters(speed)); });
    }

    @Override
    public void setSurface(Surface newSurface) {
        mainHandler.post(() -> {
            if (surface != null && surface != newSurface) surface.release();
            surface = newSurface;
            if (player != null) player.setVideoSurface(surface);
        });
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                jzvd.onStatePreparingPlaying();
                if (bufferingCallback != null) mainHandler.post(bufferingCallback);
                break;
            case Player.STATE_READY:
                if (player != null && player.getPlayWhenReady()) jzvd.onStatePlaying();
                break;
            case Player.STATE_ENDED:
                jzvd.onCompletion();
                break;
            case Player.STATE_IDLE:
            default:
                break;
        }
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        jzvd.onError(1000, 1000);
    }

    @Override
    public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
        int width = videoSize.width;
        int height = videoSize.height;
        if (width > 0 && height > 0) jzvd.onVideoSizeChanged(width, height);
    }

    @Override
    public void onRenderedFirstFrame() {
        // no-op
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        if (JZMediaInterface.SAVED_SURFACE == null) {
            JZMediaInterface.SAVED_SURFACE = surfaceTexture;
            setSurface(new Surface(surfaceTexture));
            prepare();
        } else if (jzvd.textureView != null) {
            jzvd.textureView.setSurfaceTexture(JZMediaInterface.SAVED_SURFACE);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
    }

    private class BufferingUpdateRunnable implements Runnable {
        @Override
        public void run() {
            if (player == null) return;
            int percent = player.getBufferedPercentage();
            jzvd.setBufferProgress(percent);
            if (percent < 100) mainHandler.postDelayed(this, 300);
        }
    }
}
