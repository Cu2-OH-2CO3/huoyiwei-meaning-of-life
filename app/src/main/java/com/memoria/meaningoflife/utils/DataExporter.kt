package com.memoria.meaningoflife.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.data.database.checkin.CheckinEntity
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.data.database.lunch.LotteryHistoryEntity
import com.memoria.meaningoflife.data.database.painting.GoalEntity
import com.memoria.meaningoflife.data.database.painting.NodeEntity
import com.memoria.meaningoflife.data.database.painting.WorkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import com.bumptech.glide.Glide
import java.util.*

object DataExporter {

    private const val TAG = "DataExporter"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * 导出数据类
     */
    data class ExportData(
        val exportTime: String,
        val version: String = "1.0",
        val paintings: List<WorkEntity>,
        val nodes: List<NodeEntity>,
        val goals: List<GoalEntity>,
        val diaries: List<DiaryEntity>,
        val dishes: List<DishEntity>,
        val lotteryHistory: List<LotteryHistoryEntity>,
        val checkins: List<CheckinEntity>
    )

    /**
     * 导出所有数据到 JSON 文件
     */
    suspend fun exportAllData(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "exportAllData: Starting export")

            val database = AppDatabase.getInstance(context)

            // 获取所有数据
            val paintings = database.workDao().getAllWorksSync()
            val nodes = database.nodeDao().getNodesByWorkId(0L) // 获取所有节点需要遍历作品
            val allNodes = mutableListOf<NodeEntity>()
            paintings.forEach { work ->
                allNodes.addAll(database.nodeDao().getNodesByWorkId(work.id))
            }

            val goals = database.goalDao().getAllGoals()
            val diaries = database.diaryDao().getAllDiariesSync()
            val dishes = database.dishDao().getAllDishesSync()
            val lotteryHistory = database.lotteryHistoryDao().getRecentHistory()
            val checkins = database.checkinDao().getAllCheckinsSync()

            Log.d(TAG, "exportAllData: paintings=${paintings.size}, nodes=${allNodes.size}, goals=${goals.size}, " +
                    "diaries=${diaries.size}, dishes=${dishes.size}, history=${lotteryHistory.size}, checkins=${checkins.size}")

            val exportData = ExportData(
                exportTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                version = "1.0",
                paintings = paintings,
                nodes = allNodes,
                goals = goals,
                diaries = diaries,
                dishes = dishes,
                lotteryHistory = lotteryHistory,
                checkins = checkins
            )

            val json = gson.toJson(exportData)
            val exportDir = FileUtils.getExportDir(context)

            // 创建带时间戳的文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "huoyiwei_export_$timestamp.json"
            val jsonFile = File(exportDir, fileName)

            FileWriter(jsonFile).use { writer ->
                writer.write(json)
            }

            Log.d(TAG, "exportAllData: Success, file=${jsonFile.absolutePath}, size=${jsonFile.length()} bytes")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出成功: $fileName\n路径: ${exportDir.absolutePath}", Toast.LENGTH_LONG).show()
            }

            return@withContext jsonFile
        } catch (e: Exception) {
            Log.e(TAG, "exportAllData: Error", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }
    }

    /**
     * 从 JSON 文件导入数据
     */
    suspend fun importData(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "importData: Starting import from $uri")

            val inputStream = context.contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader().use { it?.readText() }
            inputStream?.close()

            if (json.isNullOrEmpty()) {
                Log.e(TAG, "importData: Empty or null JSON")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "文件为空或无效", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }

            Log.d(TAG, "importData: JSON size=${json.length} bytes")

            val type = object : TypeToken<ExportData>() {}.type
            val exportData: ExportData = try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                Log.e(TAG, "importData: JSON parse error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "文件格式错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }

            Log.d(TAG, "importData: Parsed data - paintings=${exportData.paintings.size}, " +
                    "nodes=${exportData.nodes.size}, diaries=${exportData.diaries.size}")

            val database = AppDatabase.getInstance(context)

            // 开始事务（Room 会自动处理，但为了性能可以分批）
            // 清空现有数据
            Log.d(TAG, "importData: Clearing existing data")

            // 删除所有作品（会级联删除节点）
            val existingWorks = database.workDao().getAllWorksSync()
            existingWorks.forEach { work ->
                database.workDao().softDeleteWork(work.id)
            }
            database.workDao().deletePermanently()

            // 删除目标
            val existingGoals = database.goalDao().getAllGoals()
            existingGoals.forEach { goal ->
                database.goalDao().deleteGoal(goal)
            }

            // 删除日记
            val existingDiaries = database.diaryDao().getAllDiariesSync()
            existingDiaries.forEach { diary ->
                database.diaryDao().softDeleteDiary(diary.id)
            }

            // 删除菜品
            val existingDishes = database.dishDao().getAllDishesSync()
            existingDishes.forEach { dish ->
                database.dishDao().deleteDish(dish)
            }

            // 删除抽选历史
            val existingHistory = database.lotteryHistoryDao().getRecentHistory()
            existingHistory.forEach { history ->
                database.lotteryHistoryDao().deleteHistory(history)
            }

            // 删除签到
            val existingCheckins = database.checkinDao().getAllCheckinsSync()
            existingCheckins.forEach { checkin ->
                database.checkinDao().deleteCheckin(checkin)
            }

            Log.d(TAG, "importData: Existing data cleared")

            // 导入新数据
            Log.d(TAG, "importData: Importing paintings")
            exportData.paintings.forEach { work ->
                try {
                    database.workDao().insertWork(work)
                } catch (e: Exception) {
                    Log.e(TAG, "importData: Failed to import work ${work.title}", e)
                }
            }

            Log.d(TAG, "importData: Importing nodes")
            exportData.nodes.forEach { node ->
                try {
                    database.nodeDao().insertNode(node)
                } catch (e: Exception) {
                    Log.e(TAG, "importData: Failed to import node ${node.id}", e)
                }
            }

            Log.d(TAG, "importData: Importing goals")
            exportData.goals.forEach { goal ->
                try {
                    database.goalDao().insertGoal(goal)
                } catch (e: Exception) {
                    Log.e(TAG, "importData: Failed to import goal ${goal.title}", e)
                }
            }

            Log.d(TAG, "importData: Importing diaries")
            exportData.diaries.forEach { diary ->
                try {
                    database.diaryDao().insertDiary(diary)
                } catch (e: Exception) {
                    Log.e(TAG, "importData: Failed to import diary ${diary.id}", e)
                }
            }

            Log.d(TAG, "importData: Importing dishes")
            exportData.dishes.forEach { dish ->
                try {
                    database.dishDao().insertDish(dish)
                } catch (e: Exception) {
                    Log.e(TAG, "importData: Failed to import dish ${dish.name}", e)
                }
            }

            Log.d(TAG, "importData: Importing lottery history")
            exportData.lotteryHistory.forEach { history ->
                try {
                    database.lotteryHistoryDao().insertHistory(history)
                } catch (e: Exception) {
                    Log.e(TAG, "importData: Failed to import history ${history.id}", e)
                }
            }

            Log.d(TAG, "importData: Importing checkins")
            exportData.checkins.forEach { checkin ->
                try {
                    database.checkinDao().insertCheckin(checkin)
                } catch (e: Exception) {
                    Log.e(TAG, "importData: Failed to import checkin ${checkin.date}", e)
                }
            }

            Log.d(TAG, "importData: Import completed successfully")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "恢复成功！请重启应用以查看数据", Toast.LENGTH_LONG).show()
            }

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "importData: Error", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }
    }

    /**
     * 导出为字符串（用于调试）
     */
    suspend fun exportToString(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(context)

            val paintings = database.workDao().getAllWorksSync()
            val nodes = mutableListOf<NodeEntity>()
            paintings.forEach { work ->
                nodes.addAll(database.nodeDao().getNodesByWorkId(work.id))
            }
            val goals = database.goalDao().getAllGoals()
            val diaries = database.diaryDao().getAllDiariesSync()
            val dishes = database.dishDao().getAllDishesSync()
            val lotteryHistory = database.lotteryHistoryDao().getRecentHistory()
            val checkins = database.checkinDao().getAllCheckinsSync()

            val exportData = ExportData(
                exportTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                version = "1.0",
                paintings = paintings,
                nodes = nodes,
                goals = goals,
                diaries = diaries,
                dishes = dishes,
                lotteryHistory = lotteryHistory,
                checkins = checkins
            )

            return@withContext gson.toJson(exportData)
        } catch (e: Exception) {
            Log.e(TAG, "exportToString: Error", e)
            return@withContext null
        }
    }

    /**
     * 清除缓存
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "clearCache: Clearing cache")

            val thumbnailsDir = FileUtils.getThumbnailsDir(context)
            if (thumbnailsDir.exists()) {
                val deleted = FileUtils.deleteDirectory(thumbnailsDir)
                Log.d(TAG, "clearCache: Deleted thumbnails dir: $deleted")
            }
            thumbnailsDir.mkdirs()

            // 清除 Glide 缓存
            try {
                Glide.get(context).clearDiskCache()
                Log.d(TAG, "clearCache: Glide disk cache cleared")
            } catch (e: Exception) {
                Log.e(TAG, "clearCache: Failed to clear Glide cache", e)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "clearCache: Error", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "清除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 导出指定作品的数据
     */
    suspend fun exportWork(context: Context, workId: Long): File? = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(context)
            val work = database.workDao().getWorkById(workId)
            val nodes = database.nodeDao().getNodesByWorkId(workId)

            val data = mapOf(
                "work" to work,
                "nodes" to nodes,
                "exportTime" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            val json = gson.toJson(data)
            val exportDir = FileUtils.getExportDir(context)
            val fileName = "work_${workId}_${System.currentTimeMillis()}.json"
            val jsonFile = File(exportDir, fileName)

            FileWriter(jsonFile).use { writer ->
                writer.write(json)
            }

            Log.d(TAG, "exportWork: Success, file=${jsonFile.absolutePath}")
            return@withContext jsonFile
        } catch (e: Exception) {
            Log.e(TAG, "exportWork: Error", e)
            return@withContext null
        }
    }

    /**
     * 导出日记数据
     */
    suspend fun exportDiary(context: Context, diaryId: Long): File? = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(context)
            val diary = database.diaryDao().getDiaryById(diaryId)

            val data = mapOf(
                "diary" to diary,
                "exportTime" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            val json = gson.toJson(data)
            val exportDir = FileUtils.getExportDir(context)
            val fileName = "diary_${diaryId}_${System.currentTimeMillis()}.json"
            val jsonFile = File(exportDir, fileName)

            FileWriter(jsonFile).use { writer ->
                writer.write(json)
            }

            Log.d(TAG, "exportDiary: Success, file=${jsonFile.absolutePath}")
            return@withContext jsonFile
        } catch (e: Exception) {
            Log.e(TAG, "exportDiary: Error", e)
            return@withContext null
        }
    }

    /**
     * 获取导出目录大小
     */
    suspend fun getExportDirSize(context: Context): Long = withContext(Dispatchers.IO) {
        try {
            val exportDir = FileUtils.getExportDir(context)
            FileUtils.getDirectorySize(exportDir)
        } catch (e: Exception) {
            Log.e(TAG, "getExportDirSize: Error", e)
            0L
        }
    }

    /**
     * 清理旧导出文件（保留最近N个）
     */
    suspend fun cleanOldExports(context: Context, keepCount: Int = 10) = withContext(Dispatchers.IO) {
        try {
            val exportDir = FileUtils.getExportDir(context)
            val files = exportDir.listFiles()?.filter { it.isFile && it.name.endsWith(".json") } ?: return@withContext

            if (files.size > keepCount) {
                val sortedFiles = files.sortedByDescending { it.lastModified() }
                val toDelete = sortedFiles.drop(keepCount)
                toDelete.forEach { file ->
                    val deleted = file.delete()
                    Log.d(TAG, "cleanOldExports: Deleted ${file.name}: $deleted")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "cleanOldExports: Error", e)
        }
    }
}