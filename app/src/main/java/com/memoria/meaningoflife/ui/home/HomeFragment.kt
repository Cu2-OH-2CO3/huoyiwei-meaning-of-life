package com.memoria.meaningoflife.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.FragmentHomeBinding
import com.memoria.meaningoflife.ui.diary.DiaryActivity
import com.memoria.meaningoflife.ui.lunch.LunchActivity
import com.memoria.meaningoflife.ui.painting.PaintingActivity
import com.memoria.meaningoflife.utils.QuoteManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var quoteManager: QuoteManager
    private lateinit var moduleAdapter: HomeModuleAdapter
    private var isEditMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quoteManager = QuoteManager(requireContext())
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 设置每日一言
        try {
            val dailyQuote = quoteManager.getDailyQuote()
            binding.tvSubtitle.text = dailyQuote
        } catch (e: Exception) {
            binding.tvSubtitle.text = "记录创作，品味生活"
        }

        setupModuleRecyclerView()
        setupClickListeners()
        observeData()
    }

    private fun setupModuleRecyclerView() {
        moduleAdapter = HomeModuleAdapter(
            onModuleClick = { module ->
                if (!isEditMode) {
                    when (module.id) {
                        "painting" -> startActivity(Intent(requireContext(), PaintingActivity::class.java))
                        "diary" -> startActivity(Intent(requireContext(), DiaryActivity::class.java))
                        "lunch" -> startActivity(Intent(requireContext(), LunchActivity::class.java))
                    }
                }
            },
            onDeleteClick = { module ->
                viewModel.hideModule(module.id)
                Toast.makeText(requireContext(), "${module.title}已隐藏", Toast.LENGTH_SHORT).show()
            }
        )

        binding.moduleRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moduleAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAddModule.setOnClickListener {
            showAddModuleDialog()
        }

        binding.btnEditMode.setOnClickListener {
            isEditMode = !isEditMode
            moduleAdapter.setEditMode(isEditMode)
            // 使用 Toast 提示当前模式
            Toast.makeText(requireContext(), if (isEditMode) "编辑模式" else "完成", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddModuleDialog() {
        val hiddenModules = viewModel.getHiddenModules()
        if (hiddenModules.isEmpty()) {
            Toast.makeText(requireContext(), "没有可添加的模块", Toast.LENGTH_SHORT).show()
            return
        }

        val moduleNames = hiddenModules.map { it.title }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("添加模块")
            .setItems(moduleNames) { _, which ->
                val module = hiddenModules[which]
                viewModel.showModule(module.id)
                Toast.makeText(requireContext(), "${module.title}已添加", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeData() {
        viewModel.visibleModules.observe(viewLifecycleOwner) { modules ->
            moduleAdapter.submitList(modules)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}