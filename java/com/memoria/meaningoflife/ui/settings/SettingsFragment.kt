package com.memoria.meaningoflife.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.memoria.meaningoflife.ui.MainActivity
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.FragmentSettingsBinding

import com.memoria.meaningoflife.utils.CardColorManager
import com.memoria.meaningoflife.utils.DataExporter
import com.memoria.meaningoflife.utils.FileUtils
import com.memoria.meaningoflife.utils.QuoteManager
import kotlinx.coroutines.launch
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SettingsViewModel
    private lateinit var quoteManager: QuoteManager
    private lateinit var quoteAdapter: QuoteListAdapter
    private var previousThemePosition = 0
    private var previousDarkModeState = false

    private var eggClickCount = 0
    private var eggLastClickTime = 0L

    private val restoreFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            restoreFromUri(uri)
        } else {
            Toast.makeText(requireContext(), "未选择文件", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

        previousThemePosition = viewModel.getThemePreference()
        previousDarkModeState = viewModel.isDarkModeEnabled()

        Log.d(TAG, "onViewCreated: themePosition=$previousThemePosition, darkMode=$previousDarkModeState")

        setupClickListeners()
        setupQuoteRecyclerView()
        loadData()
        loadQuotes()

        binding.btnAddQuote.setTextColor(primaryColor)
        binding.switchDarkMode.isChecked = previousDarkModeState
    }

    /**
     * 检查存储权限（Android 11+ 需要 MANAGE_EXTERNAL_STORAGE）
     */
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * 请求存储权限（Android 11+）
     */
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlertDialog.Builder(requireContext())
                .setTitle("需要存储权限")
                .setMessage("为了将备份文件保存到 Download 文件夹，需要授予\"管理所有文件\"权限。")
                .setPositiveButton("去授权") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${requireContext().packageName}")
                        startActivity(intent)
                    } catch (e: Exception) {
                        // 如果上述方式失败，跳转到设置页面
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            Toast.makeText(requireContext(), "Android 10及以下不需要此权限", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreFromUri(uri: Uri) {
        Log.d(TAG, "restoreFromUri: uri=$uri")
        lifecycleScope.launch {
            var tempFile: File? = null
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    tempFile = File(requireContext().cacheDir, "temp_restore_${System.currentTimeMillis()}.json")
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    inputStream.close()

                    Log.d(TAG, "restoreFromUri: tempFile created, size=${tempFile.length()}")

                    val success = DataExporter.importData(requireContext(), tempFile)
                    if (success) {
                        Toast.makeText(requireContext(), "恢复成功，请重启应用", Toast.LENGTH_LONG).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            requireActivity().recreate()
                        }, 1500)
                    } else {
                        Toast.makeText(requireContext(), "恢复失败，请检查文件格式", Toast.LENGTH_SHORT).show()
                    }
                    tempFile.delete()
                } else {
                    Toast.makeText(requireContext(), "无法读取文件", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "恢复数据失败", e)
                Toast.makeText(requireContext(), "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                tempFile?.delete()
            }
        }
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
        binding.btnBackup.setOnClickListener { exportData() }
        binding.btnRestore.setOnClickListener { restoreData() }
        binding.btnStorage.setOnClickListener { showStoragePath() }
        binding.btnClearCache.setOnClickListener { clearCache() }

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

        binding.btnBackgroundPreview.setOnClickListener {
            startActivity(Intent(requireContext(), BackgroundPreviewActivity::class.java))
        }

        binding.btnCardColors.setOnClickListener {
            showCardColorDialog()
        }

        binding.btnAddQuote.setOnClickListener { showAddQuoteDialog() }

        binding.btnLogViewer.setOnClickListener {
            startActivity(Intent(requireContext(), LogViewerActivity::class.java))
        }
        binding.btnPrivacy.setOnClickListener { showPrivacyPolicy() }
        binding.btnIntro.setOnClickListener { showIntro() }
    }

    private fun showCardColorDialog() {
        val presetNames = CardColorManager.getPresetNames(requireContext())
        val items = presetNames.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("选择卡片主题")
            .setSingleChoiceItems(items, -1) { _, which ->
                val selectedPreset = presetNames[which]
                CardColorManager.applyPreset(requireContext(), selectedPreset)
                Toast.makeText(requireContext(), "已切换为「${selectedPreset}」主题", Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.refreshHomeModules()
            }
            .setNeutralButton("自定义") { _, _ ->
                showCustomColorDialog()
            }
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showCustomColorDialog() {
        val dialogBinding = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_custom_card_colors, null)

        val etPaintingColor = dialogBinding.findViewById<android.widget.EditText>(R.id.etPaintingColor)
        val etDiaryColor = dialogBinding.findViewById<android.widget.EditText>(R.id.etDiaryColor)
        val etLunchColor = dialogBinding.findViewById<android.widget.EditText>(R.id.etLunchColor)
        val etTaskColor = dialogBinding.findViewById<android.widget.EditText>(R.id.etTaskColor)
        val etBackupColor = dialogBinding.findViewById<android.widget.EditText>(R.id.etBackupColor)
        val etPresetName = dialogBinding.findViewById<android.widget.EditText>(R.id.etPresetName)

        // ✅ 修复：将颜色值安全转换为十六进制字符串
        etPaintingColor.setText(colorToHexString(CardColorManager.getPaintingCardColorHex(requireContext())))
        etDiaryColor.setText(colorToHexString(CardColorManager.getDiaryCardColorHex(requireContext())))
        etLunchColor.setText(colorToHexString(CardColorManager.getLunchCardColorHex(requireContext())))
        etTaskColor.setText(colorToHexString(CardColorManager.getTaskCardColorHex(requireContext())))
        etBackupColor.setText(colorToHexString(CardColorManager.getBackupCardColorHex(requireContext())))

        AlertDialog.Builder(requireContext())
            .setTitle("自定义卡片颜色")
            .setView(dialogBinding)
            .setPositiveButton("保存") { _, _ ->
                val presetName = etPresetName.text.toString().trim()
                val paintingColor = etPaintingColor.text.toString().trim()
                val diaryColor = etDiaryColor.text.toString().trim()
                val lunchColor = etLunchColor.text.toString().trim()
                val taskColor = etTaskColor.text.toString().trim()
                val backupColor = etBackupColor.text.toString().trim()

                if (presetName.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入预设名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val colors = mutableListOf<String>()
                if (paintingColor.isNotEmpty()) colors.add(paintingColor)
                if (diaryColor.isNotEmpty()) colors.add(diaryColor)
                if (lunchColor.isNotEmpty()) colors.add(lunchColor)
                if (taskColor.isNotEmpty()) colors.add(taskColor)
                if (backupColor.isNotEmpty()) colors.add(backupColor)

                if (colors.size >= 5) {
                    CardColorManager.saveCustomPreset(requireContext(), presetName, colors)
                    CardColorManager.applyPreset(requireContext(), presetName)
                    Toast.makeText(requireContext(), "自定义颜色已保存", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.refreshHomeModules()
                } else {
                    Toast.makeText(requireContext(), "请填写所有五个颜色", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 将 Int 颜色值转换为 #RRGGBB 格式的十六进制字符串
     */
    private fun colorToHexString(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    private fun toggleDarkMode(isDarkMode: Boolean) {
        viewModel.saveDarkModePreference(isDarkMode)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        requireActivity().recreate()
        Toast.makeText(requireContext(), if (isDarkMode) "深色模式已开启" else "深色模式已关闭", Toast.LENGTH_SHORT).show()
    }

    private fun applyThemeAndRestart(position: Int) {
        viewModel.saveThemePreference(position)
        val themeNames = arrayOf("橙色", "绿色", "蓝色", "紫色", "粉色", "红色")
        val themeName = themeNames[position]

        AlertDialog.Builder(requireContext())
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
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        requireActivity().finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun showAddQuoteDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "输入新的一言"

        AlertDialog.Builder(requireContext())
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

        AlertDialog.Builder(requireContext())
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

        binding.tvVersion.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - eggLastClickTime > 1000) {
                eggClickCount = 0
            }
            eggLastClickTime = currentTime
            eggClickCount++

            if (eggClickCount >= 5) {
                eggClickCount = 0
                startActivity(Intent(requireContext(), DinoGameActivity::class.java))
                Toast.makeText(requireContext(), "彩蛋解锁！", Toast.LENGTH_SHORT).show()
            }
        }

        val isDarkMode = viewModel.isDarkModeEnabled()
        binding.switchDarkMode.isChecked = isDarkMode
        previousDarkModeState = isDarkMode

        val themes = arrayOf("橙-子很好吃", "绿-色是伪装", "蓝-Instant Blue", "紫-エンパープル", "粉-Sakura Fubuki", "红-無我夢中")
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

        // 检查存储权限（Android 11+）
        if (!checkStoragePermission()) {
            requestStoragePermission()
            Toast.makeText(requireContext(), "请授权后再次点击备份", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val file = DataExporter.exportAllData(requireContext())
            if (file != null) {
                // 复制到 Download 文件夹
                try {
                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (downloadDir != null && downloadDir.exists() || downloadDir?.mkdirs() == true) {
                        val destFile = File(downloadDir, file.name)
                        file.copyTo(destFile, overwrite = true)
                        Log.d(TAG, "exportData: copied to ${destFile.absolutePath}")
                        Toast.makeText(requireContext(), "备份成功: ${file.name}\n已保存到 Download 文件夹", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "备份成功: ${file.name}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "exportData: failed to copy to Download", e)
                    Toast.makeText(requireContext(), "备份成功: ${file.name}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), "备份失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreData() {
        Log.d(TAG, "restoreData: Starting restore")
        restoreFileLauncher.launch("application/json")
    }

    private fun showStoragePath() {
        val path = FileUtils.getAppStorageDir(requireContext()).absolutePath
        Toast.makeText(requireContext(), "存储路径: $path", Toast.LENGTH_LONG).show()
        Log.d(TAG, "showStoragePath: path=$path")
    }

    private fun clearCache() {
        Log.d(TAG, "clearCache: Clearing cache")
        lifecycleScope.launch {
            try {
                val cacheDir = requireContext().cacheDir
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
                Glide.get(requireContext()).clearDiskCache()
                Toast.makeText(requireContext(), "缓存已清除", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "clearCache: Error clearing cache", e)
                Toast.makeText(requireContext(), "清除缓存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPrivacyPolicy() {
        startActivity(Intent(requireContext(), PrivacyPolicyActivity::class.java))
    }

    private fun showIntro() {
        startActivity(Intent(requireContext(), IntroActivity::class.java))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}