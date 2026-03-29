package com.memoria.meaningoflife.ui.settings

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.memoria.meaningoflife.databinding.ActivityLogViewerBinding
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogViewerActivity : BaseActivity() {

    private lateinit var binding: ActivityLogViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "应用日志"

        setupClickListeners()
        loadLogs()
    }

    private fun setupClickListeners() {
        binding.btnRefresh.setOnClickListener {
            loadLogs()
        }

        binding.btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("清除日志")
                .setMessage("确定要清除所有日志吗？此操作不可恢复。")
                .setPositiveButton("清除") { _, _ ->
                    clearLogs()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        binding.btnExport.setOnClickListener {
            exportLogs()
        }

        binding.btnCopy.setOnClickListener {
            copyToClipboard()
        }
    }

    private fun loadLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            val logContent = LogManager.getLogContent()
            val fileSize = LogManager.getLogFileSize()

            withContext(Dispatchers.Main) {
                binding.tvLogContent.text = if (logContent.isBlank()) {
                    "暂无日志记录"
                } else {
                    logContent
                }
                binding.tvLogContent.movementMethod = ScrollingMovementMethod()

                val sizeKB = fileSize / 1024
                binding.tvLogInfo.text = "日志大小: ${sizeKB}KB"
            }
        }
    }

    private fun clearLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            val success = LogManager.clearLogs()
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@LogViewerActivity, "日志已清除", Toast.LENGTH_SHORT).show()
                    loadLogs()
                } else {
                    Toast.makeText(this@LogViewerActivity, "清除失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun exportLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            val file = LogManager.exportLog(this@LogViewerActivity)
            withContext(Dispatchers.Main) {
                if (file != null) {
                    Toast.makeText(
                        this@LogViewerActivity,
                        "日志已导出到: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this@LogViewerActivity, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun copyToClipboard() {
        val logContent = binding.tvLogContent.text.toString()
        if (logContent.isNotEmpty() && logContent != "暂无日志记录") {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("应用日志", logContent)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "没有可复制的内容", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}