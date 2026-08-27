package com.viralclip.app.util

import android.content.Context
import android.util.Log
import android.view.Choreographer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object PerformanceMonitor {

    private const val TAG = "ViralClipPerf"
    private const val FRAME_THRESHOLD_MS = 16L
    private const val SLOW_FRAME_THRESHOLD_MS = 32L

    private val frameTimings = ConcurrentHashMap<String, Long>()
    private val operationTimings = ConcurrentHashMap<String, Long>()
    private val memorySnapshots = ConcurrentHashMap<String, Long>()
    private var totalFrames = AtomicLong(0)
    private var droppedFrames = AtomicLong(0)

    fun startFrameTracking(label: String) {
        frameTimings[label] = System.nanoTime()
    }

    fun endFrameTracking(label: String) {
        val startTime = frameTimings.remove(label) ?: return
        val durationMs = (System.nanoTime() - startTime) / 1_000_000
        if (durationMs > SLOW_FRAME_THRESHOLD_MS) {
            droppedFrames.incrementAndGet()
            Log.w(TAG, "Slow frame [$label]: ${durationMs}ms")
        }
        totalFrames.incrementAndGet()
    }

    inline fun <T> trackFrame(label: String, block: () -> T): T {
        startFrameTracking(label)
        return try {
            block()
        } finally {
            endFrameTracking(label)
        }
    }

    fun startOperation(label: String) {
        operationTimings[label] = System.nanoTime()
    }

    fun endOperation(label: String, logToConsole: Boolean = true): Long {
        val startTime = operationTimings.remove(label) ?: return 0L
        val durationMs = (System.nanoTime() - startTime) / 1_000_000
        if (logToConsole) {
            Log.d(TAG, "Operation [$label] took ${durationMs}ms")
        }
        return durationMs
    }

    inline fun <T> trackOperation(label: String, logToConsole: Boolean = true, block: () -> T): T {
        startOperation(label)
        return try {
            block()
        } finally {
            endOperation(label, logToConsole)
        }
    }

    fun recordMemoryUsage(label: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        memorySnapshots[label] = usedMemory
        Log.d(TAG, "Memory [$label]: ${usedMemory}MB")
    }

    fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    }

    fun getAvailableMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.freeMemory() / 1024 / 1024
    }

    fun getMaxMemory(): Long {
        return Runtime.getRuntime().maxMemory() / 1024 / 1024
    }

    fun getTotalMemory(): Long {
        return Runtime.getRuntime().totalMemory() / 1024 / 1024
    }

    fun getMemoryUsagePercent(): Float {
        val used = getMemoryUsage().toFloat()
        val max = getMaxMemory().toFloat()
        return if (max > 0) (used / max) * 100f else 0f
    }

    fun isMemoryLow(): Boolean {
        return getMemoryUsagePercent() > 80f
    }

    fun getDroppedFrameCount(): Long = droppedFrames.get()
    fun getTotalFrameCount(): Long = totalFrames.get()
    fun getDroppedFrameRate(): Float {
        val total = totalFrames.get()
        return if (total > 0) droppedFrames.get().toFloat() / total else 0f
    }

    fun resetCounters() {
        totalFrames.set(0)
        droppedFrames.set(0)
    }

    fun logPerformanceReport() {
        val memoryUsed = getMemoryUsage()
        val memoryMax = getMaxMemory()
        val memoryPercent = getMemoryUsagePercent()
        val totalFrames = getTotalFrameCount()
        val droppedFrames = getDroppedFrameCount()
        val dropRate = getDroppedFrameRate() * 100f

        Log.i(TAG, """
            === Performance Report ===
            Memory: ${memoryUsed}MB / ${memoryMax}MB (${"%.1f".format(memoryPercent)}%)
            Frames: $totalFrames total, $droppedFrames dropped (${"%.2f".format(dropRate)}%)
            Available: ${getAvailableMemory()}MB
        """.trimIndent())
    }

    fun Choreographer.scheduleFrameCallback(callback: (Long) -> Unit): Choreographer.FrameCallback {
        val choreographer = this
        return object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                callback(frameTimeNanos)
            }
        }.also { postFrameCallback(it) }
    }
}

class PerformanceMetrics(
    val label: String,
    private val startTime: Long = System.nanoTime()
) {
    private val marks = ConcurrentHashMap<String, Long>()
    private val counters = ConcurrentHashMap<String, AtomicLong>()

    fun mark(name: String) {
        marks[name] = System.nanoTime()
    }

    fun elapsed(name: String? = null): Long {
        val endTime = System.nanoTime()
        val startMark = name?.let { marks[it] } ?: startTime
        return (endTime - startMark) / 1_000_000
    }

    fun increment(name: String, value: Long = 1) {
        counters.computeIfAbsent(name) { AtomicLong(0) }.addAndGet(value)
    }

    fun getCounter(name: String): Long {
        return counters[name]?.get() ?: 0L
    }

    fun report(): String {
        val elapsed = elapsed()
        return buildString {
            appendLine("[$label] Total: ${elapsed}ms")
            marks.forEach { (name, _) ->
                appendLine("  Mark[$name]: ${elapsed(name)}ms")
            }
            counters.forEach { (name, value) ->
                appendLine("  Count[$name]: ${value.get()}")
            }
        }
    }
}
