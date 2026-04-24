package com.xiaomao.shell.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.xiaomao.shell.AppContainer
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.Category
import com.xiaomao.shell.data.model.VideoItem
import com.xiaomao.shell.databinding.FragmentLibraryBinding
import com.xiaomao.shell.ui.adapter.SideCategoryAdapter
import com.xiaomao.shell.ui.adapter.TopCategoryAdapter
import com.xiaomao.shell.ui.adapter.VideoListAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy { AppContainer.repository(requireContext()) }
    private val topAdapter = TopCategoryAdapter(::onCategorySelected)
    private val sideAdapter = SideCategoryAdapter(::onCategorySelected)
    private val videoAdapter = VideoListAdapter(::openDetail)

    private var categories: List<Category> = emptyList()
    private var currentCategory: Category? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerTopTabs.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerTopTabs.adapter = topAdapter

        binding.recyclerSideMenu.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSideMenu.adapter = sideAdapter

        binding.recyclerVideos.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerVideos.adapter = videoAdapter

        loadCategories()
    }

    private fun loadCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.isVisible = true
            try {
                val home = repository.loadHome()
                categories = home.meta.categories
                val defaultCategory = categories.firstOrNull()
                currentCategory = defaultCategory
                topAdapter.submitList(categories.take(7), defaultCategory?.typeId)
                sideAdapter.submitList(categories, defaultCategory?.typeId)
                defaultCategory?.let { loadCategory(it) }
                binding.textEmpty.isVisible = categories.isEmpty()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                binding.textEmpty.isVisible = true
                binding.textEmpty.text = getString(R.string.message_load_failed, error.message ?: "")
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun loadCategory(category: Category) {
        currentCategory = category
        topAdapter.submitList(categories.take(7), category.typeId)
        sideAdapter.submitList(categories, category.typeId)
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.isVisible = true
            try {
                val videos = repository.loadCategory(category.typeId, 1)
                videoAdapter.submitList(videos)
                binding.textEmpty.isVisible = videos.isEmpty()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                binding.textEmpty.isVisible = true
                binding.textEmpty.text = getString(R.string.message_load_failed, error.message ?: "")
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun onCategorySelected(category: Category) {
        loadCategory(category)
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
