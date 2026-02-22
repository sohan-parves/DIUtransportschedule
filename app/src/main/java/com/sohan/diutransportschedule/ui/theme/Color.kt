package com.sohan.diutransportschedule.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Brand palette (Green, White, Deep Blue) =====
val DeepBlue = Color(0xFF0B2E6D)
val DeepBlueDark = Color(0xFF071F4B)
val AccentGreen = Color(0xFF18A558)
val White = Color(0xFFFFFFFF)

// ===== Backward compatible names (old code compile) =====
val PrimaryBlue = DeepBlue
val PrimaryBlueDark = DeepBlueDark

// ===== App surfaces =====
val AppBackgroundLight = Color(0xFFF6F8FF)
// Dark background should be noticeably darker than card surface
val AppBackgroundDark = Color(0xFF070B17)

val CardSurfaceLight = Color(0xFFFFFFFF)
// Dark card surface slightly lighter for clear separation
val CardSurfaceDark = Color(0xFF1A2440)

// ===== Text tokens =====
// High-contrast text tokens for readability (use AccentGreen only as accent, not primary text)
val TextPrimaryLight = Color(0xFF0F172A)
val TextPrimaryDark = Color(0xFFE6EAF2)

val TextSecondaryLight = Color(0xFF475569)
val TextSecondaryDark = Color(0xFFB8C2D6)

val SoftBlue = Color(0xFFE8F0FF)

val SurfaceVariantDark = Color(0xFF223055)

val SuccessBg = Color(0xFFEAF7EF)
val SuccessText = AccentGreen

// ===== Old names used by older UI code =====
val CardSurface = CardSurfaceLight
val TextPrimary = TextPrimaryLight
val TextSecondary = TextSecondaryLight