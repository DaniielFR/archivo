package com.dfuentes.archivo.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dfuentes.archivo.core.database.entity.WorkWithRelations
import kotlinx.coroutines.flow.Flow

/**
 * Proyección de lectura para la rejilla: SOLO lo que pinta la tarjeta.
 * Devolver WorkEntity completo aquí cargaría sinopsis, identificadores externos
 * y sinopsis de 2.000 filas que nunca se muestran.
 *
 * `type` y `status` viajan como String y se convierten a enum en el mapper:
 * así los TypeConverters de la base de datos solo tienen que cubrir columnas
 * de entidad, sin casos nullable ambiguos en las proyecciones.
 */
data class WorkCard(
    val id: Long,
    val type: String,
    val title: String,
    val year: Int?,
    @ColumnInfo(name = "cover_path") val coverPath: String?,
    @ColumnInfo(name = "dominant_color") val dominantColor: Int?,
    val creators: String?,
    val status: String?,
    val rating: Int?,
    @ColumnInfo(name = "finished_on") val finishedOn: Long?,
)

@Dao
interface LibraryDao {

    /**
     * Consulta única de biblioteca: los filtros nulos se neutralizan en el WHERE,
     * de modo que una sola consulta cubre todas las combinaciones de filtros.
     *
     * El LEFT JOIN toma el registro VIGENTE de cada obra (mayor `round`), no todos:
     * si no, un libro releído tres veces aparecería tres veces en la rejilla.
     *
     * `finished_on` son días desde epoch, de ahí el `* 86400` para strftime.
     */
    @Query(
        """
        SELECT  w.id                AS id,
                w.type              AS type,
                w.title             AS title,
                w.year              AS year,
                w.cover_path        AS cover_path,
                w.dominant_color    AS dominant_color,
                (SELECT GROUP_CONCAT(p.name, ', ')
                   FROM work_person wp
                   JOIN person p ON p.id = wp.person_id
                  WHERE wp.work_id = w.id
                    AND wp.role IN ('AUTHOR', 'DIRECTOR', 'CREATOR')
                ) AS creators,
                e.status            AS status,
                e.rating            AS rating,
                e.finished_on       AS finished_on
        FROM    work w
        LEFT JOIN entry e ON e.id = (
                    SELECT id FROM entry
                     WHERE work_id = w.id
                     ORDER BY round DESC, id DESC
                     LIMIT 1
                )
        WHERE  (:type   IS NULL OR w.type   = :type)
          AND  (:status IS NULL OR e.status = :status)
          AND  (:year   IS NULL OR CAST(strftime('%Y', e.finished_on * 86400, 'unixepoch') AS INTEGER) = :year)
        ORDER BY
            CASE WHEN :sort = 'RECENT' THEN e.finished_on END DESC,
            CASE WHEN :sort = 'RATING' THEN e.rating      END DESC,
            CASE WHEN :sort = 'TITLE'  THEN w.sort_title  END ASC,
            CASE WHEN :sort = 'ADDED'  THEN w.created_at  END DESC,
            w.updated_at DESC
        """,
    )
    fun library(
        type: String?,
        status: String?,
        year: Int?,
        sort: String,
    ): Flow<List<WorkCard>>

    /** Carrusel "En curso" de la parte superior de la biblioteca. */
    @Query(
        """
        SELECT  w.id AS id, w.type AS type, w.title AS title, w.year AS year,
                w.cover_path AS cover_path, w.dominant_color AS dominant_color,
                NULL AS creators, e.status AS status, e.rating AS rating,
                e.finished_on AS finished_on
        FROM    entry e
        JOIN    work w ON w.id = e.work_id
        WHERE   e.status = 'IN_PROGRESS'
        ORDER BY e.updated_at DESC
        """,
    )
    fun inProgress(): Flow<List<WorkCard>>

    /**
     * Búsqueda provisional por LIKE sobre la clave de ordenación (que ya viene
     * sin tildes). En la fase 5 se sustituye por FTS4 con remove_diacritics=2,
     * que además cubrirá el contenido de las notas.
     */
    @Query(
        """
        SELECT  w.id AS id, w.type AS type, w.title AS title, w.year AS year,
                w.cover_path AS cover_path, w.dominant_color AS dominant_color,
                NULL AS creators, MAX(e.status) AS status, MAX(e.rating) AS rating,
                MAX(e.finished_on) AS finished_on
        FROM    work w
        LEFT JOIN entry e ON e.work_id = w.id
        WHERE   w.sort_title LIKE '%' || :query || '%'
        GROUP BY w.id
        LIMIT   60
        """,
    )
    fun search(query: String): Flow<List<WorkCard>>

    @Transaction
    @Query("SELECT * FROM work WHERE id = :id")
    fun workDetail(id: Long): Flow<WorkWithRelations?>

    @Query("SELECT COUNT(*) FROM work")
    fun workCount(): Flow<Int>
}
