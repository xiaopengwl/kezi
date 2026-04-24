package com.xiaomao.shell.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xiaomao.shell.AppContainer
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.Category
import com.xiaomao.shell.data.model.VideoItem
import com.xiaomao.shell.databinding.FragmentChartsBinding
import com.xiaomao.shell.ui.adapter.RankingAdapter
import com.xiaomao.shell.ui.adapter.TopCategoryAdapter
import kotlinx.coroutines.launch

class ChartsFragment : Fragment() {
    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy { AppContainer.repository(requireContext()) }
    private val tabAdapter = TopCategoryAdapter(::loadCategory)
    private val rankingAdapter = RankingAdapter(::openDetail)
    private var categories: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerTabs.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerTabs.adapter = tabAdapter

        binding.recyclerRanking.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRanking.adapter = rankingAdapter

        loadInitial()
    }

    private fun loadInitial() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.isVisible = true
            try {
                val home = repository.loadHome()
                categories = home.meta.categories
                tabAdapter.submitList(categories.take(6), categories.firstOrNull()?.typeId)
                rankingAdapter.submitList(home.videos.take(10))
                binding.textEmpty.isVisible = home.videos.isEmpty()
            } catch (error: Throwable) {
                binding.textEmpty.isVisible = true
                binding.textEmpty.text = getString(R.string.message_load_failed, error.message ?: "")
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun loadCategory(category: Category) {
        tabAdapter.submitList(categories.take(6), category.typeId)
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.isVisible = true
            try {
                val videos = repository.loadCategory(category.typeId, 1)
                rankingAdapter.submitList(videos.take(10))
                binding.textEmpty.isVisible = videos.isEmpty()
            } catch (error: Throwable) {
                binding.textEmpty.isVisible = true
                binding.textEmpty.text = getString(R.string.message_load_failed, error.message ?: "")
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun openDetail(item: VideoItem) {
        startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_DETAIL_URL, item.detailUrl)
            putExtra(DetailActivity.EXTRA_TITLE, item.title)
        })
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

