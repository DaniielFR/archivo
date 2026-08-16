package com.dfuentes.archivo.core.database.di

import android.content.Context
import androidx.room.Room
import com.dfuentes.archivo.core.database.ArchivoDatabase
import com.dfuentes.archivo.core.database.dao.EntryDao
import com.dfuentes.archivo.core.database.dao.LibraryDao
import com.dfuentes.archivo.core.database.dao.WorkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArchivoDatabase =
        Room.databaseBuilder(context, ArchivoDatabase::class.java, ArchivoDatabase.NAME)
            // Deliberadamente SIN fallbackToDestructiveMigration():
            // preferimos que la app falle al arrancar a que borre tus datos en silencio.
            .build()

    @Provides fun provideLibraryDao(db: ArchivoDatabase): LibraryDao = db.libraryDao()

    @Provides fun provideWorkDao(db: ArchivoDatabase): WorkDao = db.workDao()

    @Provides fun provideEntryDao(db: ArchivoDatabase): EntryDao = db.entryDao()
}
