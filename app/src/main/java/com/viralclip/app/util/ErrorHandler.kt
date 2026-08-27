package com.viralclip.app.util

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null,
    val errorCode: String = "UNKNOWN"
) : Throwable(message, cause) {

    class VideoImportError(message: String, cause: Throwable? = null) : AppError(message, cause, "VIDEO_IMPORT")
    class VideoExportError(message: String, cause: Throwable? = null) : AppError(message, cause, "VIDEO_EXPORT")
    class ProcessingError(message: String, cause: Throwable? = null) : AppError(message, cause, "PROCESSING")
    class CaptionError(message: String, cause: Throwable? = null) : AppError(message, cause, "CAPTION")
    class StorageError(message: String, cause: Throwable? = null) : AppError(message, cause, "STORAGE")
    class NetworkError(message: String, cause: Throwable? = null) : AppError(message, cause, "NETWORK")
    class ValidationError(message: String) : AppError(message, null, "VALIDATION")
    class UnknownError(message: String, cause: Throwable? = null) : AppError(message, cause, "UNKNOWN")

    fun getStackTraceString(): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        printStackTrace(pw)
        return sw.toString()
    }
}

sealed class Result<out T> {
    data class Success<out T>(val value: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.value
    fun exceptionOrNull(): AppError? = (this as? Failure)?.error

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> default
    }

    fun getOrElse(onFailure: (AppError) -> @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> onFailure(error)
    }

    companion object {
        fun <T> success(value: T): Result<T> = Success(value)
        fun failure(error: AppError): Result<Nothing> = Failure(error)
        fun failure(message: String, cause: Throwable? = null): Result<Nothing> = Failure(AppError.UnknownError(message, cause))
    }
}

inline fun <T> runCatchingApp(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: AppError) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(AppError.UnknownError(e.message ?: "Unknown error", e))
    }
}

inline fun <T> runCatchingResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e.message ?: "Unknown error", e)
    }
}

object ErrorHandler {

    private const val TAG = "ViralClipError"
    private const val MAX_LOGGED_ERRORS = 100
    private val errorLog = mutableListOf<LoggedError>()
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()

    data class LoggedError(
        val error: AppError,
        val timestamp: Long = System.currentTimeMillis(),
        val additionalContext: Map<String, Any> = emptyMap()
    )

    fun handleError(error: AppError, additionalContext: Map<String, Any> = emptyMap()) {
        Log.e(TAG, "Error [${error.errorCode}]: ${error.message}", error.cause)
        AnalyticsHelper.trackError(error.errorCode, error.cause, additionalContext)

        synchronized(errorLog) {
            errorLog.add(LoggedError(error, System.currentTimeMillis(), additionalContext))
            if (errorLog.size > MAX_LOGGED_ERRORS) {
                errorLog.removeAt(0)
            }
        }

        errorCounts.computeIfAbsent(error.errorCode) { AtomicLong(0) }.incrementAndGet()
    }

    fun handleError(throwable: Throwable, additionalContext: Map<String, Any> = emptyMap()) {
        val appError = when (throwable) {
            is AppError -> throwable
            else -> AppError.UnknownError(throwable.message ?: "Unknown error", throwable)
        }
        handleError(appError, additionalContext)
    }

    fun handleError(message: String, additionalContext: Map<String, Any> = emptyMap()) {
        handleError(AppError.UnknownError(message), additionalContext)
    }

    fun getRecentErrors(limit: Int = 50): List<LoggedError> {
        return synchronized(errorLog) {
            errorLog.takeLast(limit)
        }
    }

    fun getErrorCounts(): Map<String, Long> {
        return errorCounts.mapValues { it.value.get() }
    }

    fun clearErrors() {
        synchronized(errorLog) {
            errorLog.clear()
        }
        errorCounts.clear()
    }

    fun generateErrorReport(): String {
        val recentErrors = getRecentErrors(20)
        val counts = getErrorCounts()
        return buildString {
            appendLine("=== Error Report ===")
            appendLine("Total Errors Logged: ${errorLog.size}")
            appendLine("\nError Counts by Code:")
            counts.forEach { (code, count) ->
                appendLine("  $code: $count")
            }
            appendLine("\nRecent Errors:")
            recentErrors.forEach { logged ->
                appendLine("  [${logged.error.errorCode}] ${logged.error.message} @ ${logged.timestamp}")
            }
        }
    }
}

fun createCoroutineExceptionHandler(): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, throwable ->
        ErrorHandler.handleError(throwable)
    }
}

inline fun safeExecute(
    errorMessage: String = "Operation failed",
    additionalContext: Map<String, Any> = emptyMap(),
    block: () -> Unit
) {
    try {
        block()
    } catch (e: Exception) {
        Log.e(TAG, errorMessage, e)
        ErrorHandler.handleError(AppError.UnknownError(errorMessage, e), additionalContext)
    }
}

inline fun <T> safeExecuteWithResult(
    defaultValue: T,
    errorMessage: String = "Operation failed",
    additionalContext: Map<String, Any> = emptyMap(),
    block: () -> T
): T {
    return try {
        block()
    } catch (e: Exception) {
        Log.e(TAG, errorMessage, e)
        ErrorHandler.handleError(AppError.UnknownError(errorMessage, e), additionalContext)
        defaultValue
    }
}
