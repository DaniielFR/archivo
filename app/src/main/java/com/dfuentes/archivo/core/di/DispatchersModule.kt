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

    // El nombre del método no afecta al binding (Hilt liga por tipo + qualifier).
    // Se evita `default` porque es palabra reservada de Java y rompe el código
    // que genera Dagger vía KSP.
    @Provides @DefaultDispatcher fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
