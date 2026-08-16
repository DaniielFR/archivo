// Plugins declarados aquí (sin aplicar) para fijar su versión en todo el proyecto.
plugins {
    alias(libs.plugins.android.application) apply false
    // kotlin.android ya no se aplica: AGP 9 trae Kotlin integrado (built-in Kotlin).
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.ksp)                 apply false
    alias(libs.plugins.hilt)                apply false
    alias(libs.plugins.room)                apply false
}
