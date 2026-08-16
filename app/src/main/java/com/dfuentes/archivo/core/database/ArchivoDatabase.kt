package com.dfuentes.archivo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dfuentes.archivo.core.database.dao.BackupDao
import com.dfuentes.archivo.core.database.dao.EntryDao
import com.dfuentes.archivo.core.database.dao.LibraryDao
import com.dfuentes.archivo.core.database.dao.WorkDao
import com.dfuentes.archivo.core.database.entity.EntryEntity
import com.dfuentes.archivo.core.database.entity.GenreEntity
import com.dfuentes.archivo.core.database.entity.PersonEntity
import com.dfuentes.archivo.core.database.entity.TagEntity
import com.dfuentes.archivo.core.database.entity.WorkEntity
import com.dfuentes.archivo.core.database.entity.WorkGenreCrossRef
import com.dfuentes.archivo.core.database.entity.WorkPersonCrossRef
import com.dfuentes.archivo.core.database.entity.WorkTagCrossRef

/**
 * Base de datos = única fuente de verdad de la app.
 *
 * REGLAS DE MIGRACIÓN (ver informe §4.5):
 *  1. Cada cambio de esquema sube `version` en 1.
 *  2. Se añade una @AutoMigration; solo si hay renombrados o cambios de tipo
 *     se escribe una Migration manual.
 *  3. NUNCA se usa fallbackToDestructiveMigration() fuera de debug.
 *  4. Cada versión nueva lleva su test con MigrationTestHelper.
 *
 * El JSON del esquema se exporta a app/schemas/ y SE VERSIONA EN GIT.
 * Sin ese fichero no hay migración automática posible.
 */
@Database(
    entities = [
        WorkEntity::class,
        EntryEntity::class,
        PersonEntity::class,
        WorkPersonCrossRef::class,
        TagEntity::class,
        WorkTagCrossRef::class,
        GenreEntity::class,
        WorkGenreCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
    // autoMigrations = [AutoMigration(from = 1, to = 2)],  ← se añade en el futuro
)
@TypeConverters(Converters::class)
abstract class ArchivoDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun workDao(): WorkDao
    abstract fun entryDao(): EntryDao

    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "archivo.db"
    }
}
