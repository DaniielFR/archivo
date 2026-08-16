package com.dfuentes.archivo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dfuentes.archivo.core.designsystem.theme.ArchivoTheme
import com.dfuentes.archivo.navigation.ArchivoApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Obligatorio en targetSdk 36: la app dibuja bajo las barras del sistema
        // y son los composables los que aplican los insets.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ArchivoTheme {
                ArchivoApp()
            }
        }
    }
}
