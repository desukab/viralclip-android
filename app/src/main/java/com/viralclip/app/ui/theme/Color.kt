package com.viralclip.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Brand Colors - Vibrant Purple/Pink Gradient
val ViralPurple = Color(0xFF7C3AED)
val ViralPurpleDark = Color(0xFF6D28D9)
val ViralPurpleLight = Color(0xFFA78BFA)
val ViralPink = Color(0xFFEC4899)
val ViralPinkDark = Color(0xFFDB2777)
val ViralBlue = Color(0xFF3B82F6)
val ViralCyan = Color(0xFF06B6D4)
val ViralGreen = Color(0xFF10B981)
val ViralOrange = Color(0xFFF97316)
val ViralRed = Color(0xFFEF4444)
val ViralYellow = Color(0xFFFBBF24)
val ViralIndigo = Color(0xFF6366F1)
val ViralTeal = Color(0xFF14B8A6)
val ViralAmber = Color(0xFFF59E0B)
val ViralLime = Color(0xFF84CC16)
val ViralEmerald = Color(0xFF10B981)
val ViralFuchsia = Color(0xFFD946EF)
val ViralRose = Color(0xFFF43F5E)

// Gradient Colors
val GradientPurplePink = listOf(ViralPurple, ViralPink)
val GradientBlueCyan = listOf(ViralBlue, ViralCyan)
val GradientGreenCyan = listOf(ViralGreen, ViralCyan)
val GradientOrangePink = listOf(ViralOrange, ViralPink)
val GradientPurpleCyan = listOf(ViralPurple, ViralCyan)
val GradientPinkOrange = listOf(ViralPink, ViralOrange)
val GradientYellowGreen = listOf(ViralYellow, ViralGreen)
val GradientIndigoPurple = listOf(ViralIndigo, ViralPurple)
val GradientSunset = listOf(ViralOrange, ViralPink, ViralPurple)
val GradientOcean = listOf(ViralBlue, ViralCyan, ViralTeal)
val GradientFire = listOf(ViralRed, ViralOrange, ViralYellow)
val GradientAurora = listOf(ViralGreen, ViralCyan, ViralBlue, ViralPurple)

// Dark Theme Backgrounds
val DarkBackground = Color(0xFF0A0A0F)
val DarkSurface = Color(0xFF12121A)
val DarkSurfaceElevated = Color(0xFF1A1A25)
val DarkSurfaceHighest = Color(0xFF222233)
val DarkCard = Color(0xFF16161F)
val DarkBorder = Color(0xFF2A2A3A)
val DarkDivider = Color(0xFF1F1F2E)
val DarkOverlay = Color(0x80000000)
val DarkScrim = Color(0xCC000000)

// Light Theme Backgrounds
val LightBackground = Color(0xFFF8F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF0F1F5)
val LightSurfaceHighest = Color(0xFFE8E9F0)
val LightCard = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE2E4EA)
val LightDivider = Color(0xFFE5E7EB)
val LightOverlay = Color(0x80FFFFFF)
val LightScrim = Color(0x99FFFFFF)

// Text Colors - Dark Theme
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0C0)
val TextTertiary = Color(0xFF6B6B80)
val TextDisabled = Color(0xFF4A4A5A)

// Text Colors - Light Theme
val TextPrimaryDark = Color(0xFF1A1A2E)
val TextSecondaryDark = Color(0xFF6B7280)
val TextTertiaryDark = Color(0xFF9CA3AF)
val TextDisabledDark = Color(0xFFD1D5DB)

// Accent / Functional Colors
val SuccessColor = Color(0xFF10B981)
val SuccessLight = Color(0xFFD1FAE5)
val SuccessDark = Color(0xFF065F46)
val WarningColor = Color(0xFFF59E0B)
val WarningLight = Color(0xFFFEF3C7)
val WarningDark = Color(0xFF92400E)
val ErrorColor = Color(0xFFEF4444)
val ErrorLight = Color(0xFFFEE2E2)
val ErrorDark = Color(0xFF991B1B)
val InfoColor = Color(0xFF3B82F6)
val InfoLight = Color(0xFFDBEAFE)
val InfoDark = Color(0xFF1E40AF)

// Virality Score Colors
val ViralityHigh = Color(0xFF10B981)
val ViralityMedium = Color(0xFFF59E0B)
val ViralityLow = Color(0xFFEF4444)
val ViralityScoreGradient = listOf(ViralityLow, ViralityMedium, ViralityHigh)

// Caption Highlight Colors
val CaptionHighlightYellow = Color(0xFFFBBF24)
val CaptionHighlightGreen = Color(0xFF34D399)
val CaptionHighlightPink = Color(0xFFF472B6)
val CaptionHighlightBlue = Color(0xFF60A5FA)
val CaptionHighlightPurple = Color(0xFFA78BFA)
val CaptionHighlightCyan = Color(0xFF22D3EE)
val CaptionHighlightOrange = Color(0xFFFB923C)
val CaptionHighlightRed = Color(0xFFF87171)

// Platform Brand Colors
val TikTokColor = Color(0xFF00F2EA)
val TikTokPink = Color(0xFFFF0050)
val InstagramColor = Color(0xFFE1306C)
val InstagramOrange = Color(0xFFF77737)
val InstagramYellow = Color(0xFFFCAF45)
val YouTubeColor = Color(0xFFFF0000)
val TwitterColor = Color(0xFF1DA1F2)
val FacebookColor = Color(0xFF1877F2)
val LinkedInColor = Color(0xFF0A66C2)
val PinterestColor = Color(0xFFE60023)
val SnapchatColor = Color(0xFFFFFC00)
val TwitchColor = Color(0xFF9146FF)
val DiscordColor = Color(0xFF5865F2)

// Semantic Colors
val OnPrimary = Color.White
val OnSecondary = Color.White
val OnTertiary = Color.White
val OnBackground = Color.White
val OnSurface = Color.White
val OnError = Color.White
val OnSuccess = Color.White
val OnWarning = Color.Black
val OnInfo = Color.White

// Transparency Colors
val Transparent = Color.Transparent
val WhiteAlpha10 = Color.White.copy(alpha = 0.1f)
val WhiteAlpha20 = Color.White.copy(alpha = 0.2f)
val WhiteAlpha30 = Color.White.copy(alpha = 0.3f)
val WhiteAlpha40 = Color.White.copy(alpha = 0.4f)
val WhiteAlpha50 = Color.White.copy(alpha = 0.5f)
val WhiteAlpha60 = Color.White.copy(alpha = 0.6f)
val WhiteAlpha70 = Color.White.copy(alpha = 0.7f)
val WhiteAlpha80 = Color.White.copy(alpha = 0.8f)
val WhiteAlpha90 = Color.White.copy(alpha = 0.9f)

val BlackAlpha10 = Color.Black.copy(alpha = 0.1f)
val BlackAlpha20 = Color.Black.copy(alpha = 0.2f)
val BlackAlpha30 = Color.Black.copy(alpha = 0.3f)
val BlackAlpha40 = Color.Black.copy(alpha = 0.4f)
val BlackAlpha50 = Color.Black.copy(alpha = 0.5f)
val BlackAlpha60 = Color.Black.copy(alpha = 0.6f)
val BlackAlpha70 = Color.Black.copy(alpha = 0.7f)
val BlackAlpha80 = Color.Black.copy(alpha = 0.8f)
val BlackAlpha90 = Color.Black.copy(alpha = 0.9f)

// Shimmer/Skeleton Colors
val ShimmerBase = Color(0xFF2A2A3A)
val ShimmerHighlight = Color(0xFF3A3A4A)

// Button State Colors
val ButtonPrimaryDefault = ViralPurple
val ButtonPrimaryPressed = ViralPurpleDark
val ButtonPrimaryDisabled = Color(0xFF4A4A5A)
val ButtonSecondaryDefault = DarkSurfaceHighest
val ButtonSecondaryPressed = DarkSurfaceElevated
val ButtonSecondaryDisabled = DarkSurface

// Overlay Colors
val OverlayDark = Color(0xCC000000)
val OverlayLight = Color(0x80FFFFFF)
val OverlayGradient = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))

// Canvas/Video Background
val CanvasBackground = Color(0xFF0D0D12)
val VideoPlaceholder = Color(0xFF1A1A25)
val TimelineBackground = Color(0xFF14141C)
val TrackBackground = Color(0xFF1E1E2A)

// Error State Colors
val ErrorContainer = Color(0xFF3D1515)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainer = ErrorColor
val OnErrorContainerLight = Color(0xFF991B1B)

// Success State Colors
val SuccessContainer = Color(0xFF0D3D2E)
val SuccessContainerLight = Color(0xFFD1FAE5)
val OnSuccessContainer = SuccessColor
val OnSuccessContainerLight = Color(0xFF065F46)

// Warning State Colors
val WarningContainer = Color(0xFF3D3010)
val WarningContainerLight = Color(0xFFFEF3C7)
val OnWarningContainer = WarningColor
val OnWarningContainerLight = Color(0xFF92400E)

// Info State Colors
val InfoContainer = Color(0xFF0D2B4D)
val InfoContainerLight = Color(0xFFDBEAFE)
val OnInfoContainer = InfoColor
val OnInfoContainerLight = Color(0xFF1E40AF)
