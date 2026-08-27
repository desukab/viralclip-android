package com.viralclip.app.util

import android.content.Context
import android.os.Build
import android.util.Log

object PlatformOptimizations {

    private const val TAG = "ViralClipPlatform"

    enum class DevicePerformanceTier {
        LOW,
        MEDIUM,
        HIGH,
        FLAGSHIP
    }

    fun getPerformanceTier(context: Context): DevicePerformanceTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMemoryGB = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val processorCount = Runtime.getRuntime().availableProcessors()

        return when {
            totalMemoryGB >= 8.0 && processorCount >= 8 -> DevicePerformanceTier.FLAGSHIP
            totalMemoryGB >= 4.0 && processorCount >= 6 -> DevicePerformanceTier.HIGH
            totalMemoryGB >= 2.0 && processorCount >= 4 -> DevicePerformanceTier.MEDIUM
            else -> DevicePerformanceTier.LOW
        }
    }

    fun shouldUseHighQualityProcessing(context: Context): Boolean {
        val tier = getPerformanceTier(context)
        return tier == DevicePerformanceTier.HIGH || tier == DevicePerformanceTier.FLAGSHIP
    }

    fun getOptimalThreadCount(context: Context): Int {
        val tier = getPerformanceTier(context)
        val maxCores = Runtime.getRuntime().availableProcessors()
        return when (tier) {
            DevicePerformanceTier.FLAGSHIP -> minOf(maxCores, 8)
            DevicePerformanceTier.HIGH -> minOf(maxCores, 6)
            DevicePerformanceTier.MEDIUM -> minOf(maxCores, 4)
            DevicePerformanceTier.LOW -> minOf(maxCores, 2)
        }
    }

    fun getOptimalVideoBitrate(context: Context): Int {
        val tier = getPerformanceTier(context)
        return when (tier) {
            DevicePerformanceTier.FLAGSHIP -> 15_000_000
            DevicePerformanceTier.HIGH -> 10_000_000
            DevicePerformanceTier.MEDIUM -> 6_000_000
            DevicePerformanceTier.LOW -> 3_000_000
        }
    }

    fun getOptimalFrameRate(context: Context): Int {
        val tier = getPerformanceTier(context)
        return when (tier) {
            DevicePerformanceTier.FLAGSHIP -> 60
            DevicePerformanceTier.HIGH -> 60
            DevicePerformanceTier.MEDIUM -> 30
            DevicePerformanceTier.LOW -> 30
        }
    }

    fun getOptimalThumbnailSize(context: Context): Int {
        val tier = getPerformanceTier(context)
        return when (tier) {
            DevicePerformanceTier.FLAGSHIP -> 640
            DevicePerformanceTier.HIGH -> 480
            DevicePerformanceTier.MEDIUM -> 320
            DevicePerformanceTier.LOW -> 240
        }
    }

    fun getOptimalMaxCacheSize(context: Context): Long {
        val tier = getPerformanceTier(context)
        return when (tier) {
            DevicePerformanceTier.FLAGSHIP -> 512L * 1024 * 1024
            DevicePerformanceTier.HIGH -> 256L * 1024 * 1024
            DevicePerformanceTier.MEDIUM -> 128L * 1024 * 1024
            DevicePerformanceTier.LOW -> 64L * 1024 * 1024
        }
    }

    fun supportsHardwareDecoding(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    fun supportsHevcDecoding(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hasDecodingCapability("video/hevc")
    }

    fun supportsAv1Decoding(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasDecodingCapability("video/av01")
    }

    private fun hasDecodingCapability(mimeType: String): Boolean {
        return try {
            val mediaCodecList = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
            for (codecInfo in mediaCodecList.codecInfos) {
                if (codecInfo.isEncoder) continue
                for (type in codecInfo.supportedTypes) {
                    if (type.equals(mimeType, ignoreCase = true)) {
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            hardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            processorCount = Runtime.getRuntime().availableProcessors(),
            totalMemoryBytes = Runtime.getRuntime().maxMemory()
        )
    }

    fun logDeviceInfo() {
        val info = getDeviceInfo()
        Log.d(TAG, "Device: ${info.manufacturer} ${info.model}")
        Log.d(TAG, "Android: ${info.androidVersion} (SDK ${info.sdkInt})")
        Log.d(TAG, "ABI: ${info.abi}")
        Log.d(TAG, "Cores: ${info.processorCount}")
        Log.d(TAG, "Max Memory: ${info.totalMemoryBytes / 1024 / 1024}MB")
    }

    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val brand: String,
        val device: String,
        val product: String,
        val hardware: String,
        val androidVersion: String,
        val sdkInt: Int,
        val abi: String,
        val processorCount: Int,
        val totalMemoryBytes: Long
    )
}
