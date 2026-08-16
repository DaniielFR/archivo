package com.dfuentes.archivo.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Serif para títulos de obra, sans para la interfaz: es el vocabulario visual
 * del mundo editorial y es lo que hace que la app "sepa de libros" sin adornos.
 *
 * CÓMO ACTIVAR LAS FUENTES REALES (5 minutos, merece mucho la pena):
 *  1. Descarga Fraunces y Inter de Google Fonts (ficheros .ttf estáticos,
 *     no variables — Compose los trata mejor).
 *  2. Cópialos a app/src/main/res/font/ con nombres en minúscula y guion bajo:
 *       fraunces_regular.ttf, fraunces_semibold.ttf
 *       inter_regular.ttf, inter_medium.ttf, inter_semibold.ttf
 *  3. Descomenta los dos FontFamily de abajo y sustituye Default por ellos.
 *
 * No uses el proveedor de fuentes descargables: añade latencia en el primer frame.
 */

// private val Fraunces = FontFamily(
//     Font(R.font.fraunces_regular,  FontWeight.Normal),
//     Font(R.font.fraunces_semibold, FontWeight.SemiBold),
// )
// private val Inter = FontFamily(
//     Font(R.font.inter_regular,  FontWeight.Normal),
//     Font(R.font.inter_medium,   FontWeight.Medium),
//     Font(R.font.inter_semibold, FontWeight.SemiBold),
// )

private val Display: FontFamily = FontFamily.Serif   // → Fraunces
private val Body: FontFamily = FontFamily.Default    // → Inter

val ArchivoTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp, lineHeight = 42.sp, letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 20.sp, lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)
