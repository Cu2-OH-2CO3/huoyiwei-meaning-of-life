package com.memoria.meaningoflife.ui.home

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.FragmentHomeBinding
import com.memoria.meaningoflife.ui.diary.DiaryActivity
import com.memoria.meaningoflife.ui.lunch.LunchActivity
import com.memoria.meaningoflife.ui.painting.PaintingActivity
import com.memoria.meaningoflife.ui.task.TaskListActivity
import com.memoria.meaningoflife.utils.BackgroundManager
import com.memoria.meaningoflife.utils.QuoteManager
import java.io.File

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var quoteManager: QuoteManager
    private lateinit var moduleAdapter: HomeModuleAdapter
    private var isEditMode = false
    private var backgroundImageView: ImageView? = null
    private var baseColorView: View? = null

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // 创建一个 FrameLayout 作为根容器
        val rootContainer = FrameLayout(requireContext())

        // 加载原布局
        _binding = FragmentHomeBinding.inflate(inflater, rootContainer, false)
        rootContainer.addView(binding.root)

        return rootContainer
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "onViewCreated: Start")

        // 初始化 BackgroundManager
        BackgroundManager.init(requireContext())

        quoteManager = QuoteManager(requireContext())
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 设置每日一言
        try {
            val dailyQuote = quoteManager.getDailyQuote()
            binding.tvSubtitle.text = dailyQuote
            Log.d(TAG, "Daily quote loaded: $dailyQuote")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load daily quote", e)
            binding.tvSubtitle.text = "记录创作，品味生活"
        }

        // 设置背景（包含底色层和图片层）
        setupBackground()

        setupModuleRecyclerView()
        setupClickListeners()
        observeData()

        Log.d(TAG, "onViewCreated: End")
    }

    private fun setupBackground() {
        Log.d(TAG, "setupBackground: Start")

        val isEnabled = BackgroundManager.isBackgroundEnabled()
        Log.d(TAG, "setupBackground: isEnabled=$isEnabled")

        // 获取根容器（FrameLayout）
        val rootContainer = (view as? ViewGroup)?.parent as? FrameLayout ?: return

        // 获取当前主题模式
        val isDarkMode = (requireActivity().resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        // 底色：深色模式用黑色，浅色模式用白色
        val baseColor = if (isDarkMode) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }

        Log.d(TAG, "setupBackground: isDarkMode=$isDarkMode, baseColor=${Integer.toHexString(baseColor)}")

        // 添加底色层（最底层）
        if (baseColorView == null) {
            baseColorView = View(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(baseColor)
            }
            rootContainer.addView(baseColorView, 0)
            Log.d(TAG, "setupBackground: Base color view added")
        } else {
            baseColorView?.setBackgroundColor(baseColor)
        }

        if (isEnabled) {
            val path = BackgroundManager.getBackgroundPath()
            val alphaPercent = BackgroundManager.getBackgroundAlpha()
            Log.d(TAG, "setupBackground: path=$path, alphaPercent=$alphaPercent%")

            // 计算图片透明度 (0-255)
            val imageAlpha = (alphaPercent * 2.55).toInt()
            Log.d(TAG, "setupBackground: imageAlpha=$imageAlpha")

            if (!path.isNullOrEmpty()) {
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        Log.d(TAG, "setupBackground: Bitmap loaded, size=${bitmap.width}x${bitmap.height}")

                        // 添加或更新背景图片层
                        if (backgroundImageView == null) {
                            backgroundImageView = ImageView(requireContext()).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            }
                            // 添加到底色层之上，内容布局之下（索引1）
                            rootContainer.addView(backgroundImageView, 1)
                            Log.d(TAG, "setupBackground: Background ImageView added")
                        }

                        backgroundImageView?.setImageBitmap(bitmap)
                        backgroundImageView?.alpha = imageAlpha / 255f
                        backgroundImageView?.visibility = View.VISIBLE

                        // 设置内容布局背景为透明
                        binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        Log.d(TAG, "setupBackground: Background applied with alpha=${imageAlpha}/255")
                    } else {
                        Log.e(TAG, "setupBackground: Failed to decode bitmap")
                        clearBackground(rootContainer)
                    }
                } else {
                    Log.e(TAG, "setupBackground: File not exists: $path")
                    clearBackground(rootContainer)
                }
            } else {
                Log.e(TAG, "setupBackground: Path is null")
                clearBackground(rootContainer)
            }
        } else {
            clearBackground(rootContainer)
        }
    }

    private fun clearBackground(rootContainer: FrameLayout) {
        backgroundImageView?.visibility = View.GONE
        // 设置内容布局背景为默认背景色
        binding.root.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.background)
        )
        Log.d(TAG, "clearBackground: Background cleared")
    }
    fun refreshModuleColors() {
        // 重新加载模块数据，触发适配器更新
        viewModel.loadStats()
        viewModel.loadVisibleModules()
    }

    private fun setupModuleRecyclerView() {
        moduleAdapter = HomeModuleAdapter(
            onModuleClick = { module ->
                if (!isEditMode) {
                    Log.d(TAG, "Module clicked: ${module.id}")
                    when (module.id) {
                        "painting" -> startActivity(Intent(requireContext(), PaintingActivity::class.java))
                        "diary" -> startActivity(Intent(requireContext(), DiaryActivity::class.java))
                        "lunch" -> startActivity(Intent(requireContext(), LunchActivity::class.java))
                        "task" -> startActivity(Intent(requireContext(), TaskListActivity::class.java))
                    }
                }
            },
            onDeleteClick = { module ->
                Log.d(TAG, "Module delete clicked: ${module.id}")
                viewModel.hideModule(module.id)
                Toast.makeText(requireContext(), "${module.title}已隐藏", Toast.LENGTH_SHORT).show()
            }
        )

        binding.moduleRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moduleAdapter
        }

        Log.d(TAG, "setupModuleRecyclerView: RecyclerView setup complete")
    }

    private fun setupClickListeners() {
        binding.btnAddModule.setOnClickListener {
            Log.d(TAG, "Add module button clicked")
            showAddModuleDialog()
        }

        binding.btnEditMode.setOnClickListener {
            isEditMode = !isEditMode
            moduleAdapter.setEditMode(isEditMode)
            binding.btnEditMode.setImageResource(
                if (isEditMode) android.R.drawable.ic_menu_save else R.drawable.ic_edit
            )
            Log.d(TAG, "Edit mode toggled: $isEditMode")
        }
    }

    private fun showAddModuleDialog() {
        val hiddenModules = viewModel.getHiddenModules()
        Log.d(TAG, "showAddModuleDialog: Hidden modules count=${hiddenModules.size}")

        if (hiddenModules.isEmpty()) {
            Toast.makeText(requireContext(), "没有可添加的模块", Toast.LENGTH_SHORT).show()
            return
        }

        val moduleNames = hiddenModules.map { it.title }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("添加模块")
            .setItems(moduleNames) { _, which ->
                val module = hiddenModules[which]
                Log.d(TAG, "Adding module: ${module.id}")
                viewModel.showModule(module.id)
                Toast.makeText(requireContext(), "${module.title}已添加", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeData() {
        viewModel.visibleModules.observe(viewLifecycleOwner) { modules ->
            Log.d(TAG, "visibleModules changed: count=${modules.size}")
            moduleAdapter.submitList(modules)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Refreshing background")
        setupBackground()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")

        // 清理背景视图
        val rootContainer = (view as? ViewGroup)?.parent as? FrameLayout
        backgroundImageView?.let {
            rootContainer?.removeView(it)
            backgroundImageView = null
        }
        baseColorView?.let {
            rootContainer?.removeView(it)
            baseColorView = null
        }
        _binding = null
    }
}