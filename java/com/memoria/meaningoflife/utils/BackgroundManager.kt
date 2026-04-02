package com.memoria.meaningoflife.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object BackgroundManager {

    private const val TAG = "BackgroundManager"
    private const val PREFS_NAME = "background_prefs"
    private const val KEY_BACKGROUND_PATH = "background_path"
    private const val KEY_BACKGROUND_ALPHA = "background_alpha"
    private const val KEY_BACKGROUND_ENABLED = "background_enabled"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "BackgroundManager initialized")
    }

    fun saveBackgroundImage(context: Context, uri: Uri): Boolean {
        return try {
            Log.d(TAG, "saveBackgroundImage: uri=$uri")

            // 获取图片
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e(TAG, "saveBackgroundImage: Failed to decode bitmap")
                return false
            }

            Log.d(TAG, "saveBackgroundImage: Original bitmap size=${bitmap.width}x${bitmap.height}")

            // 保存到应用私有目录（保持原图，不缩放）
            val backgroundDir = File(context.filesDir, "backgrounds")
            if (!backgroundDir.exists()) {
                backgroundDir.mkdirs()
                Log.d(TAG, "saveBackgroundImage: Created background dir: ${backgroundDir.absolutePath}")
            }

            val file = File(backgroundDir, "home_background.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()

            // 保存路径到 SharedPreferences
            prefs.edit().putString(KEY_BACKGROUND_PATH, file.absolutePath).apply()
            prefs.edit().putBoolean(KEY_BACKGROUND_ENABLED, true).apply()

            Log.d(TAG, "saveBackgroundImage: Success, path=${file.absolutePath}, size=${file.length()} bytes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "saveBackgroundImage: Error", e)
            false
        }
    }

    fun getBackgroundPath(): String? {
        val path = prefs.getString(KEY_BACKGROUND_PATH, null)
        Log.d(TAG, "getBackgroundPath: $path")
        return path
    }

    fun getBackgroundAlpha(): Int {
        val alpha = prefs.getInt(KEY_BACKGROUND_ALPHA, 100)
        Log.d(TAG, "getBackgroundAlpha: $alpha")
        return alpha
    }

    fun setBackgroundAlpha(alpha: Int) {
        Log.d(TAG, "setBackgroundAlpha: $alpha")
        prefs.edit().putInt(KEY_BACKGROUND_ALPHA, alpha).apply()
    }

    fun isBackgroundEnabled(): Boolean {
        val enabled = prefs.getBoolean(KEY_BACKGROUND_ENABLED, false)
        Log.d(TAG, "isBackgroundEnabled: $enabled")
        return enabled
    }

    fun clearBackground() {
        Log.d(TAG, "clearBackground")
        prefs.edit().putBoolean(KEY_BACKGROUND_ENABLED, false).apply()
        prefs.edit().remove(KEY_BACKGROUND_PATH).apply()

        // 删除图片文件
        getBackgroundPath()?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "clearBackground: Deleted file: $deleted")
            }
        }
    }

    fun getBackgroundBitmap(context: Context): Bitmap? {
        val path = getBackgroundPath()
        if (path.isNullOrEmpty()) {
            Log.d(TAG, "getBackgroundBitmap: No background path")
            return null
        }

        return try {
            val file = File(path)
            if (!file.exists()) {
                Log.e(TAG, "getBackgroundBitmap: File not exists: $path")
                return null
            }
            val bitmap = BitmapFactory.decodeFile(path)
            Log.d(TAG, "getBackgroundBitmap: Success, bitmap size=${bitmap?.width}x${bitmap?.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "getBackgroundBitmap: Error", e)
            null
        }
    }
}