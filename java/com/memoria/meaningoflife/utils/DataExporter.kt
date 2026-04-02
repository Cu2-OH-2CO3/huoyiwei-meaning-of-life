package com.memoria.meaningoflife.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.data.database.checkin.CheckinEntity
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.data.database.lunch.LotteryHistoryEntity
import com.memoria.meaningoflife.data.database.painting.GoalEntity
import com.memoria.meaningoflife.data.database.painting.NodeEntity
import com.memoria.meaningoflife.data.database.painting.WorkEntity
import com.memoria.meaningoflife.data.database.task.TaskEntity
import com.memoria.meaningoflife.data.database.task.TaskNodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DataExporter {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    data class BackupData(
        val version: Int = 1,
        val exportTime: String = dateFormat.format(Date()),
        val works: List<WorkEntity> = emptyList(),
        val nodes: List<NodeEntity> = emptyList(),
        val goals: List<GoalEntity> = emptyList(),
        val diaries: List<DiaryEntity> = emptyList(),
        val dishes: List<DishEntity> = emptyList(),
        val lotteryHistory: List<LotteryHistoryEntity> = emptyList(),
        val checkins: List<CheckinEntity> = emptyList(),
        val tasks: List<TaskEntity> = emptyList(),
        val taskNodes: List<TaskNodeEntity> = emptyList()
    )

    /**
     * 导出所有数据到 JSON 文件
     */
    suspend fun exportAllData(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(context)

            val backupData = BackupData(
                works = database.workDao().getAllWorksSync(),
                nodes = database.nodeDao().getAllNodesSync(),
                goals = database.goalDao().getAllGoalsSync(),
                diaries = database.diaryDao().getAllDiariesSync(),
                dishes = database.dishDao().getAllDishesSync(),
                lotteryHistory = database.lotteryHistoryDao().getAllHistorySync(),
                checkins = database.checkinDao().getAllCheckinsSync(),
                tasks = database.taskDao().getAllTasksSync(),
                taskNodes = database.taskNodeDao().getAllTaskNodesSync()
            )

            val json = gson.toJson(backupData)
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupDir = File(downloadDir, "HuoyiweiBackups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val fileName = "backup_${System.currentTimeMillis()}.json"
            val file = File(backupDir, fileName)
            FileOutputStream(file).use { outputStream ->
                outputStream.write(json.toByteArray())
            }

            return@withContext file
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * 从 JSON 文件导入数据
     */
    suspend fun importData(context: Context, file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = file.readText()
            importDataFromJson(context, json)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从 JSON 字符串导入数据
     */
    suspend fun importDataFromJson(context: Context, json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("DataExporter", "importDataFromJson: json length=${json.length}")
            Log.d("DataExporter", "importDataFromJson: json preview=${json.take(500)}")

            val database = AppDatabase.getInstance(context)
            val jsonObject = JSONObject(json)

            // 打印所有顶级键
            Log.d("DataExporter", "importDataFromJson: keys=${jsonObject.keys().asSequence().toList()}")

            // 检查每个数组
            Log.d("DataExporter", "importDataFromJson: works=${jsonObject.optJSONArray("works")?.length()}")
            Log.d("DataExporter", "importDataFromJson: nodes=${jsonObject.optJSONArray("nodes")?.length()}")
            Log.d("DataExporter", "importDataFromJson: goals=${jsonObject.optJSONArray("goals")?.length()}")
            Log.d("DataExporter", "importDataFromJson: diaries=${jsonObject.optJSONArray("diaries")?.length()}")
            Log.d("DataExporter", "importDataFromJson: dishes=${jsonObject.optJSONArray("dishes")?.length()}")
            Log.d("DataExporter", "importDataFromJson: lotteryHistory=${jsonObject.optJSONArray("lotteryHistory")?.length()}")
            Log.d("DataExporter", "importDataFromJson: checkins=${jsonObject.optJSONArray("checkins")?.length()}")
            Log.d("DataExporter", "importDataFromJson: tasks=${jsonObject.optJSONArray("tasks")?.length()}")
            Log.d("DataExporter", "importDataFromJson: taskNodes=${jsonObject.optJSONArray("taskNodes")?.length()}")

            database.runInTransaction {
                // 清空所有表
                database.workDao().deleteAllSync()
                database.nodeDao().deleteAllSync()
                database.goalDao().deleteAllSync()
                database.diaryDao().deleteAllSync()
                database.dishDao().deleteAllSync()
                database.lotteryHistoryDao().deleteAllSync()
                database.checkinDao().deleteAllSync()
                database.taskDao().deleteAllSync()
                database.taskNodeDao().deleteAllSync()

                Log.d("DataExporter", "importDataFromJson: cleared all tables")

                // 导入数据
                importWorks(database, jsonObject)
                importNodes(database, jsonObject)
                importGoals(database, jsonObject)
                importDiaries(database, jsonObject)
                importDishes(database, jsonObject)
                importLotteryHistory(database, jsonObject)
                importCheckins(database, jsonObject)
                importTasks(database, jsonObject)
                importTaskNodes(database, jsonObject)

                Log.d("DataExporter", "importDataFromJson: all data imported")
            }
            true
        } catch (e: Exception) {
            Log.e("DataExporter", "importDataFromJson failed", e)
            false
        }
    }

    private fun importDiaries(database: AppDatabase, jsonObject: JSONObject) {
        val diariesArray = jsonObject.optJSONArray("diaries")
        Log.d("DataExporter", "importDiaries: array length=${diariesArray?.length()}")
        if (diariesArray != null) {
            for (i in 0 until diariesArray.length()) {
                val diaryObj = diariesArray.getJSONObject(i)
                Log.d("DataExporter", "importDiaries: diary $i, title=${diaryObj.optString("title")}, date=${diaryObj.optString("createdDate")}")
                val diary = DiaryEntity(
                    id = diaryObj.optLong("id", 0),
                    title = diaryObj.optString("title", null),
                    content = diaryObj.optString("content", ""),
                    mood = diaryObj.optInt("mood", 0),
                    weather = if (diaryObj.has("weather") && !diaryObj.isNull("weather"))
                        diaryObj.optInt("weather") else null,
                    tags = diaryObj.optString("tags", null),
                    images = diaryObj.optString("images", null),
                    createdDate = diaryObj.optString("createdDate", ""),
                    createdTime = diaryObj.optLong("createdTime", 0),
                    updatedTime = diaryObj.optLong("updatedTime", 0),
                    isDeleted = diaryObj.optBoolean("isDeleted", false)
                )
                database.diaryDao().insertDiary(diary)
                Log.d("DataExporter", "importDiaries: inserted diary id=${diary.id}")
            }
        }
    }

    // ==================== 导入方法 ====================

    private fun importWorks(database: AppDatabase, jsonObject: JSONObject) {
        val worksArray = jsonObject.optJSONArray("works")
        if (worksArray != null) {
            for (i in 0 until worksArray.length()) {
                val workObj = worksArray.getJSONObject(i)
                val work = WorkEntity(
                    id = workObj.optLong("id", 0),
                    title = workObj.optString("title", ""),
                    description = workObj.optString("description", null),
                    finalImagePath = workObj.optString("finalImagePath", ""),
                    totalDuration = workObj.optInt("totalDuration", 0),
                    tags = workObj.optString("tags", null),
                    createdDate = workObj.optString("createdDate", ""),
                    createdTime = workObj.optLong("createdTime", 0),
                    updatedTime = workObj.optLong("updatedTime", 0),
                    isDeleted = workObj.optBoolean("isDeleted", false)
                )
                database.workDao().insertWork(work)
            }
        }
    }

    private fun importNodes(database: AppDatabase, jsonObject: JSONObject) {
        val nodesArray = jsonObject.optJSONArray("nodes")
        if (nodesArray != null) {
            for (i in 0 until nodesArray.length()) {
                val nodeObj = nodesArray.getJSONObject(i)
                val node = NodeEntity(
                    id = nodeObj.optLong("id", 0),
                    workId = nodeObj.optLong("workId", 0),
                    nodeOrder = nodeObj.optInt("nodeOrder", 0),
                    imagePath = nodeObj.optString("imagePath", ""),
                    duration = nodeObj.optInt("duration", 0),
                    cumulativeDuration = nodeObj.optInt("cumulativeDuration", 0),
                    note = nodeObj.optString("note", null),
                    referenceImagePath = nodeObj.optString("referenceImagePath", null),
                    inspiration = nodeObj.optString("inspiration", null),
                    createdTime = nodeObj.optLong("createdTime", 0)
                )
                database.nodeDao().insertNode(node)
            }
        }
    }

    private fun importGoals(database: AppDatabase, jsonObject: JSONObject) {
        val goalsArray = jsonObject.optJSONArray("goals")
        if (goalsArray != null) {
            for (i in 0 until goalsArray.length()) {
                val goalObj = goalsArray.getJSONObject(i)
                val goal = GoalEntity(
                    id = goalObj.optLong("id", 0),
                    title = goalObj.optString("title", ""),
                    description = goalObj.optString("description", null),
                    targetDate = goalObj.optString("targetDate", null),
                    startDate = goalObj.optString("startDate", ""),
                    targetType = goalObj.optInt("targetType", 0),
                    targetValue = goalObj.optInt("targetValue", 0),
                    currentValue = goalObj.optInt("currentValue", 0),
                    status = goalObj.optInt("status", 0),
                    createdTime = goalObj.optLong("createdTime", 0),
                    completedTime = if (goalObj.has("completedTime") && !goalObj.isNull("completedTime"))
                        goalObj.optLong("completedTime") else null
                )
                database.goalDao().insertGoal(goal)
            }
        }
    }





    private fun importDishes(database: AppDatabase, jsonObject: JSONObject) {
        val dishesArray = jsonObject.optJSONArray("dishes")
        if (dishesArray != null) {
            for (i in 0 until dishesArray.length()) {
                val dishObj = dishesArray.getJSONObject(i)
                val dish = DishEntity(
                    id = dishObj.optLong("id", 0),
                    name = dishObj.optString("name", ""),
                    cuisine = dishObj.optString("cuisine", null),
                    spicyLevel = dishObj.optInt("spicyLevel", 0),
                    sortOrder = dishObj.optInt("sortOrder", 0),
                    isActive = dishObj.optBoolean("isActive", true),
                    createdTime = dishObj.optLong("createdTime", 0)
                )
                database.dishDao().insertDish(dish)
            }
        }
    }

    private fun importLotteryHistory(database: AppDatabase, jsonObject: JSONObject) {
        val lotteryArray = jsonObject.optJSONArray("lotteryHistory")
        if (lotteryArray != null) {
            for (i in 0 until lotteryArray.length()) {
                val lotteryObj = lotteryArray.getJSONObject(i)
                val history = LotteryHistoryEntity(
                    id = lotteryObj.optLong("id", 0),
                    dishId = lotteryObj.optLong("dishId", 0),
                    dishName = lotteryObj.optString("dishName", ""),
                    selectedDate = lotteryObj.optString("selectedDate", ""),
                    selectedTime = lotteryObj.optLong("selectedTime", 0)
                )
                database.lotteryHistoryDao().insertHistory(history)
            }
        }
    }

    private fun importCheckins(database: AppDatabase, jsonObject: JSONObject) {
        val checkinsArray = jsonObject.optJSONArray("checkins")
        if (checkinsArray != null) {
            for (i in 0 until checkinsArray.length()) {
                val checkinObj = checkinsArray.getJSONObject(i)
                val checkin = CheckinEntity(
                    date = checkinObj.optString("date", ""),
                    createdTime = checkinObj.optLong("createdTime", 0),
                    note = checkinObj.optString("note", null)
                )
                database.checkinDao().insertCheckin(checkin)
            }
        }
    }

    private fun importTasks(database: AppDatabase, jsonObject: JSONObject) {
        val tasksArray = jsonObject.optJSONArray("tasks")
        if (tasksArray != null) {
            for (i in 0 until tasksArray.length()) {
                val taskObj = tasksArray.getJSONObject(i)
                val task = TaskEntity(
                    id = taskObj.optLong("id", 0),
                    title = taskObj.optString("title", ""),
                    description = taskObj.optString("description", null),
                    isUrgent = taskObj.optBoolean("isUrgent", false),
                    isImportant = taskObj.optBoolean("isImportant", false),
                    deadline = if (taskObj.has("deadline") && !taskObj.isNull("deadline"))
                        taskObj.optLong("deadline") else null,
                    createdAt = taskObj.optLong("createdAt", 0),
                    completedAt = if (taskObj.has("completedAt") && !taskObj.isNull("completedAt"))
                        taskObj.optLong("completedAt") else null,
                    isDeleted = taskObj.optBoolean("isDeleted", false)
                )
                database.taskDao().insertTask(task)
            }
        }
    }

    private fun importTaskNodes(database: AppDatabase, jsonObject: JSONObject) {
        val taskNodesArray = jsonObject.optJSONArray("taskNodes")
        if (taskNodesArray != null) {
            for (i in 0 until taskNodesArray.length()) {
                val nodeObj = taskNodesArray.getJSONObject(i)
                val node = TaskNodeEntity(
                    id = nodeObj.optLong("id", 0),
                    taskId = nodeObj.optLong("taskId", 0),
                    title = nodeObj.optString("title", ""),
                    description = nodeObj.optString("description", null),
                    deadline = if (nodeObj.has("deadline") && !nodeObj.isNull("deadline"))
                        nodeObj.optLong("deadline") else null,
                    isCompleted = nodeObj.optBoolean("isCompleted", false),
                    completedAt = if (nodeObj.has("completedAt") && !nodeObj.isNull("completedAt"))
                        nodeObj.optLong("completedAt") else null,
                    sortOrder = nodeObj.optInt("sortOrder", 0),
                    createdAt = nodeObj.optLong("createdAt", 0)
                )
                database.taskNodeDao().insertNode(node)
            }
        }
    }

    /**
     * 获取所有备份文件
     */
    fun getBackupFiles(context: Context): List<File> {
        val backupDir = File(context.filesDir, "backups")
        return backupDir.listFiles { file -> file.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 删除备份文件
     */
    fun deleteBackupFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}