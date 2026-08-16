package com.dfuentes.archivo.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Preferencia de tema del usuario. Se persistirá en DataStore (fase 1). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun ArchivoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // minSdk 31 garantiza color dinámico; la comprobación de versión se mantiene
    // por claridad y por si algún día bajamos el minSdk.
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ArchivoDarkColors
        else -> ArchivoLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ArchivoTypography,
        shapes = ArchivoShapes,
        content = content,
    )
}
