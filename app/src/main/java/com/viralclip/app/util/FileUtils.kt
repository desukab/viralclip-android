package com.viralclip.app.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

object FileUtils {

    fun Context.getFileName(uri: Uri): String {
        var name = "video"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex) ?: "video"
            }
        }
        return name
    }

    fun Context.getFileSize(uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }

    fun Context.getMimeType(uri: Uri): String {
        return contentResolver.getType(uri) ?: "video/*"
    }

    fun Context.isVideoFile(uri: Uri): Boolean {
        val mimeType = getMimeType(uri)
        return mimeType.startsWith("video/")
    }

    fun Context.getOutputDirectory(subDir: String = "ViralClip"): File {
        val dir = File(getExternalFilesDir(null), subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun Context.getTempDirectory(): File {
        val dir = File(cacheDir, "processing")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun Context.getExportDirectory(): File {
        return getOutputDirectory("Exports")
    }

    fun Context.getThumbnailDirectory(): File {
        return getOutputDirectory("Thumbnails")
    }

    fun Context.getCacheDirectory(subDir: String = "cache"): File {
        val dir = File(cacheDir, subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun Context.createTempFile(prefix: String, suffix: String = ".mp4"): File {
        val tempDir = getTempDirectory()
        return File.createTempFile(prefix, suffix, tempDir)
    }

    fun File.sizeInBytes(): Long {
        return if (exists()) length() else 0L
    }

    fun File.deleteRecursively(): Boolean {
        if (!exists()) return false
        if (isDirectory) {
            listFiles()?.forEach { it.deleteRecursively() }
        }
        return delete()
    }

    fun File.ensureExists(): File {
        if (!exists()) {
            parentFile?.mkdirs()
            createNewFile()
        }
        return this
    }

    fun File.getDirectorySize(): Long {
        var size = 0L
        if (isDirectory) {
            listFiles()?.forEach { file ->
                size += if (file.isDirectory) file.getDirectorySize() else file.length()
            }
        } else {
            size = length()
        }
        return size
    }

    fun File.getFileCount(): Int {
        if (!isDirectory) return 0
        var count = 0
        listFiles()?.forEach { file ->
            count += if (file.isDirectory) file.getFileCount() else 1
        }
        return count
    }

    fun cleanDirectory(directory: File): Long {
        var bytesFreed = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                bytesFreed += if (file.isDirectory) {
                    cleanDirectory(file) + file.length()
                } else {
                    file.length()
                }
                file.delete()
            }
        }
        return bytesFreed
    }

    fun Context.getAvailableStorageSpace(): Long {
        return getExternalFilesDir(null)?.usableSpace ?: 0L
    }

    fun Context.getTotalStorageSpace(): Long {
        return getExternalFilesDir(null)?.totalSpace ?: 0L
    }

    fun Context.hasEnoughStorage(requiredBytes: Long): Boolean {
        return getAvailableStorageSpace() >= requiredBytes
    }

    fun Context.copyUriToFile(uri: Uri, destFile: File): Long {
        var bytesCopied = 0L
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    output.write(buffer, 0, read)
                    bytesCopied += read
                }
            }
        }
        return bytesCopied
    }

    fun Context.copyFile(source: File, dest: File): Long {
        var bytesCopied = 0L
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    output.write(buffer, 0, read)
                    bytesCopied += read
                }
            }
        }
        return bytesCopied
    }

    fun File.generateChecksum(): String {
        if (!exists()) return ""
        val digest = MessageDigest.getInstance("MD5")
        FileInputStream(this).use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(100)
            .ifEmpty { "untitled" }
    }

    fun generateUniqueFileName(baseName: String, extension: String, directory: File): File {
        val sanitized = sanitizeFileName(baseName)
        val ext = if (extension.startsWith(".")) extension else ".$extension"
        var file = File(directory, "$sanitized$ext")
        var counter = 1
        while (file.exists()) {
            file = File(directory, "${sanitized}_$counter$ext")
            counter++
        }
        return file
    }

    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun Context.readFileToBytes(uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getMediaDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun getMediaDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
