package com.dfuentes.archivo.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Andamiaje de migraciones. Hoy solo hay versión 1, así que este test únicamente
 * comprueba que el esquema exportado existe y abre.
 *
 * CUANDO AÑADAS LA VERSIÓN 2, el test nuevo son ocho líneas:
 *
 *     @Test fun migrate1To2() {
 *         helper.createDatabase(DB, 1).apply {
 *             execSQL("INSERT INTO work (...) VALUES (...)")
 *             close()
 *         }
 *         val db = helper.runMigrationsAndValidate(DB, 2, true)
 *         // afirmar que la fila insertada sigue ahí y con los datos correctos
 *     }
 *
 * Escríbelo SIEMPRE en el mismo commit que el cambio de esquema. Una migración
 * sin test es una pérdida de datos esperando a que la instales.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ArchivoDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun elEsquemaV1AbreYSeValida() {
        helper.createDatabase(DB, 1).close()
        helper.runMigrationsAndValidate(DB, 1, true)
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
