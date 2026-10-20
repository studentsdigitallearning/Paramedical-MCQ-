package com.jacboard.mcqmaster.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Purple & Indigo Brand
val PrimaryPurple = Color(0xFF6C47FF)
val PrimaryPurpleLight = Color(0xFF8B5CF6)
val PrimaryPurpleDark = Color(0xFF4F26E6)
val PrimaryPurpleContainerLight = Color(0xFFEEEAFE)
val PrimaryPurpleContainerDark = Color(0xFF28205C)
val PrimaryPurpleSoftBg = Color(0xFFF5F2FF)
val PrimaryIndigo = Color(0xFF1E1644)

// Brand Gold & Yellow
val AccentGold = Color(0xFFFFB800)
val AccentGoldOrange = Color(0xFFFF9200)
val AccentOrange = Color(0xFFF97316)
val AccentGoldBgLight = Color(0xFFFFF9E6)
val AccentGoldBgDark = Color(0xFF382D12)

// Dark Theme Base (Deep Indigo Glass)
val DarkBackground = Color(0xFF0F0D2E)
val DarkSurface = Color(0xFF181545)
val DarkSurfaceElevated = Color(0xFF241F5E)
val DarkBorder = Color(0xFF2F2975)
val DarkTextPrimary = Color(0xFFF8F9FE)
val DarkTextSecondary = Color(0xFFB4B8D6)
val DarkTextTertiary = Color(0xFF7E82A8)

// Light Theme Base (Crisp Slate Indigo)
val LightBackground = Color(0xFFF8F9FE)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE2E5F2)
val LightTextPrimary = Color(0xFF151336)
val LightTextSecondary = Color(0xFF5B6082)
val LightTextTertiary = Color(0xFF8F94B2)

// Status Colors
val SuccessGreen = Color(0xFF10B981)
val SuccessGreenBg = Color(0xFFE6F8F0)
val ErrorRed = Color(0xFFEF4444)
val ErrorRedBg = Color(0xFFFEECEC)
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberBg = Color(0xFFFEF3E2)
val InfoBlue = Color(0xFF3B82F6)
val InfoBlueBg = Color(0xFFEBF3FF)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    listOf(Color(0xFF4F26E6), Color(0xFF7C56FF))
)

val PurpleGradient = Brush.horizontalGradient(
    listOf(Color(0xFF6C47FF), Color(0xFF8B5CF6))
)

val GoldGradient = Brush.horizontalGradient(
    listOf(Color(0xFFFFB800), Color(0xFFFFA000))
)

val DailyChallengeGradient = Brush.linearGradient(
    listOf(
        Color(0xFF3D1DB8),
        Color(0xFF592CE0),
        Color(0xFF7A45F5)
    )
)

val DailyChallengeDarkGradient = Brush.linearGradient(
    listOf(
        Color(0xFF1C1344),
        Color(0xFF2C1762),
        Color(0xFF431F86)
    )
)

val SplashGradient = Brush.verticalGradient(
    listOf(Color(0xFF181347), Color(0xFF0F0D2E))
)
