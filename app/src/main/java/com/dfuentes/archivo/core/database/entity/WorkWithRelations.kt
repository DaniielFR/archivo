package com.dfuentes.archivo.core.database.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** Ficha completa en una sola consulta (@Transaction en el DAO). */
data class WorkWithRelations(
    @Embedded val work: WorkEntity,

    @Relation(parentColumn = "id", entityColumn = "work_id")
    val entries: List<EntryEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorkPersonCrossRef::class,
            parentColumn = "work_id",
            entityColumn = "person_id",
        ),
    )
    val people: List<PersonEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorkTagCrossRef::class,
            parentColumn = "work_id",
            entityColumn = "tag_id",
        ),
    )
    val tags: List<TagEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorkGenreCrossRef::class,
            parentColumn = "work_id",
            entityColumn = "genre_id",
        ),
    )
    val genres: List<GenreEntity>,
)
