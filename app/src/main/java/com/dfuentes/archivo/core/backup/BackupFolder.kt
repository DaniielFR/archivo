package com.dfuentes.archivo.core.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Escritura en la carpeta que el usuario concedió una sola vez (árbol SAF).
 *
 * Se usa DocumentsContract directamente en lugar de androidx.documentfile para
 * no añadir otra dependencia por tres llamadas. A cambio hay que manejar los
 * cursores a mano, que es lo que hace este fichero.
 */
object BackupFolder {

    private const val MIME = "application/zip"

    fun createFile(resolver: ContentResolver, treeUri: Uri, name: String): Uri? {
        val dir = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        return runCatching { DocumentsContract.createDocument(resolver, dir, MIME, name) }.getOrNull()
    }

    /** Nombre + uri de las copias que hay en la carpeta, más recientes primero. */
    fun listBackups(resolver: ContentResolver, treeUri: Uri): List<Pair<String, Uri>> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        return runCatching {
            resolver.query(children, projection, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(0)
                        val name = cursor.getString(1)
                        if (name.endsWith(".$BACKUP_EXTENSION")) {
                            add(name to DocumentsContract.buildDocumentUriUsingTree(treeUri, id))
                        }
                    }
                }
            }.orEmpty()
                // Los nombres son archivo-AAAA-MM-DD, así que el orden
                // lexicográfico inverso es el orden cronológico inverso.
                .sortedByDescending { it.first }
        }.getOrDefault(emptyList())
    }

    /**
     * Rotación: deja las [keep] más recientes. Sin esto, una copia semanal llena
     * la carpeta de Drive de ficheros en un año y medio.
     */
    fun rotate(resolver: ContentResolver, treeUri: Uri, keep: Int = 5) {
        listBackups(resolver, treeUri).drop(keep).forEach { (_, uri) ->
            runCatching { DocumentsContract.deleteDocument(resolver, uri) }
        }
    }
}
