package com.xiaomao.shell.player

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import com.xiaomao.shell.AppContainer
import com.xiaomao.shell.R
import com.xiaomao.shell.databinding.ActivityPlayerBinding
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private val repository by lazy { AppContainer.repository(this) }
    private var player: ExoPlayer? = null

    private val playTitle by lazy { intent.getStringExtra(EXTRA_PLAY_TITLE).orEmpty() }
    private val episodeName by lazy { intent.getStringExtra(EXTRA_EPISODE_NAME).orEmpty() }
    private val playUrl by lazy { intent.getStringExtra(EXTRA_PLAY_URL).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = buildString {
            append(playTitle.ifBlank { getString(R.string.app_name) })
            if (episodeName.isNotBlank()) append(" - ").append(episodeName)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        resolveAndPlay()
    }

    private fun resolveAndPlay() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.textStatus.visibility = View.VISIBLE
            binding.textStatus.setText(R.string.message_play_loading)
            try {
                val result = repository.parsePlay(playUrl)
                if (result.url.isBlank()) error(getString(R.string.message_parse_failed))
                startPlayer(result.url, result.headers)
            } catch (error: Throwable) {
                binding.textStatus.text = getString(R.string.message_load_failed, error.message ?: "")
                Toast.makeText(
                    this@PlayerActivity,
                    getString(R.string.message_load_failed, error.message ?: ""),
                    Toast.LENGTH_SHORT,
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun startPlayer(url: String, headers: Map<String, String>) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .also { exoPlayer ->
                binding.playerView.player = exoPlayer
                binding.textStatus.visibility = View.GONE
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_PLAY_TITLE = "extra_play_title"
        const val EXTRA_EPISODE_NAME = "extra_episode_name"
        const val EXTRA_PLAY_URL = "extra_play_url"
    }
}
