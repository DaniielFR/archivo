package com.dfuentes.archivo.core.network

import android.content.Context
import com.dfuentes.archivo.BuildConfig
import com.dfuentes.archivo.core.network.books.GoogleBooksApi
import com.dfuentes.archivo.core.network.books.OpenLibraryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class GoogleBooksRetrofit

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class OpenLibraryRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CACHE_BYTES = 20L * 1024 * 1024

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // La API cambia sin avisar y añade campos: ignorarlos es obligatorio.
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            // Caché en disco: repetir la misma búsqueda mientras escribes no debe
            // gastar cuota. Open Library además limita a 1 req/s si no te identificas.
            .cache(Cache(context.cacheDir.resolve("http"), CACHE_BYTES))
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        // Identificarse sube el límite de Open Library de 1 a 3 req/s
                        // y es lo que sus términos piden explícitamente.
                        .header("User-Agent", "Archivo/${BuildConfig.VERSION_NAME} (app personal)")
                        .build(),
                )
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                    )
                }
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideGoogleBooksApi(client: OkHttpClient, json: Json): GoogleBooksApi =
        retrofit(GoogleBooksApi.BASE_URL, client, json).create(GoogleBooksApi::class.java)

    @Provides
    @Singleton
    fun provideOpenLibraryApi(client: OkHttpClient, json: Json): OpenLibraryApi =
        retrofit(OpenLibraryApi.BASE_URL, client, json).create(OpenLibraryApi::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
