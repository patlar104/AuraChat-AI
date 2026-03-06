package com.aurachat.ui.theme

import androidx.compose.ui.graphics.Color

// ── Backgrounds (true black for AMOLED) ──────────────────────────────────────
val AuraBackground        = Color(0xFF000000)   // True black — main background
val AuraSurface           = Color(0xFF0D0D0D)   // Slightly lifted surface (cards, bubbles)
val AuraSurfaceVariant    = Color(0xFF1A1A1A)   // Input bars, drawers, elevated surfaces

// ── Primary accent (Google Blue family) ──────────────────────────────────────
val AuraPrimary           = Color(0xFF8AB4F8)   // Blue — send buttons, links, active states
val AuraOnPrimary         = Color(0xFF000000)   // Text/icons on primary
val AuraPrimaryContainer  = Color(0xFF1A2D4D)   // Container behind primary elements

// ── Secondary accent ─────────────────────────────────────────────────────────
val AuraSecondary         = Color(0xFFAECBFA)   // Lighter blue — secondary actions
val AuraOnSecondary       = Color(0xFF000000)

// ── Tertiary accent (soft purple — Aura brand) ───────────────────────────────
val AuraTertiary          = Color(0xFFD2A8FF)   // Purple — suggestion chips, highlights
val AuraOnTertiary        = Color(0xFF000000)

// ── Text ─────────────────────────────────────────────────────────────────────
val AuraOnBackground      = Color(0xFFE8EAED)   // Primary text
val AuraOnSurface         = Color(0xFFE8EAED)   // Text on surfaces
val AuraOnSurfaceVariant  = Color(0xFF9AA0A6)   // Secondary / hint text

// ── Dividers & outlines ───────────────────────────────────────────────────────
val AuraOutline           = Color(0xFF3C4043)   // Borders
val AuraOutlineVariant    = Color(0xFF2D2D2D)   // Subtle separators

// ── Error ─────────────────────────────────────────────────────────────────────
val AuraError             = Color(0xFFF28B82)   // Soft red
val AuraOnError           = Color(0xFF000000)
