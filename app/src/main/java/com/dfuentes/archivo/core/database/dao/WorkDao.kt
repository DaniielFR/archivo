package com.dfuentes.archivo.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.dfuentes.archivo.core.database.entity.GenreEntity
import com.dfuentes.archivo.core.database.entity.PersonEntity
import com.dfuentes.archivo.core.database.entity.TagEntity
import com.dfuentes.archivo.core.database.entity.WorkEntity
import com.dfuentes.archivo.core.database.entity.WorkGenreCrossRef
import com.dfuentes.archivo.core.database.entity.WorkPersonCrossRef
import com.dfuentes.archivo.core.database.entity.WorkTagCrossRef
import com.dfuentes.archivo.core.model.MetadataSource

@Dao
interface WorkDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(work: WorkEntity): Long

    @Update
    suspend fun update(work: WorkEntity)

    @Delete
    suspend fun delete(work: WorkEntity)

    @Query("SELECT * FROM work WHERE id = :id")
    suspend fun findById(id: Long): WorkEntity?

    /** Deduplicación al añadir desde una API o al importar un backup. */
    @Query("SELECT * FROM work WHERE source = :source AND source_id = :sourceId LIMIT 1")
    suspend fun findBySource(source: MetadataSource, sourceId: String): WorkEntity?

    @Query("SELECT * FROM work WHERE isbn13 = :isbn13 LIMIT 1")
    suspend fun findByIsbn(isbn13: String): WorkEntity?

    // ── Personas ──
    @Query("SELECT * FROM person WHERE name = :name LIMIT 1")
    suspend fun findPerson(name: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Upsert
    suspend fun linkPerson(ref: WorkPersonCrossRef)

    @Query("DELETE FROM work_person WHERE work_id = :workId")
    suspend fun clearPeople(workId: Long)

    // ── Etiquetas ──
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    suspend fun findTag(name: String): TagEntity?

    @Query("SELECT * FROM tag ORDER BY name")
    suspend fun allTags(): List<TagEntity>

    @Upsert
    suspend fun linkTag(ref: WorkTagCrossRef)

    @Query("DELETE FROM work_tag WHERE work_id = :workId AND tag_id = :tagId")
    suspend fun unlinkTag(workId: Long, tagId: Long)

    // ── Géneros ──
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenre(genre: GenreEntity): Long

    @Query("SELECT * FROM genre WHERE name = :name LIMIT 1")
    suspend fun findGenre(name: String): GenreEntity?

    @Upsert
    suspend fun linkGenre(ref: WorkGenreCrossRef)

    @Query("DELETE FROM work_genre WHERE work_id = :workId")
    suspend fun clearGenres(workId: Long)
}
