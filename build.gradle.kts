// Plugins declarados aquí (sin aplicar) para fijar su versión en todo el proyecto.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.ksp)                 apply false
    alias(libs.plugins.hilt)                apply false
    alias(libs.plugins.room)                apply false
}
