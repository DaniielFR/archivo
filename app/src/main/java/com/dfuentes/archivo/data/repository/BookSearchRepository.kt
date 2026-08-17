package com.dfuentes.archivo.data.repository

import com.dfuentes.archivo.core.datastore.SettingsRepository
import com.dfuentes.archivo.core.di.IoDispatcher
import com.dfuentes.archivo.core.model.BookCandidate
import com.dfuentes.archivo.core.model.MetadataSource
import com.dfuentes.archivo.core.network.books.GoogleBooksApi
import com.dfuentes.archivo.core.network.books.OpenLibraryApi
import com.dfuentes.archivo.core.network.dto.GoogleBookItem
import com.dfuentes.archivo.core.network.dto.OpenLibraryDoc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SearchOutcome {
    data class Success(val results: List<BookCandidate>) : SearchOutcome

    data object Offline : SearchOutcome

    data class Error(val message: String) : SearchOutcome
}

/**
 * Búsqueda de libros con dos fuentes.
 *
 * Ninguna basta por sí sola para un lector en español: Google Books cubre mucho
 * mejor el catálogo en castellano, pero se deja páginas, editorial y a veces la
 * portada. Open Library rellena esos huecos. Se consultan **en paralelo** y se
 * fusiona por ISBN, porque encadenarlas duplicaría la espera.
 */
@Singleton
class BookSearchRepository @Inject constructor(
    private val googleBooks: GoogleBooksApi,
    private val openLibrary: OpenLibraryApi,
    private val settings: SettingsRepository,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {

    suspend fun search(query: String): SearchOutcome = withContext(io) {
        if (query.isBlank()) return@withContext SearchOutcome.Success(emptyList())

        val apiKey = settings.preferences.first().googleBooksKey?.takeIf { it.isNotBlank() }

        runCatching {
            coroutineScope {
                val googleDeferred = async {
                    runCatching { googleBooks.search(query = query, key = apiKey).items }
                        .getOrDefault(emptyList())
                }
                val openLibraryDeferred = async {
                    runCatching { openLibrary.search(query = query).docs }
                        .getOrDefault(emptyList())
                }
                merge(googleDeferred.await(), openLibraryDeferred.await())
            }
        }.fold(
            onSuccess = { SearchOutcome.Success(it) },
            onFailure = { throwable ->
                if (throwable is java.io.IOException) {
                    SearchOutcome.Offline
                } else {
                    SearchOutcome.Error(throwable.message ?: "Error de búsqueda")
                }
            },
        )
    }

    /** Completa una ficha concreta con lo que Google no trajo. */
    suspend fun enrich(candidate: BookCandidate): BookCandidate = withContext(io) {
        val isbn = candidate.isbn13 ?: return@withContext candidate
        if (candidate.coverUrl != null && candidate.pageCount != null) return@withContext candidate

        val doc = runCatching { openLibrary.byIsbn(isbn).docs.firstOrNull() }.getOrNull()
            ?: return@withContext candidate
        candidate.copy(
            coverUrl = candidate.coverUrl ?: doc.coverUrl,
            pageCount = candidate.pageCount ?: doc.pageCount,
            publisher = candidate.publisher ?: doc.publisher.firstOrNull(),
        )
    }

    /**
     * Google manda en el orden (mejor relevancia en español) y Open Library solo
     * rellena huecos de los que ya están o añade lo que Google no encontró.
     */
    private fun merge(
        googleItems: List<GoogleBookItem>,
        openLibraryDocs: List<OpenLibraryDoc>,
    ): List<BookCandidate> {
        val fromGoogle = googleItems.mapNotNull { it.toCandidate() }
        val byIsbn = openLibraryDocs.associateBy { doc -> doc.isbn.firstOrNull { it.length == 13 } }

        val completed = fromGoogle.map { candidate ->
            val doc = candidate.isbn13?.let(byIsbn::get)
            if (doc == null) {
                candidate
            } else {
                candidate.copy(
                    coverUrl = candidate.coverUrl ?: doc.coverUrl,
                    pageCount = candidate.pageCount ?: doc.pageCount,
                    publisher = candidate.publisher ?: doc.publisher.firstOrNull(),
                )
            }
        }

        val knownTitles = completed.map { it.title.lowercase() }.toSet()
        val extras = openLibraryDocs
            .filter { it.title?.lowercase() !in knownTitles }
            .mapNotNull { it.toCandidate() }

        return (completed + extras).distinctBy { it.source to it.sourceId }.take(MAX_RESULTS)
    }

    private fun GoogleBookItem.toCandidate(): BookCandidate? {
        val info = volumeInfo
        val title = info.title ?: return null
        return BookCandidate(
            sourceId = id,
            source = MetadataSource.GOOGLE_BOOKS,
            title = title,
            subtitle = info.subtitle,
            authors = info.authors,
            year = info.year,
            publisher = info.publisher,
            pageCount = info.pageCount,
            description = info.description,
            categories = info.categories,
            language = info.language,
            isbn13 = info.isbn13,
            isbn10 = info.isbn10,
            coverUrl = info.imageLinks?.best,
        )
    }

    private fun OpenLibraryDoc.toCandidate(): BookCandidate? {
        val title = title ?: return null
        val key = key ?: return null
        return BookCandidate(
            sourceId = key,
            source = MetadataSource.OPENLIBRARY,
            title = title,
            authors = authorName,
            year = firstPublishYear,
            publisher = publisher.firstOrNull(),
            pageCount = pageCount,
            categories = subject.take(4),
            isbn13 = isbn.firstOrNull { it.length == 13 },
            coverUrl = coverUrl,
        )
    }

    private companion object {
        const val MAX_RESULTS = 25
    }
}
