package com.memoria.meaningoflife.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.FragmentSettingsBinding
import com.memoria.meaningoflife.utils.DataExporter
import com.memoria.meaningoflife.utils.FileUtils
import com.memoria.meaningoflife.utils.LogManager
import com.memoria.meaningoflife.utils.QuoteManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SettingsViewModel
    private lateinit var quoteManager: QuoteManager
    private lateinit var quoteAdapter: QuoteListAdapter
    private var previousThemePosition = 0
    private var previousDarkModeState = false

    companion object {
        private const val TAG = "SettingsFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        quoteManager = QuoteManager(requireContext())

        // 保存初始状态
        previousThemePosition = viewModel.getThemePreference()
        previousDarkModeState = viewModel.isDarkModeEnabled()

        Log.d(TAG, "onViewCreated: themePosition=$previousThemePosition, darkMode=$previousDarkModeState")

        setupClickListeners()
        setupQuoteRecyclerView()
        loadData()
        loadQuotes()

        binding.btnAddQuote.setTextColor(primaryColor)

        // 设置开关初始状态
        binding.switchDarkMode.isChecked = previousDarkModeState
    }

    private fun setupQuoteRecyclerView() {
        quoteAdapter = QuoteListAdapter(
            onEdit = { quote, position ->
                showEditQuoteDialog(quote, position)
            },
            onDelete = { position ->
                quoteManager.deleteQuote(position)
                loadQuotes()
                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
            }
        )

        binding.quoteRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quoteAdapter
        }
    }

    private fun loadQuotes() {
        val quotes = quoteManager.getQuotes()
        quoteAdapter.submitList(quotes)
        Log.d(TAG, "loadQuotes: loaded ${quotes.size} quotes")
    }

    private fun setupClickListeners() {
        // 数据管理
        binding.btnBackup.setOnClickListener { exportData() }
        binding.btnRestore.setOnClickListener { restoreData() }
        binding.btnStorage.setOnClickListener { showStoragePath() }
        binding.btnClearCache.setOnClickListener { clearCache() }

        // 界面外观
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != previousDarkModeState) {
                previousDarkModeState = isChecked
                toggleDarkMode(isChecked)
            }
        }

        binding.spinnerTheme.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != previousThemePosition) {
                    previousThemePosition = position
                    applyThemeAndRestart(position)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 首页背景设置
        binding.btnBackgroundPreview.setOnClickListener {
            Log.d(TAG, "Background preview button clicked")
            startActivity(Intent(requireContext(), BackgroundPreviewActivity::class.java))
        }

        // 每日一言
        binding.btnAddQuote.setOnClickListener { showAddQuoteDialog() }

        // 关于
        binding.btnLogViewer.setOnClickListener {
            Log.d(TAG, "Log viewer button clicked")
            startActivity(Intent(requireContext(), LogViewerActivity::class.java))
        }
        binding.btnPrivacy.setOnClickListener { showPrivacyPolicy() }
        binding.btnIntro.setOnClickListener { showIntro() }
    }

    private fun toggleDarkMode(isDarkMode: Boolean) {
        Log.d(TAG, "toggleDarkMode: isDarkMode=$isDarkMode")

        // 保存设置
        viewModel.saveDarkModePreference(isDarkMode)

        // 应用深色模式
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // 刷新当前界面
        requireActivity().recreate()

        Toast.makeText(requireContext(), if (isDarkMode) "深色模式已开启" else "深色模式已关闭", Toast.LENGTH_SHORT).show()
    }

    private fun applyThemeAndRestart(position: Int) {
        Log.d(TAG, "applyThemeAndRestart: position=$position")

        // 保存设置
        viewModel.saveThemePreference(position)

        val themeNames = arrayOf("橙色", "绿色", "蓝色", "紫色")
        val themeName = themeNames[position]

        // 显示重启对话框
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("重启应用")
            .setMessage("主题已切换为${themeName}，需要重启应用以生效。是否立即重启？")
            .setPositiveButton("立即重启") { _, _ ->
                restartApp()
            }
            .setNegativeButton("稍后") { _, _ ->
                Toast.makeText(requireContext(), "主题已保存，下次启动生效", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun restartApp() {
        Log.d(TAG, "restartApp: Restarting app")

        // 创建重启 Intent
        val intent = Intent(requireContext(), com.memoria.meaningoflife.ui.MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        // 结束当前 Activity
        requireActivity().finish()

        // 结束进程
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun showAddQuoteDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "输入新的一言"

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("添加每日一言")
            .setView(input)
            .setPositiveButton("添加") { _, _ ->
                val newQuote = input.text.toString().trim()
                if (newQuote.isNotEmpty()) {
                    quoteManager.addQuote(newQuote)
                    loadQuotes()
                    Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditQuoteDialog(quote: String, position: Int) {
        val input = android.widget.EditText(requireContext())
        input.setText(quote)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("编辑每日一言")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val newQuote = input.text.toString().trim()
                if (newQuote.isNotEmpty()) {
                    quoteManager.updateQuote(position, newQuote)
                    loadQuotes()
                    Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadData() {
        binding.tvVersion.text = "版本号 ${viewModel.getVersionName()}"

        // 获取深色模式状态
        val isDarkMode = viewModel.isDarkModeEnabled()
        binding.switchDarkMode.isChecked = isDarkMode
        previousDarkModeState = isDarkMode

        val themes = arrayOf("橙色", "绿色", "蓝色", "紫色")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, themes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTheme.adapter = adapter
        val themePosition = viewModel.getThemePreference()
        binding.spinnerTheme.setSelection(themePosition)
        previousThemePosition = themePosition

        Log.d(TAG, "loadData: themePosition=$themePosition, darkMode=$isDarkMode")
    }

    private fun exportData() {
        Log.d(TAG, "exportData: Starting export")
        lifecycleScope.launch {
            DataExporter.exportAllData(requireContext())
        }
    }

    private fun restoreData() {
        Log.d(TAG, "restoreData: Starting restore")
        // 打开文件选择器
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.let { uri ->
                lifecycleScope.launch {
                    val success = DataExporter.importData(requireContext(), uri)
                    if (success) {
                        Toast.makeText(requireContext(), "恢复成功，请重启应用", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "恢复失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showStoragePath() {
        val path = FileUtils.getAppStorageDir(requireContext()).absolutePath
        Toast.makeText(requireContext(), "存储路径: $path", Toast.LENGTH_LONG).show()
        Log.d(TAG, "showStoragePath: path=$path")
    }

    private fun clearCache() {
        Log.d(TAG, "clearCache: Clearing cache")
        lifecycleScope.launch {
            DataExporter.clearCache(requireContext())
        }
    }

    private fun showPrivacyPolicy() {
        Log.d(TAG, "showPrivacyPolicy: Opening privacy policy")
        startActivity(Intent(requireContext(), PrivacyPolicyActivity::class.java))
    }

    private fun showIntro() {
        Log.d(TAG, "showIntro: Opening intro")
        startActivity(Intent(requireContext(), IntroActivity::class.java))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}