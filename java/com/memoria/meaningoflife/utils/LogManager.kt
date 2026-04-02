package com.memoria.meaningoflife.utils

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object LogManager {

    private const val LOG_FILE_NAME = "app_log.txt"
    private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB

    private var logFile: File? = null
    private var logWriter: FileWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        try {
            val logDir = FileUtils.getAppStorageDir(context)
            logFile = File(logDir, LOG_FILE_NAME)

            // 如果日志文件超过最大大小，备份并创建新文件
            if (logFile!!.exists() && logFile!!.length() > MAX_LOG_SIZE) {
                val backupFile = File(logDir, "app_log_${System.currentTimeMillis()}.txt")
                logFile!!.renameTo(backupFile)
                logFile = File(logDir, LOG_FILE_NAME)
            }

            logWriter = FileWriter(logFile, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun log(level: String, tag: String, message: String) {
        try {
            val timestamp = dateFormat.format(Date())
            val logLine = "$timestamp [$level] $tag: $message\n"

            // 写入文件
            logWriter?.append(logLine)
            logWriter?.flush()

            // 同时输出到 Logcat
            when (level) {
                "V" -> android.util.Log.v(tag, message)
                "D" -> android.util.Log.d(tag, message)
                "I" -> android.util.Log.i(tag, message)
                "W" -> android.util.Log.w(tag, message)
                "E" -> android.util.Log.e(tag, message)
                else -> android.util.Log.i(tag, message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun v(tag: String, message: String) = log("V", tag, message)
    fun d(tag: String, message: String) = log("D", tag, message)
    fun i(tag: String, message: String) = log("I", tag, message)
    fun w(tag: String, message: String) = log("W", tag, message)
    fun e(tag: String, message: String) = log("E", tag, message)

    fun getLogContent(): String {
        return try {
            logFile?.readText() ?: "暂无日志"
        } catch (e: Exception) {
            "读取日志失败: ${e.message}"
        }
    }

    fun clearLogs(): Boolean {
        return try {
            logWriter?.close()
            logFile?.delete()
            logFile?.createNewFile()
            logWriter = FileWriter(logFile, true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getLogFileSize(): Long {
        return logFile?.length() ?: 0
    }

    fun exportLog(context: Context): File? {
        return try {
            val exportDir = FileUtils.getExportDir(context)
            val exportFile = File(exportDir, "app_log_${System.currentTimeMillis()}.txt")
            logFile?.copyTo(exportFile, overwrite = true)
            exportFile
        } catch (e: Exception) {
            null
        }
    }
}