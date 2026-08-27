package com.viralclip.app.util

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object AnimationUtils {

    const val DURATION_FAST = 150
    const val DURATION_NORMAL = 300
    const val DURATION_SLOW = 500
    const val DURATION_EXTRA_SLOW = 800

    val FastOutSlowIn = FastOutSlowInEasing
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> springSpec(
        dampingRatio: Float = Spring.DampingRatioMediumBouncy,
        stiffness: Float = Spring.StiffnessMediumLow
    ): AnimationSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

    fun fadeInUp(duration: Int = DURATION_NORMAL): EnterTransition = slideInVertically(
        animationSpec = tween(duration, easing = EmphasizedDecelerate),
        initialOffsetY = { it / 4 }
    ) + fadeIn(animationSpec = tween(duration))

    fun fadeInDown(duration: Int = DURATION_NORMAL): EnterTransition = slideInVertically(
        animationSpec = tween(duration, easing = EmphasizedDecelerate),
        initialOffsetY = { -it / 4 }
    ) + fadeIn(animationSpec = tween(duration))

    fun fadeInLeft(duration: Int = DURATION_NORMAL): EnterTransition = slideInHorizontally(
        animationSpec = tween(duration, easing = EmphasizedDecelerate),
        initialOffsetX = { -it / 4 }
    ) + fadeIn(animationSpec = tween(duration))

    fun fadeInRight(duration: Int = DURATION_NORMAL): EnterTransition = slideInHorizontally(
        animationSpec = tween(duration, easing = EmphasizedDecelerate),
        initialOffsetX = { it / 4 }
    ) + fadeIn(animationSpec = tween(duration))

    fun fadeOutUp(duration: Int = DURATION_NORMAL): ExitTransition = slideOutVertically(
        animationSpec = tween(duration, easing = EmphasizedAccelerate),
        targetOffsetY = { -it / 4 }
    ) + fadeOut(animationSpec = tween(duration))

    fun fadeOutDown(duration: Int = DURATION_NORMAL): ExitTransition = slideOutVertically(
        animationSpec = tween(duration, easing = EmphasizedAccelerate),
        targetOffsetY = { it / 4 }
    ) + fadeOut(animationSpec = tween(duration))

    fun scaleInCenter(duration: Int = DURATION_NORMAL): EnterTransition = scaleIn(
        animationSpec = tween(duration, easing = EmphasizedDecelerate),
        initialScale = 0.8f
    ) + fadeIn(animationSpec = tween(duration))

    fun scaleOutCenter(duration: Int = DURATION_NORMAL): ExitTransition = scaleOut(
        animationSpec = tween(duration, easing = EmphasizedAccelerate),
        targetScale = 0.8f
    ) + fadeOut(animationSpec = tween(duration))

    fun slideInFromRight(duration: Int = DURATION_NORMAL): EnterTransition = slideInHorizontally(
        animationSpec = tween(duration, easing = EmphasizedDecelerate),
        initialOffsetX = { it }
    ) + fadeIn(animationSpec = tween(duration))

    fun slideOutToLeft(duration: Int = DURATION_NORMAL): ExitTransition = slideOutHorizontally(
        animationSpec = tween(duration, easing = EmphasizedAccelerate),
        targetOffsetX = { -it }
    ) + fadeOut(animationSpec = tween(duration))
}

@Composable
fun rememberShimmerAnimation(
    durationMs: Int = 1200,
    enabled: Boolean = true
): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    return if (enabled) progress else 0f
}

@Composable
fun rememberPulseAnimation(
    minAlpha: Float = 0.3f,
    maxAlpha: Float = 1f,
    durationMs: Int = 1000
): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    return alpha
}

@Composable
fun rememberBounceAnimation(
    amplitude: Float = 0.1f,
    durationMs: Int = 800
): Float {
    val transition = rememberInfiniteTransition(label = "bounce")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceOffset"
    )
    return (kotlin.math.sin(offset * Math.PI * 2) * amplitude).toFloat()
}

@Composable
fun rememberRotationAnimation(
    durationMs: Int = 2000
): Float {
    val transition = rememberInfiniteTransition(label = "rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    return rotation
}

@Composable
fun Modifier.shimmerEffect(
    enabled: Boolean = true,
    colors: List<Color> = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.05f)
    )
): Modifier {
    val progress = rememberShimmerAnimation(enabled = enabled)
    if (!enabled) return this

    val brush = Brush.linearGradient(
        colors = colors,
        start = androidx.compose.ui.geometry.Offset(progress * 1000f - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(progress * 1000f + 200f, 0f)
    )
    return this.background(brush)
}

@Composable
fun rememberCountdownAnimation(
    durationMs: Int,
    onComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(durationMs.toLong())
        onComplete()
    }
}

@Composable
fun rememberDelayedVisibility(
    delayMs: Int = 100,
    initialVisible: Boolean = false
): Boolean {
    var visible by remember { mutableStateOf(initialVisible) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    return visible
}
