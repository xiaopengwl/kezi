package com.xiaomao.shell.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.xiaomao.shell.AppContainer
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.PlayEpisode
import com.xiaomao.shell.data.model.VideoDetail
import com.xiaomao.shell.databinding.ActivityDetailBinding
import com.xiaomao.shell.ui.adapter.EpisodeAdapter
import com.xiaomao.shell.ui.adapter.LineAdapter
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private val repository by lazy { AppContainer.repository(this) }

    private val lineAdapter = LineAdapter(::onLineSelected)
    private val episodeAdapter = EpisodeAdapter(::playEpisode)

    private var detailUrl: String = ""
    private var currentDetail: VideoDetail? = null
    private var selectedLineIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detailUrl = intent.getStringExtra(EXTRA_DETAIL_URL).orEmpty()
        val fallbackTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = fallbackTitle.ifBlank { getString(R.string.message_detail) }
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recyclerLines.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerLines.adapter = lineAdapter

        binding.recyclerEpisodes.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerEpisodes.adapter = episodeAdapter

        loadDetail()
    }

    private fun loadDetail() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val detail = repository.loadDetail(detailUrl)
                currentDetail = detail
                render(detail)
            } catch (error: Throwable) {
                binding.textSummary.text = getString(R.string.message_load_failed, error.message ?: "")
                Toast.makeText(
                    this@DetailActivity,
                    getString(R.string.message_load_failed, error.message ?: ""),
                    Toast.LENGTH_SHORT,
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun render(detail: VideoDetail) {
        supportActionBar?.title = detail.title.ifBlank { getString(R.string.message_detail) }
        binding.imageCover.load(detail.coverUrl) {
            placeholder(R.drawable.ic_placeholder)
            error(R.drawable.ic_placeholder)
            crossfade(true)
        }
        binding.textTitle.text = detail.title
        binding.textMeta.text = buildString {
            appendIfNotBlank(getString(R.string.label_meta_remarks), detail.remarks)
            appendIfNotBlank(getString(R.string.label_meta_year), detail.year)
            appendIfNotBlank(getString(R.string.label_meta_area), detail.area)
            appendIfNotBlank(getString(R.string.label_meta_director), detail.director)
            appendIfNotBlank(getString(R.string.label_meta_actor), detail.actor)
        }.ifBlank { getString(R.string.message_no_video_meta) }
        binding.textSummary.text = detail.summary.ifBlank { getString(R.string.message_no_summary) }

        selectedLineIndex = 0
        lineAdapter.submitList(detail.playGroups, selectedLineIndex)
        episodeAdapter.submitList(detail.playGroups.firstOrNull()?.episodes.orEmpty())
        binding.textEmptyEpisode.visibility =
            if (detail.playGroups.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onLineSelected(index: Int) {
        selectedLineIndex = index
        val groups = currentDetail?.playGroups.orEmpty()
        lineAdapter.submitList(groups, selectedLineIndex)
        episodeAdapter.submitList(groups.getOrNull(index)?.episodes.orEmpty())
    }

    private fun playEpisode(episode: PlayEpisode) {
        episodeAdapter.submitList(
            currentDetail?.playGroups?.getOrNull(selectedLineIndex)?.episodes.orEmpty(),
            selectedPlayUrl = episode.playUrl,
        )
        startActivity(Intent(this, com.xiaomao.shell.player.PlayerActivity::class.java).apply {
            putExtra(com.xiaomao.shell.player.PlayerActivity.EXTRA_PLAY_TITLE, currentDetail?.title.orEmpty())
            putExtra(com.xiaomao.shell.player.PlayerActivity.EXTRA_EPISODE_NAME, episode.name)
            putExtra(com.xiaomao.shell.player.PlayerActivity.EXTRA_PLAY_URL, episode.playUrl)
        })
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun StringBuilder.appendIfNotBlank(label: String, value: String) {
        if (value.isBlank()) return
        if (isNotEmpty()) append('\n')
        append(label).append("：").append(value)
    }

    companion object {
        const val EXTRA_DETAIL_URL = "extra_detail_url"
        const val EXTRA_TITLE = "extra_title"
    }
}
