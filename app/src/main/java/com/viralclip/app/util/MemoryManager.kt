package com.viralclip.app.util

import android.util.Log
import androidx.collection.LruCache
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MemoryCache<K : Any, V : Any>(maxSize: Int) : LruCache<K, V>(maxSize) {
    private val accessCounts = ConcurrentHashMap<K, Int>()

    override fun sizeOf(key: K, value: V): Int {
        return when (value) {
            is Bitmap -> value.byteCount / 1024
            is String -> value.length
            is ByteArray -> value.size / 1024
            else -> 1
        }
    }

    override fun get(key: K): V? {
        accessCounts.compute(key) { _, count -> (count ?: 0) + 1 }
        return super.get(key)
    }

    override fun put(key: K, value: V): V? {
        return super.put(key, value)
    }

    fun getAccessCount(key: K): Int = accessCounts[key] ?: 0

    fun getMostAccessedKeys(limit: Int = 10): List<Pair<K, Int>> {
        return accessCounts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }
}

object CacheManager {

    private val memoryCaches = ConcurrentHashMap<String, MemoryCache<*, *>>()

    fun <K : Any, V : Any> createMemoryCache(name: String, maxSizeInKb: Int): MemoryCache<K, V> {
        val cache = MemoryCache<K, V>(maxSizeInKb)
        memoryCaches[name] = cache
        return cache
    }

    fun getMemoryCache(name: String): MemoryCache<*, *>? = memoryCaches[name]

    fun clearAllMemoryCaches() {
        memoryCaches.values.forEach { it.evictAll() }
        memoryCaches.clear()
    }

    fun clearMemoryCache(name: String) {
        memoryCaches[name]?.evictAll()
    }

    fun getAllCacheNames(): List<String> = memoryCaches.keys.toList()

    fun getTotalCacheSize(): Int {
        return memoryCaches.values.sumOf { it.size() }
    }

    fun getTotalMaxCacheSize(): Int {
        return memoryCaches.values.sumOf { it.maxSize() }
    }
}

object MemoryManager {

    private const val TAG = "ViralClipMemory"
    private const val LOW_MEMORY_THRESHOLD_PERCENT = 80f
    private const val CRITICAL_MEMORY_THRESHOLD_PERCENT = 95f

    fun getUsedMemoryMB(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    }

    fun getMaxMemoryMB(): Long {
        return Runtime.getRuntime().maxMemory() / 1024 / 1024
    }

    fun getAvailableMemoryMB(): Long {
        return Runtime.getRuntime().freeMemory() / 1024 / 1024
    }

    fun getMemoryUsagePercent(): Float {
        val used = getUsedMemoryMB().toFloat()
        val max = getMaxMemoryMB().toFloat()
        return if (max > 0) (used / max) * 100f else 0f
    }

    fun isLowMemory(): Boolean {
        return getMemoryUsagePercent() >= LOW_MEMORY_THRESHOLD_PERCENT
    }

    fun isCriticalMemory(): Boolean {
        return getMemoryUsagePercent() >= CRITICAL_MEMORY_THRESHOLD_PERCENT
    }

    fun requestGC() {
        System.gc()
        System.runFinalization()
        System.gc()
    }

    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        return MemoryStats(
            usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
            totalMemory = runtime.totalMemory() / 1024 / 1024,
            maxMemory = runtime.maxMemory() / 1024 / 1024,
            freeMemory = runtime.freeMemory() / 1024 / 1024,
            usagePercent = getMemoryUsagePercent(),
            isLowMemory = isLowMemory(),
            isCriticalMemory = isCriticalMemory()
        )
    }

    fun logMemoryUsage(tag: String) {
        val stats = getMemoryStats()
        Log.d(TAG, "[$tag] Memory: ${stats.usedMemory}MB / ${stats.maxMemory}MB (${"%.1f".format(stats.usagePercent)}%)")
    }

    fun tryToFreeMemory() {
        CacheManager.clearAllMemoryCaches()
        ImageUtils.clearCache()
        requestGC()
        logMemoryUsage("AfterFree")
    }

    data class MemoryStats(
        val usedMemory: Long,
        val totalMemory: Long,
        val maxMemory: Long,
        val freeMemory: Long,
        val usagePercent: Float,
        val isLowMemory: Boolean,
        val isCriticalMemory: Boolean
    )
}

object DiskCache {

    fun getDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) getDirectorySize(file) else file.length()
            }
        }
        return size
    }

    fun cleanOldFiles(directory: File, maxAgeMs: Long): Long {
        var bytesFreed = 0L
        val now = System.currentTimeMillis()
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAgeMs) {
                    bytesFreed += if (file.isDirectory) {
                        cleanOldFiles(file, maxAgeMs) + file.length()
                    } else {
                        file.length()
                    }
                    file.delete()
                }
            }
        }
        return bytesFreed
    }

    fun cleanLargestFiles(directory: File, count: Int): Long {
        var bytesFreed = 0L
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()?.toMutableList() ?: return 0L
            files.sortByDescending { it.length() }
            files.take(count).forEach { file ->
                bytesFreed += if (file.isDirectory) {
                    cleanLargestFiles(file, Int.MAX_VALUE) + file.length()
                } else {
                    file.length()
                }
                file.delete()
            }
        }
        return bytesFreed
    }

    fun limitCacheSize(directory: File, maxSizeBytes: Long): Long {
        if (!directory.exists()) return 0L
        val currentSize = getDirectorySize(directory)
        if (currentSize <= maxSizeBytes) return 0L

        val files = directory.listFiles()?.toMutableList() ?: return 0L
        files.sortBy { it.lastModified() }

        var bytesFreed = 0L
        var freedTarget = currentSize - maxSizeBytes

        for (file in files) {
            if (freedTarget <= 0) break
            val fileSize = if (file.isDirectory) {
                getDirectorySize(file) + file.length()
            } else {
                file.length()
            }
            if (file.isDirectory) {
                cleanOldFiles(file, 0)
            } else {
                file.delete()
            }
            bytesFreed += fileSize
            freedTarget -= fileSize
        }
        return bytesFreed
    }
}
