package com.dfuentes.archivo.core.network.books

import com.dfuentes.archivo.core.network.dto.GoogleBooksResponse
import com.dfuentes.archivo.core.network.dto.OpenLibrarySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApi {

    @GET("books/v1/volumes")
    suspend fun search(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("langRestrict") language: String? = "es",
        @Query("country") country: String = "ES",
        @Query("key") key: String? = null,
    ): GoogleBooksResponse

    companion object {
        const val BASE_URL = "https://www.googleapis.com/"
    }
}

interface OpenLibraryApi {

    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("fields") fields: String = FIELDS,
    ): OpenLibrarySearchResponse

    @GET("search.json")
    suspend fun byIsbn(
        @Query("isbn") isbn: String,
        @Query("fields") fields: String = FIELDS,
    ): OpenLibrarySearchResponse

    companion object {
        const val BASE_URL = "https://openlibrary.org/"

        /**
         * Pedir solo los campos que se usan. Sin esto, una búsqueda devuelve
         * cientos de KB de JSON por resultado — y Open Library pide
         * explícitamente que no se la use como backend de alto tráfico.
         */
        const val FIELDS =
            "key,title,author_name,first_publish_year,number_of_pages_median,cover_i,publisher,isbn,subject"
    }
}
