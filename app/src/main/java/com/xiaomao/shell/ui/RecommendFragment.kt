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
import coil.load
import com.xiaomao.shell.AppContainer
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.Category
import com.xiaomao.shell.data.model.VideoItem
import com.xiaomao.shell.databinding.FragmentRecommendBinding
import com.xiaomao.shell.ui.adapter.PosterStripAdapter
import com.xiaomao.shell.ui.adapter.VideoListAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecommendFragment : Fragment() {
    private var _binding: FragmentRecommendBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy { AppContainer.repository(requireContext()) }
    private val followAdapter = PosterStripAdapter(::openDetail)
    private val hotAdapter = VideoListAdapter(::openDetail)

    private var categories: List<Category> = emptyList()
    private var featuredVideo: VideoItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecommendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerFollow.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerFollow.adapter = followAdapter
        binding.recyclerFollow.isNestedScrollingEnabled = false

        binding.recyclerHot.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerHot.adapter = hotAdapter
        binding.recyclerHot.isNestedScrollingEnabled = false

        binding.shortcutCategory.imageShortcut.setImageResource(R.drawable.ic_feature_category)
        binding.shortcutCategory.textShortcut.setText(R.string.label_shortcut_category)
        binding.shortcutFeatured.imageShortcut.setImageResource(R.drawable.ic_feature_star)
        binding.shortcutFeatured.textShortcut.setText(R.string.label_shortcut_featured)
        binding.shortcutShort.imageShortcut.setImageResource(R.drawable.ic_feature_heart)
        binding.shortcutShort.textShortcut.setText(R.string.label_shortcut_short)
        binding.shortcutSchedule.imageShortcut.setImageResource(R.drawable.ic_feature_stack)
        binding.shortcutSchedule.textShortcut.setText(R.string.label_shortcut_schedule)
        binding.shortcutLive.imageShortcut.setImageResource(R.drawable.ic_feature_tv)
        binding.shortcutLive.textShortcut.setText(R.string.label_shortcut_live)

        binding.shortcutCategory.root.setOnClickListener { jumpToLibrary() }
        binding.shortcutFeatured.root.setOnClickListener { loadShortcut(0) }
        binding.shortcutShort.root.setOnClickListener { loadShortcut(1) }
        binding.shortcutSchedule.root.setOnClickListener { loadShortcut(2) }
        binding.shortcutLive.root.setOnClickListener { loadShortcut(3) }
        binding.buttonContinueWatch.setOnClickListener { featuredVideo?.let(::openDetail) }

        binding.swipeRefresh.setOnRefreshListener { loadPage() }
        binding.textToday.text = SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date())
        loadPage()
    }

    private fun loadPage() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            binding.progressBar.isVisible = true
            try {
                val home = repository.loadHome()
                categories = home.meta.categories
                val videos = home.videos
                featuredVideo = videos.firstOrNull()
                renderFeatured(featuredVideo)
                followAdapter.submitList(videos.drop(1).take(4))
                hotAdapter.submitList(videos.drop(5).ifEmpty { videos.take(6) })
                binding.textEmpty.isVisible = videos.isEmpty()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                binding.textEmpty.isVisible = true
                binding.textEmpty.text = getString(R.string.message_load_failed, error.message ?: "")
            } finally {
                binding.progressBar.isVisible = false
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun renderFeatured(video: VideoItem?) {
        binding.textBannerTitle.text = video?.title ?: getString(R.string.message_banner_placeholder)
        binding.textBannerDesc.text = video?.remarks ?: ""
        binding.imageBanner.load(video?.coverUrl) {
            placeholder(R.drawable.ic_placeholder)
            error(R.drawable.ic_placeholder)
            crossfade(true)
        }
        binding.textContinueTitle.text = video?.title ?: getString(R.string.message_empty)
    }

    private fun loadShortcut(index: Int) {
        val target = categories.getOrNull(index) ?: return
        startActivity(Intent(requireContext(), SearchActivity::class.java).apply {
            putExtra(SearchActivity.EXTRA_PRESET_KEYWORD, target.name)
        })
    }

    private fun jumpToLibrary() {
        (activity as? MainActivity)?.selectTab(MainActivity.Tab.LIBRARY)
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
