package com.viralclip.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

object HapticFeedback {

    private const val LIGHT_CLICK_DURATION = 10L
    private const val MEDIUM_CLICK_DURATION = 20L
    private const val HEAVY_CLICK_DURATION = 30L
    private const val SUCCESS_DURATION = 50L
    private const val WARNING_DURATION = 100L
    private const val ERROR_DURATION = 150L

    fun performLightClick(context: Context) {
        vibrate(context, LIGHT_CLICK_DURATION, VibrationEffect.EFFECT_CLICK)
    }

    fun performMediumClick(context: Context) {
        vibrate(context, MEDIUM_CLICK_DURATION, VibrationEffect.EFFECT_CLICK)
    }

    fun performHeavyClick(context: Context) {
        vibrate(context, HEAVY_CLICK_DURATION, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    fun performSuccess(context: Context) {
        vibrate(context, SUCCESS_DURATION, VibrationEffect.EFFECT_CLICK)
    }

    fun performWarning(context: Context) {
        vibrate(context, WARNING_DURATION, VibrationEffect.EFFECT_DOUBLE_CLICK)
    }

    fun performError(context: Context) {
        vibrate(context, ERROR_DURATION, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    fun performSelection(context: Context) {
        vibrate(context, LIGHT_CLICK_DURATION, VibrationEffect.EFFECT_TICK)
    }

    fun performToggle(context: Context, enabled: Boolean) {
        if (enabled) {
            vibrate(context, SUCCESS_DURATION, VibrationEffect.EFFECT_CLICK)
        } else {
            vibrate(context, LIGHT_CLICK_DURATION, VibrationEffect.EFFECT_TICK)
        }
    }

    fun performDelete(context: Context) {
        vibrate(context, ERROR_DURATION, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    fun performLongPress(context: Context) {
        vibrate(context, HEAVY_CLICK_DURATION, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    private fun vibrate(context: Context, durationMs: Long, effectId: Int) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Silently fail if vibration is not available
        }
    }

    fun View.performHapticClick() {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun View.performHapticConfirm() {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun View.performHapticReject() {
        performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    fun View.performHapticLongPress() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
