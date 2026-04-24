package com.xiaomao.shell.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.xiaomao.shell.AppContainer
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.VideoItem
import com.xiaomao.shell.databinding.ActivitySearchBinding
import com.xiaomao.shell.ui.adapter.VideoListAdapter
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private val repository by lazy { AppContainer.repository(this) }
    private val videoAdapter = VideoListAdapter(::openDetail)

    private var currentKeyword = ""
    private var currentPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerResults.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerResults.adapter = videoAdapter
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.buttonSearch.setOnClickListener { startSearch(true) }
        binding.editKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                startSearch(true)
                true
            } else {
                false
            }
        }
        binding.buttonPrevPage.setOnClickListener {
            if (currentPage <= 1) {
                Toast.makeText(this, R.string.message_last_page, Toast.LENGTH_SHORT).show()
            } else {
                currentPage -= 1
                search(currentKeyword, currentPage)
            }
        }
        binding.buttonNextPage.setOnClickListener {
            currentPage += 1
            search(currentKeyword, currentPage)
        }

        val preset = intent.getStringExtra(EXTRA_PRESET_KEYWORD).orEmpty()
        if (preset.isNotBlank()) {
            binding.editKeyword.setText(preset)
            startSearch(true)
        }
    }

    private fun startSearch(resetPage: Boolean) {
        val keyword = binding.editKeyword.text?.toString()?.trim().orEmpty()
        if (keyword.isBlank()) {
            Toast.makeText(this, R.string.message_need_keyword, Toast.LENGTH_SHORT).show()
            return
        }
        currentKeyword = keyword
        if (resetPage) currentPage = 1
        search(keyword, currentPage)
    }

    private fun search(keyword: String, page: Int) {
        lifecycleScope.launch {
            binding.progressBar.isVisible = true
            try {
                val list = repository.search(keyword, page)
                videoAdapter.submitList(list)
                binding.textEmpty.isVisible = list.isEmpty()
                binding.textPage.text = getString(R.string.label_page_number, currentPage)
                binding.buttonPrevPage.isEnabled = currentPage > 1
                binding.buttonNextPage.isEnabled = true
            } catch (error: Throwable) {
                if (page > 1) currentPage -= 1
                binding.textEmpty.isVisible = true
                binding.textEmpty.text = getString(R.string.message_load_failed, error.message ?: "")
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun openDetail(item: VideoItem) {
        startActivity(Intent(this, DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_DETAIL_URL, item.detailUrl)
            putExtra(DetailActivity.EXTRA_TITLE, item.title)
        })
    }

    companion object {
        const val EXTRA_PRESET_KEYWORD = "extra_preset_keyword"
    }
}

