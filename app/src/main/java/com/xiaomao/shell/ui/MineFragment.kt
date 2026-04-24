package com.xiaomao.shell.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xiaomao.shell.databinding.FragmentMineBinding

class MineFragment : Fragment() {
    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.siteManagerRow.root.setOnClickListener {
            startActivity(Intent(requireContext(), SourceConfigActivity::class.java))
        }
        binding.parseManagerRow.root.setOnClickListener {
            startActivity(Intent(requireContext(), SourceConfigActivity::class.java))
        }
        binding.themeRow.root.setOnClickListener {
            (activity as? MainActivity)?.selectTab(MainActivity.Tab.RECOMMEND)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
