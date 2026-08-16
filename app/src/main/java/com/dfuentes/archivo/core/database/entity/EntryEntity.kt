package com.dfuentes.archivo.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dfuentes.archivo.core.model.Format
import com.dfuentes.archivo.core.model.Status

@Entity(
    tableName = "entry",
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["work_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("work_id"), Index("status"), Index("finished_on")],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "work_id") val workId: Long,

    val status: Status,
    /** 0..10 en medias estrellas. null = sin puntuar (distinto de 0 = malísimo). */
    val rating: Int? = null,

    /** Días desde epoch (LocalDate.toEpochDay), no milisegundos. */
    @ColumnInfo(name = "started_on") val startedOn: Long? = null,
    @ColumnInfo(name = "finished_on") val finishedOn: Long? = null,

    val notes: String? = null,
    val format: Format? = null,
    @ColumnInfo(name = "is_favourite") val isFavourite: Boolean = false,

    /** Página actual (BOOK) o episodios vistos (SERIES). */
    @ColumnInfo(name = "progress_value") val progressValue: Int? = null,
    @ColumnInfo(name = "progress_season") val progressSeason: Int? = null,

    /** 1 = primera lectura/visionado, 2 = relectura… */
    val round: Int = 1,

    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
