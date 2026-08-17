package com.dfuentes.archivo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dfuentes.archivo.core.database.entity.EntryEntity
import com.dfuentes.archivo.core.database.entity.GenreEntity
import com.dfuentes.archivo.core.database.entity.PersonEntity
import com.dfuentes.archivo.core.database.entity.TagEntity
import com.dfuentes.archivo.core.database.entity.WorkEntity
import com.dfuentes.archivo.core.database.entity.WorkGenreCrossRef
import com.dfuentes.archivo.core.database.entity.WorkPersonCrossRef
import com.dfuentes.archivo.core.database.entity.WorkTagCrossRef

/** Lecturas y escrituras masivas. Solo las usa la copia de seguridad. */
@Dao
interface BackupDao {

    @Query("SELECT * FROM work ORDER BY id")
    suspend fun allWorks(): List<WorkEntity>

    @Query("SELECT * FROM entry ORDER BY work_id, round")
    suspend fun allEntries(): List<EntryEntity>

    @Query("SELECT * FROM person") suspend fun allPeople(): List<PersonEntity>

    @Query("SELECT * FROM work_person") suspend fun allWorkPeople(): List<WorkPersonCrossRef>

    @Query("SELECT * FROM tag") suspend fun allTags(): List<TagEntity>

    @Query("SELECT * FROM work_tag") suspend fun allWorkTags(): List<WorkTagCrossRef>

    @Query("SELECT * FROM genre") suspend fun allGenres(): List<GenreEntity>

    @Query("SELECT * FROM work_genre") suspend fun allWorkGenres(): List<WorkGenreCrossRef>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWork(work: WorkEntity): Long

    @Insert suspend fun insertEntries(entries: List<EntryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenre(genre: GenreEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkPeople(refs: List<WorkPersonCrossRef>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkTags(refs: List<WorkTagCrossRef>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkGenres(refs: List<WorkGenreCrossRef>)

    @Query("SELECT * FROM person WHERE name = :name LIMIT 1")
    suspend fun findPerson(name: String): PersonEntity?

    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    suspend fun findTag(name: String): TagEntity?

    @Query("SELECT * FROM genre WHERE name = :name LIMIT 1")
    suspend fun findGenre(name: String): GenreEntity?

    @Query("SELECT COUNT(*) FROM work") suspend fun workCount(): Int

    @Query("SELECT COUNT(*) FROM entry") suspend fun entryCount(): Int

    /**
     * Huellas de las obras ya presentes. Se usan para deduplicar al importar:
     * ver [com.dfuentes.archivo.core.backup.fingerprintOf].
     */
    @Query("SELECT source || '|' || COALESCE(source_id, '') || '|' || COALESCE(isbn13, '') || '|' || type || '|' || title || '|' || COALESCE(year, 0) FROM work")
    suspend fun fingerprints(): List<String>

    @Transaction
    @Query("DELETE FROM work")
    suspend fun deleteAllWorks()

    @Query("DELETE FROM person") suspend fun deleteAllPeople()

    @Query("DELETE FROM tag") suspend fun deleteAllTags()

    @Query("DELETE FROM genre") suspend fun deleteAllGenres()
}
