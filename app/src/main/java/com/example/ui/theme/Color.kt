package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Crisp Minimalist Dark Studio Palette
val ObsidianBg = Color(0xFF0B0D12)
val StudioSurface = Color(0xFF131720)
val StudioSurfaceElevated = Color(0xFF1A202C)
val StudioSurfaceHigh = Color(0xFF222938)
val HairlineBorder = Color(0xFF283144)

val TextPrimary = Color(0xFFF3F6FC)
val TextSecondary = Color(0xFF93A0B8)
val TextMuted = Color(0xFF5D6A82)

// Primary Studio Accents
val StudioCyan = Color(0xFF00E5FF)
val StudioIndigo = Color(0xFF6C63FF)
val StudioMint = Color(0xFF2EFF9A)

// FNF 4-Lane Canonical Colors (Left, Down, Up, Right)
val LaneLeftColor = Color(0xFFFF2E93)   // Magenta / Purple
val LaneDownColor = Color(0xFF00E5FF)   // Cyan
val LaneUpColor = Color(0xFF2EFF9A)     // Acid Mint
val LaneRightColor = Color(0xFFFF5E5B)  // Solar Coral / Red

// Scratch Block Category Colors
val ScratchTriggerColor = Color(0xFFF59E0B)
val ScratchArrowsColor = Color(0xFF06B6D4)
val ScratchCharacterColor = Color(0xFF8B5CF6)
val ScratchCameraColor = Color(0xFFEC4899)
val ScratchHudColor = Color(0xFF10B981)

val ErrorRed = Color(0xFFFF5263)
val WarningAmber = Color(0xFFFFB020)
val SuccessGreen = Color(0xFF22E58A)

fun laneColor(laneIndex: Int): Color = when (laneIndex % 4) {
    0 -> LaneLeftColor
    1 -> LaneDownColor
    2 -> LaneUpColor
    else -> LaneRightColor
}
