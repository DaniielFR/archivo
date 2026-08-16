package com.dfuentes.archivo.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta de respaldo, usada cuando el color dinámico está desactivado en Ajustes.
 * Semilla: verde tinta #2F4A3E — color de encuadernación, no de app de productividad.
 *
 * Si quieres afinarla, genera el fichero exacto con Material Theme Builder
 * (https://material-foundation.github.io/material-theme-builder/) usando esa semilla
 * y sustituye este archivo. Los nombres de los slots son los mismos.
 */

// ── Tonos derivados de la semilla ────────────────────────────────────────────
private val Green40 = Color(0xFF3A6B55)
private val Green80 = Color(0xFF9FD3B9)
private val Green10 = Color(0xFF00210F)
private val Green30 = Color(0xFF20523E)
private val Green90 = Color(0xFFBBF0D4)

private val Clay40 = Color(0xFF7B5260)   // secondary — acento cálido para cine
private val Clay80 = Color(0xFFECB8C8)
private val Clay10 = Color(0xFF2E111D)
private val Clay30 = Color(0xFF613B49)
private val Clay90 = Color(0xFFFFD9E3)

private val Amber40 = Color(0xFF7C5800)  // tertiary — acento para libros
private val Amber80 = Color(0xFFF8BD32)
private val Amber10 = Color(0xFF271900)
private val Amber30 = Color(0xFF5E4200)
private val Amber90 = Color(0xFFFFDEA6)

private val Neutral10 = Color(0xFF191C1A)
private val Neutral90 = Color(0xFFE1E3DF)
private val Neutral99 = Color(0xFFFBFDF8)

val ArchivoLightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Clay40,
    onSecondary = Color.White,
    secondaryContainer = Clay90,
    onSecondaryContainer = Clay10,
    tertiary = Amber40,
    onTertiary = Color.White,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Color(0xFFDBE5DC),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFBFC9C1),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

val ArchivoDarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Color(0xFF073823),
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Clay80,
    onSecondary = Color(0xFF472632),
    secondaryContainer = Clay30,
    onSecondaryContainer = Clay90,
    tertiary = Amber80,
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C1),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404943),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

