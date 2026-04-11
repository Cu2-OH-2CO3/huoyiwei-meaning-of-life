// 路径：com/memoria/meaningoflife/utils/AutoBackupManager.kt
package com.memoria.meaningoflife.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.database.timeline.SourceMode
import com.memoria.meaningoflife.data.repository.*
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 自动备份管理器
 * 负责在应用启动时自动生成快速备份文件
 */
object AutoBackupManager {

    private const val TAG = "AutoBackupManager"
    private const val BACKUP_DIR_NAME = "活意味/backups"
    private const val QUICK_BACKUP_FILE_NAME = "quick_backup.json"

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    /**
     * 执行快速备份
     * @param context 上下文
     * @param force 是否强制执行（忽略开关状态）
     * @param onComplete 完成回调
     */
    fun performQuickBackup(
        context: Context,
        force: Boolean = false,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        // 检查开关是否启用
        if (!force && !PreferenceManager.isAutoBackupEnabled(context)) {
            Log.d(TAG, "自动备份已关闭")
            onComplete?.invoke(false, null)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = MeaningOfLifeApp.instance.database

                // 创建备份目录
                val backupDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    BACKUP_DIR_NAME
                )
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                val backupFile = File(backupDir, QUICK_BACKUP_FILE_NAME)

                // 收集所有数据
                val backupData = collectAllData(context, database)

                // 写入文件
                backupFile.writeText(gson.toJson(backupData))

                // 更新最后备份时间
                PreferenceManager.setLastBackupTime(context, System.currentTimeMillis())

                Log.i(TAG, "快速备份完成: ${backupFile.absolutePath}, 大小: ${backupFile.length()} bytes")

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, backupFile.absolutePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "快速备份失败", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, null)
                }
            }
        }
    }

    /**
     * 收集所有需要备份的数据
     */
    private suspend fun collectAllData(
        context: Context,
        database: com.memoria.meaningoflife.data.database.AppDatabase
    ): JsonObject {
        val root = JsonObject()

        // 备份元信息
        val meta = JsonObject()
        meta.addProperty("version", 1)
        meta.addProperty("appVersion", getAppVersion(context))
        meta.addProperty("backupTime", System.currentTimeMillis())
        meta.addProperty("backupDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        meta.addProperty("backupType", "quick")
        root.add("meta", meta)

        // 创建 Repository 实例
        val paintingRepo = PaintingRepository(database)
        val diaryRepo = DiaryRepository(database)
        val lunchRepo = LunchRepository(database)
        val taskRepo = TaskRepository(database)
        val timelineRepo = TimelineRepository(database)
        val checkinRepo = CheckinRepository(database)

        // 绘画模块数据
        root.add("works", gson.toJsonTree(paintingRepo.getAllWorksSync()))
        root.add("nodes", gson.toJsonTree(paintingRepo.getAllNodesSync()))
        root.add("goals", gson.toJsonTree(paintingRepo.getAllGoalsSync()))

        // 日记模块数据
        root.add("diaries", gson.toJsonTree(diaryRepo.getAllDiariesSync()))
        // 当前仓库层未提供标签全量接口，先导出空数组保持结构兼容
        root.add("diary_tags", gson.toJsonTree(emptyList<String>()))

        // 午餐模块数据
        root.add("dishes", gson.toJsonTree(lunchRepo.getAllDishesSync()))
        root.add("lottery_history", gson.toJsonTree(lunchRepo.getAllLotteryHistorySync()))

        // 签到模块数据
        root.add("checkins", gson.toJsonTree(checkinRepo.getAllCheckinsSync()))

        // 待办任务模块数据
        root.add("tasks", gson.toJsonTree(taskRepo.getAllTasksSync()))
        root.add("task_nodes", gson.toJsonTree(taskRepo.getAllTaskNodesSync()))

        // 时间轴模块数据
        val allTimelineEvents = timelineRepo.getEventsByMode(SourceMode.SCHEDULE) +
                timelineRepo.getEventsByMode(SourceMode.FREE)
        root.add("timeline_events", gson.toJsonTree(allTimelineEvents.distinctBy { it.id }))
        root.add("courses", gson.toJsonTree(timelineRepo.getAllCourses()))
        root.add("semesters", gson.toJsonTree(timelineRepo.getAllSemesters()))

        return root
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    /**
     * 获取备份文件信息
     */
    fun getQuickBackupFileInfo(@Suppress("UNUSED_PARAMETER") context: Context): Pair<Boolean, String> {
        val backupDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            BACKUP_DIR_NAME
        )
        val backupFile = File(backupDir, QUICK_BACKUP_FILE_NAME)

        return if (backupFile.exists()) {
            val lastModified = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(backupFile.lastModified()))
            val sizeKB = backupFile.length() / 1024
            true to "最后备份: $lastModified (${sizeKB}KB)"
        } else {
            false to "暂无快速备份"
        }
    }
}