package com.dfuentes.archivo

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.OkHttpClient
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ArchivoApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var okHttpClient: dagger.Lazy<OkHttpClient>

    /**
     * Coil reutiliza el OkHttpClient de la app: misma caché de disco, mismos
     * timeouts y mismo User-Agent. Tener dos pools de conexiones en una app que
     * hace cuatro peticiones sería desperdicio puro.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() }))
            }
            .crossfade(true)
            .build()

    /**
     * WorkManager se inicializa por Hilt, no por su inicializador automático
     * (ver el `provider` con tools:node="remove" del manifiesto). Sin esto,
     * AutoBackupWorker no podría recibir sus dependencias inyectadas.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) enableStrictMode()
    }

    /**
     * StrictMode solo en debug: avisa de lecturas de disco y de red en el hilo
     * principal. En una app cuya UI se alimenta de Room, es la red de seguridad
     * que detecta un `runBlocking` mal puesto antes de que se note en el scroll.
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build(),
        )
    }
}
