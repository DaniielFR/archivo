package com.dfuentes.archivo.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Etiquetas del usuario y géneros de la API viven en tablas separadas a propósito:
 * los géneros se re-sincronizan cuando refrescas metadatos, tus etiquetas nunca
 * se tocan. Mezclarlas significa perder tus etiquetas en el primer refresco.
 */
@Entity(tableName = "tag", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int? = null,
)

@Entity(
    tableName = "work_tag",
    primaryKeys = ["work_id", "tag_id"],
    foreignKeys = [
        ForeignKey(WorkEntity::class, ["id"], ["work_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TagEntity::class, ["id"], ["tag_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tag_id"), Index("work_id")],
)
data class WorkTagCrossRef(
    @ColumnInfo(name = "work_id") val workId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

@Entity(tableName = "genre", indices = [Index(value = ["name"], unique = true)])
data class GenreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "work_genre",
    primaryKeys = ["work_id", "genre_id"],
    foreignKeys = [
        ForeignKey(WorkEntity::class, ["id"], ["work_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GenreEntity::class, ["id"], ["genre_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("genre_id"), Index("work_id")],
)
data class WorkGenreCrossRef(
    @ColumnInfo(name = "work_id") val workId: Long,
    @ColumnInfo(name = "genre_id") val genreId: Long,
)
