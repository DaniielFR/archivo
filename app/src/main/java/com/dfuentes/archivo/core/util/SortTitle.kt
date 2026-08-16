package com.dfuentes.archivo.core.util

import java.text.Normalizer
import java.util.Locale

private val LEADING_ARTICLES = setOf(
    "el", "la", "los", "las", "un", "una", "unos", "unas",
    "the", "a", "an", "le", "les", "il", "o", "os", "as",
)

/**
 * Clave de ordenación alfabética: sin artículo inicial, sin tildes, en minúsculas.
 * "El nombre del viento" → "nombre del viento".
 *
 * Se calcula al escribir, no al leer: ordenar 2.000 filas en SQL con una columna
 * indexada es gratis; hacerlo en Kotlin en cada recomposición no.
 */
fun sortTitleOf(title: String): String {
    val trimmed = title.trim().lowercase(Locale.forLanguageTag("es-ES"))
    val withoutArticle = trimmed.substringAfter(' ', missingDelimiterValue = "")
        .takeIf { trimmed.substringBefore(' ') in LEADING_ARTICLES && it.isNotBlank() }
        ?: trimmed
    return Normalizer.normalize(withoutArticle, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
}
