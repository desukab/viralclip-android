package com.viralclip.app.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object AnalyticsHelper {

    private const val TAG = "ViralClipAnalytics"
    private const val MAX_EVENTS_IN_MEMORY = 500

    private val eventQueue = ConcurrentHashMap<String, AtomicLong>()
    private val eventLog = mutableListOf<AnalyticsEvent>()
    private val userProperties = ConcurrentHashMap<String, Any>()

    enum class EventCategory {
        USER_ACTION,
        SCREEN_VIEW,
        FEATURE_USE,
        PERFORMANCE,
        ERROR,
        CONVERSION,
        ENGAGEMENT
    }

    enum class EventName(val category: EventCategory) {
        APP_OPENED(EventCategory.ENGAGEMENT),
        APP_BACKGROUNDED(EventCategory.ENGAGEMENT),

        HOME_SCREEN_VIEWED(EventCategory.SCREEN_VIEW),
        EDITOR_SCREEN_VIEWED(EventCategory.SCREEN_VIEW),
        TEMPLATES_SCREEN_VIEWED(EventCategory.SCREEN_VIEW),
        EXPORT_SCREEN_VIEWED(EventCategory.SCREEN_VIEW),
        SETTINGS_SCREEN_VIEWED(EventCategory.SCREEN_VIEW),

        VIDEO_IMPORTED(EventCategory.USER_ACTION),
        VIDEO_IMPORT_FAILED(EventCategory.ERROR),
        VIDEO_DELETED(EventCategory.USER_ACTION),

        CLIP_GENERATED(EventCategory.FEATURE_USE),
        CLIP_SELECTED(EventCategory.USER_ACTION),
        CLIP_DELETED(EventCategory.USER_ACTION),
        CLIP_DUPLICATED(EventCategory.USER_ACTION),
        CLIP_REORDERED(EventCategory.USER_ACTION),
        CLIP_SPLIT(EventCategory.FEATURE_USE),
        CLIP_TRIMMED(EventCategory.USER_ACTION),
        CLIP_PREVIEW_PLAYED(EventCategory.ENGAGEMENT),

        CAPTION_STYLE_CHANGED(EventCategory.FEATURE_USE),
        CAPTION_PRESET_APPLIED(EventCategory.FEATURE_USE),
        CAPTION_COLOR_CHANGED(EventCategory.FEATURE_USE),
        CAPTION_FONT_CHANGED(EventCategory.FEATURE_USE),
        CAPTION_EDITED(EventCategory.USER_ACTION),

        FILTER_APPLIED(EventCategory.FEATURE_USE),
        FILTER_PRESET_CHANGED(EventCategory.FEATURE_USE),
        FILTER_ADJUSTED(EventCategory.FEATURE_USE),

        TEMPLATE_APPLIED(EventCategory.FEATURE_USE),
        TEMPLATE_PREVIEWED(EventCategory.ENGAGEMENT),
        TEMPLATE_CATEGORY_VIEWED(EventCategory.SCREEN_VIEW),

        BRAND_PRESET_CREATED(EventCategory.FEATURE_USE),
        BRAND_PRESET_APPLIED(EventCategory.FEATURE_USE),
        BRAND_PRESET_SHARED(EventCategory.CONVERSION),

        EXPORT_STARTED(EventCategory.FEATURE_USE),
        EXPORT_COMPLETED(EventCategory.CONVERSION),
        EXPORT_FAILED(EventCategory.ERROR),
        EXPORT_SHARED(EventCategory.CONVERSION),
        EXPORT_CANCELLED(EventCategory.USER_ACTION),

        VIRALITY_SCORE_VIEWED(EventCategory.ENGAGEMENT),
        VIRALITY_REASON_VIEWED(EventCategory.ENGAGEMENT),

        TEXT_OVERLAY_ADDED(EventCategory.FEATURE_USE),
        TEXT_OVERLAY_EDITED(EventCategory.USER_ACTION),
        TEXT_OVERLAY_DELETED(EventCategory.USER_ACTION),

        SETTINGS_OPENED(EventCategory.SCREEN_VIEW),
        SETTING_CHANGED(EventCategory.USER_ACTION),
        THEME_CHANGED(EventCategory.USER_ACTION),
        LANGUAGE_CHANGED(EventCategory.USER_ACTION),

        SEARCH_PERFORMED(EventCategory.USER_ACTION),
        ONBOARDING_COMPLETED(EventCategory.CONVERSION),
        TUTORIAL_VIEWED(EventCategory.ENGAGEMENT),

        UNDO_PERFORMED(EventCategory.USER_ACTION),
        REDO_PERFORMED(EventCategory.USER_ACTION),
        PROJECT_SAVED(EventCategory.USER_ACTION),
        PROJECT_OPENED(EventCategory.USER_ACTION),

        SHARE_TO_TIKTOK(EventCategory.CONVERSION),
        SHARE_TO_INSTAGRAM(EventCategory.CONVERSION),
        SHARE_TO_YOUTUBE(EventCategory.CONVERSION),
        SHARE_TO_FACEBOOK(EventCategory.CONVERSION),
        SHARE_TO_TWITTER(EventCategory.CONVERSION),
        SHARE_TO_OTHER(EventCategory.CONVERSION),

        PROCESSING_STARTED(EventCategory.PERFORMANCE),
        PROCESSING_COMPLETED(EventCategory.PERFORMANCE),
        PROCESSING_FAILED(EventCategory.ERROR),

        ERROR_OCCURRED(EventCategory.ERROR),
        CRASH_REPORTED(EventCategory.ERROR)
    }

    data class AnalyticsEvent(
        val name: String,
        val category: EventCategory,
        val timestamp: Long = System.currentTimeMillis(),
        val properties: Map<String, Any> = emptyMap(),
        val sessionId: String? = null,
        val userId: String? = null
    )

    fun initialize(context: Context, userId: String? = null) {
        userId?.let { setUserProperty("user_id", it) }
        setUserProperty("app_version", getAppVersion(context))
        setUserProperty("device_model", android.os.Build.MODEL)
        setUserProperty("os_version", "Android ${android.os.Build.VERSION.RELEASE}")
        setUserProperty("sdk_int", android.os.Build.VERSION.SDK_INT)
        trackEvent(EventName.APP_OPENED)
    }

    fun trackEvent(
        event: EventName,
        properties: Map<String, Any> = emptyMap()
    ) {
        val analyticsEvent = AnalyticsEvent(
            name = event.name,
            category = event.category,
            properties = properties,
            sessionId = getCurrentSessionId(),
            userId = userProperties["user_id"] as? String
        )

        synchronized(eventLog) {
            eventLog.add(analyticsEvent)
            if (eventLog.size > MAX_EVENTS_IN_MEMORY) {
                eventLog.removeAt(0)
            }
        }

        eventQueue.computeIfAbsent(event.name) { AtomicLong(0) }.incrementAndGet()

        if (properties.isNotEmpty()) {
            Log.d(TAG, "Event: ${event.name} | Category: ${event.category} | Properties: $properties")
        } else {
            Log.d(TAG, "Event: ${event.name} | Category: ${event.category}")
        }
    }

    fun trackEvent(
        eventName: String,
        category: EventCategory,
        properties: Map<String, Any> = emptyMap()
    ) {
        val analyticsEvent = AnalyticsEvent(
            name = eventName,
            category = category,
            properties = properties
        )

        synchronized(eventLog) {
            eventLog.add(analyticsEvent)
            if (eventLog.size > MAX_EVENTS_IN_MEMORY) {
                eventLog.removeAt(0)
            }
        }

        Log.d(TAG, "Custom Event: $eventName | Category: $category | Properties: $properties")
    }

    fun trackScreen(screenName: String, properties: Map<String, Any> = emptyMap()) {
        trackEvent(
            eventName = "screen_view_$screenName",
            category = EventCategory.SCREEN_VIEW,
            properties = mapOf("screen_name" to screenName) + properties
        )
    }

    fun trackError(
        errorName: String,
        throwable: Throwable? = null,
        properties: Map<String, Any> = emptyMap()
    ) {
        val errorProperties = properties.toMutableMap().apply {
            put("error_name", errorName)
            throwable?.let {
                put("error_message", it.message ?: "Unknown")
                put("error_type", it.javaClass.simpleName)
                put("stack_trace", Log.getStackTraceString(it).take(2000))
            }
        }
        trackEvent(EventName.ERROR_OCCURRED, errorProperties)
    }

    fun trackPerformance(
        operation: String,
        durationMs: Long,
        properties: Map<String, Any> = emptyMap()
    ) {
        trackEvent(
            eventName = "performance_$operation",
            category = EventCategory.PERFORMANCE,
            properties = mapOf(
                "operation" to operation,
                "duration_ms" to durationMs
            ) + properties
        )
    }

    fun setUserProperty(key: String, value: Any) {
        userProperties[key] = value
    }

    fun getUserProperty(key: String): Any? = userProperties[key]

    fun getAllUserProperties(): Map<String, Any> = userProperties.toMap()

    fun incrementEventCount(eventName: String) {
        eventQueue.computeIfAbsent(eventName) { AtomicLong(0) }.incrementAndGet()
    }

    fun getEventCount(eventName: String): Long {
        return eventQueue[eventName]?.get() ?: 0L
    }

    fun getAllEventCounts(): Map<String, Long> {
        return eventQueue.mapValues { it.value.get() }
    }

    fun getRecentEvents(limit: Int = 100): List<AnalyticsEvent> {
        return synchronized(eventLog) {
            eventLog.takeLast(limit)
        }
    }

    fun clearEvents() {
        synchronized(eventLog) {
            eventLog.clear()
        }
        eventQueue.clear()
    }

    fun startTimedEvent(eventName: String): Long {
        val startTime = System.currentTimeMillis()
        userProperties["timed_event_$eventName"] = startTime
        return startTime
    }

    fun endTimedEvent(eventName: String, additionalProperties: Map<String, Any> = emptyMap()): Long {
        val startTime = userProperties.remove("timed_event_$eventName") as? Long ?: return 0L
        val duration = System.currentTimeMillis() - startTime
        trackEvent(
            eventName = "timed_$eventName",
            category = EventCategory.PERFORMANCE,
            properties = mapOf("duration_ms" to duration) + additionalProperties
        )
        return duration
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun getCurrentSessionId(): String {
        return userProperties["session_id"] as? String ?: run {
            val newSessionId = java.util.UUID.randomUUID().toString()
            setUserProperty("session_id", newSessionId)
            newSessionId
        }
    }

    fun startNewSession() {
        setUserProperty("session_id", java.util.UUID.randomUUID().toString())
        trackEvent(EventName.APP_OPENED)
    }

    fun generateAnalyticsReport(): String {
        val recentEvents = getRecentEvents(50)
        val eventCounts = getAllEventCounts()
        val totalEvents = eventCounts.values.sum()

        return buildString {
            appendLine("=== Analytics Report ===")
            appendLine("Total Events: $totalEvents")
            appendLine("Unique Event Types: ${eventCounts.size}")
            appendLine("Recent Events (last ${recentEvents.size}):")
            recentEvents.takeLast(20).forEach { event ->
                appendLine("  - ${event.name} [${event.category}] @ ${event.timestamp}")
            }
            appendLine("\nUser Properties:")
            userProperties.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
        }
    }
}
