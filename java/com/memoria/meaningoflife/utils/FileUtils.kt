package com.memoria.meaningoflife.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileUtils {

    fun getAppStorageDir(context: Context): File {
        return context.getExternalFilesDir(null) ?: context.filesDir
    }

    fun getPaintingsDir(context: Context): File {
        val dir = File(getAppStorageDir(context), "paintings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.json")
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getDiariesDir(context: Context): File {
        val dir = File(getAppStorageDir(context), "diaries")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDiaryImagesDir(context: Context): File {
        val dir = File(getDiariesDir(context), "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getThumbnailsDir(context: Context): File {
        val dir = File(getAppStorageDir(context), "thumbnails")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExportDir(context: Context): File {
        val dir = File(getAppStorageDir(context), "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getFileSize(file: File): Long {
        return if (file.exists()) file.length() else 0
    }

    fun getDirectorySize(dir: File): Long {
        var size = 0L
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    fun deleteDirectory(dir: File): Boolean {
        return if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
            dir.delete()
        } else {
            true
        }
    }

    fun zipFiles(files: List<File>, outputFile: File): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                files.forEach { file ->
                    if (file.exists()) {
                        addToZip(file, file.name, zipOut)
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun addToZip(file: File, fileName: String, zipOut: ZipOutputStream) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                addToZip(child, "$fileName/${child.name}", zipOut)
            }
        } else {
            FileInputStream(file).use { fis ->
                val zipEntry = ZipEntry(fileName)
                zipOut.putNextEntry(zipEntry)
                fis.copyTo(zipOut)
                zipOut.closeEntry()
            }
        }
    }
}