package com.memoria.meaningoflife.utils

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.memoria.meaningoflife.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

object DataExporter {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    data class ExportData(
        val exportTime: String,
        val paintings: List<Any>,
        val diaries: List<Any>,
        val dishes: List<Any>,
        val checkins: List<Any>
    )

    suspend fun exportAllData(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(context)

            val paintings = database.workDao().getAllWorksSync()
            val diaries = database.diaryDao().getAllDiariesSync()
            val dishes = database.dishDao().getAllDishesSync()
            val checkins = database.checkinDao().getAllCheckinsSync()

            val exportData = ExportData(
                exportTime = DateUtils.getCurrentDateTime().toString(),
                paintings = paintings,
                diaries = diaries,
                dishes = dishes,
                checkins = checkins
            )

            val json = gson.toJson(exportData)
            val exportDir = FileUtils.getExportDir(context)
            val fileName = "huoyiwei_export_${System.currentTimeMillis()}.json"
            val jsonFile = File(exportDir, fileName)

            FileWriter(jsonFile).use { writer ->
                writer.write(json)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出成功: $fileName", Toast.LENGTH_LONG).show()
            }

            return@withContext jsonFile
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }
    }

    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val thumbnailsDir = FileUtils.getThumbnailsDir(context)
            FileUtils.deleteDirectory(thumbnailsDir)
            thumbnailsDir.mkdirs()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "清除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}