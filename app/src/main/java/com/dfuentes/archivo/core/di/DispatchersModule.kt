package com.dfuentes.archivo.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Los dispatchers se INYECTAN, nunca se referencian directamente.
 * Es lo que permite sustituirlos por un TestDispatcher y que los tests sean
 * deterministas en lugar de depender de esperas.
 */
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class IoDispatcher

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides @IoDispatcher fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides @DefaultDispatcher fun default(): CoroutineDispatcher = Dispatchers.Default
}
