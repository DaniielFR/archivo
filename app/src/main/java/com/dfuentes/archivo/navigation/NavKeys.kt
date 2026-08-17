package com.dfuentes.archivo.navigation

import androidx.navigation3.runtime.NavKey
import com.dfuentes.archivo.core.model.MediaType
import kotlinx.serialization.Serializable

/**
 * Claves de navegación. En Navigation 3 la pila es tuya: una lista de estas
 * claves serializables. No hay rutas en forma de String ni parsing de argumentos,
 * y por tanto no hay una clase entera de errores en tiempo de ejecución.
 */
@Serializable
data object LibraryKey : NavKey

@Serializable
data object StatsKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data class DetailKey(val workId: Long) : NavKey

/** [workId] null ⇒ alta nueva; con valor ⇒ edición de una obra existente. */
@Serializable
data class AddEditKey(val type: MediaType, val workId: Long? = null) : NavKey

/** Búsqueda asistida en catálogos externos. Fase 4: también películas y series. */
@Serializable
data class SearchKey(val type: MediaType) : NavKey
