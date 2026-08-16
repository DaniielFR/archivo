package com.dfuentes.archivo.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dfuentes.archivo.core.model.PersonRole

/**
 * Personas normalizadas. El motivo no es purismo: es poder responder
 * "enséñame todo lo de Sanderson" con un índice en vez de un LIKE '%…%'
 * sobre una columna de texto.
 */
@Entity(tableName = "person", indices = [Index(value = ["name"], unique = true)])
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "sort_name") val sortName: String,
)

@Entity(
    tableName = "work_person",
    primaryKeys = ["work_id", "person_id", "role"],
    foreignKeys = [
        ForeignKey(WorkEntity::class, ["id"], ["work_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PersonEntity::class, ["id"], ["person_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("person_id"), Index("work_id")],
)
data class WorkPersonCrossRef(
    @ColumnInfo(name = "work_id") val workId: Long,
    @ColumnInfo(name = "person_id") val personId: Long,
    val role: PersonRole,
    val position: Int = 0,
)
