package com.xiaomao.shell.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.xiaomao.shell.R
import com.xiaomao.shell.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val sourceEditorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                recreate()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.buttonMore.setOnClickListener {
            sourceEditorLauncher.launch(Intent(this, SourceConfigActivity::class.java))
        }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_recommend -> {
                    showTab(Tab.RECOMMEND)
                    true
                }
                R.id.nav_library -> {
                    showTab(Tab.LIBRARY)
                    true
                }
                R.id.nav_charts -> {
                    showTab(Tab.CHARTS)
                    true
                }
                R.id.nav_mine -> {
                    showTab(Tab.MINE)
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_recommend
        }
    }

    fun selectTab(tab: Tab) {
        binding.bottomNavigation.selectedItemId = when (tab) {
            Tab.RECOMMEND -> R.id.nav_recommend
            Tab.LIBRARY -> R.id.nav_library
            Tab.CHARTS -> R.id.nav_charts
            Tab.MINE -> R.id.nav_mine
        }
    }

    private fun showTab(tab: Tab) {
        val fragment: Fragment
        val title: String
        when (tab) {
            Tab.RECOMMEND -> {
                fragment = RecommendFragment()
                title = "Mi Video"
            }
            Tab.LIBRARY -> {
                fragment = LibraryFragment()
                title = "Film Library"
            }
            Tab.CHARTS -> {
                fragment = ChartsFragment()
                title = "Mi Charts"
            }
            Tab.MINE -> {
                fragment = MineFragment()
                title = getString(R.string.label_mine)
            }
        }
        binding.textBrandTitle.text = title
        binding.textBrandSubtitle.setText(R.string.label_brand_subtitle)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    enum class Tab {
        RECOMMEND,
        LIBRARY,
        CHARTS,
        MINE,
    }
}

