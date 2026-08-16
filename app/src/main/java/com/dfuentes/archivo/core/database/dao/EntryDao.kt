package com.dfuentes.archivo.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dfuentes.archivo.core.database.entity.EntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Delete
    suspend fun delete(entry: EntryEntity)

    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun findById(id: Long): EntryEntity?

    @Query("SELECT * FROM entry WHERE work_id = :workId ORDER BY round DESC")
    fun forWork(workId: Long): Flow<List<EntryEntity>>

    @Query("SELECT COALESCE(MAX(round), 0) FROM entry WHERE work_id = :workId")
    suspend fun lastRound(workId: Long): Int

    // ── Estadísticas (fase 5) ──
    @Query(
        """
        SELECT COUNT(*) FROM entry
        WHERE status = 'FINISHED'
          AND CAST(strftime('%Y', finished_on * 86400, 'unixepoch') AS INTEGER) = :year
        """,
    )
    fun finishedCountInYear(year: Int): Flow<Int>

    @Query(
        """
        SELECT AVG(rating) FROM entry
        WHERE rating IS NOT NULL
          AND CAST(strftime('%Y', finished_on * 86400, 'unixepoch') AS INTEGER) = :year
        """,
    )
    fun averageRatingInYear(year: Int): Flow<Double?>
}
