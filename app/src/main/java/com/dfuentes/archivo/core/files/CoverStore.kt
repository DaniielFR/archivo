package com.dfuentes.archivo.core.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import com.dfuentes.archivo.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de guardar una portada: ruta relativa y color dominante. */
data class StoredCover(val relativePath: String, val dominantColor: Int?)

/**
 * Portadas en almacenamiento interno.
 *
 * Regla dura del proyecto (RNF-04): **una obra guardada nunca vuelve a
 * necesitar la red.** Las URLs de Google y Open Library pueden pudrirse, las
 * APIs pueden cambiar de términos o desaparecer; la copia local no. `cover_url`
 * se conserva solo por si algún día quieres volver a descargarla en más calidad.
 *
 * Además resuelve el rendimiento: la rejilla lee del disco, no de la red.
 */
@Singleton
class CoverStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {

    private val dir: File
        get() = File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun fileFor(relativePath: String): File = File(context.filesDir, relativePath)

    fun absolutePathOf(relativePath: String?): String? =
        relativePath?.let { fileFor(it).takeIf(File::exists)?.absolutePath }

    /**
     * Descarga, reescala y guarda. Devuelve null si algo falla: una portada que
     * no baja NO debe impedir que se guarde la obra. El registro es lo
     * importante; la imagen es decoración.
     */
    suspend fun download(url: String, workId: Long): StoredCover? = withContext(io) {
        runCatching {
            val request = Request.Builder().url(url).build()
            val bytes = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                response.body?.bytes() ?: return@runCatching null
            }

            val bitmap = decodeScaled(bytes) ?: return@runCatching null
            val target = File(dir, "$workId.jpg")
            target.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
            val color = runCatching {
                Palette.from(bitmap).clearFilters().generate().getDominantColor(0).takeIf { it != 0 }
            }.getOrNull()
            bitmap.recycle()

            StoredCover("$DIR/${target.name}", color)
        }.getOrNull()
    }

    fun delete(workId: Long) {
        File(dir, "$workId.jpg").delete()
    }

    fun allFiles(): List<File> = dir.listFiles()?.toList().orEmpty()

    /** Usado al restaurar una copia de seguridad que incluye portadas. */
    suspend fun writeRaw(name: String, bytes: ByteArray): String? = withContext(io) {
        runCatching {
            val target = File(dir, name)
            target.writeBytes(bytes)
            "$DIR/${target.name}"
        }.getOrNull()
    }

    /**
     * Decodifica en dos pasadas: primero solo los límites para calcular
     * `inSampleSize`, luego de verdad. Decodificar a tamaño completo una portada
     * de 2000 px para guardarla a 600 es la forma más fácil de provocar un
     * OutOfMemory en un móvil modesto.
     */
    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > MAX_WIDTH * 2) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private companion object {
        const val DIR = "covers"
        const val MAX_WIDTH = 600
        const val QUALITY = 85
    }
}
