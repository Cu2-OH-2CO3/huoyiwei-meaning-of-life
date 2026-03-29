package com.memoria.meaningoflife.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    private const val QUALITY = 85

    fun saveImageToStorage(context: Context, bitmap: Bitmap, subDir: String, fileName: String): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToMediaStore(context, bitmap, subDir, fileName)
        } else {
            saveImageToExternalStorage(context, bitmap, subDir, fileName)
        }
    }

    private fun saveImageToMediaStore(context: Context, bitmap: Bitmap, subDir: String, fileName: String): String? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/活意味/$subDir")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, outputStream)
                return it.toString()
            }
        }

        return null
    }

    private fun saveImageToExternalStorage(context: Context, bitmap: Bitmap, subDir: String, fileName: String): String? {
        // 使用应用私有目录，不需要权限
        val appDir = File(context.getExternalFilesDir(null), subDir)

        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        val file = File(appDir, fileName)
        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteImage(path: String): Boolean {
        return try {
            val file = File(path)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}