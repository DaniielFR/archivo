# Archivo — contexto del proyecto

App personal de Android para archivar lecturas, películas y series.
100 % local, sin cuentas ni servidores. Un solo usuario: Daniel.

El diseño completo y el plan por fases están en `informe-tecnico.md`.
Los ajustes de versiones hechos al montar el proyecto, en `CAMBIOS-SETUP.md`.

## Estado

Fases 0 y 1 completadas. **Siguiente: fase 2 — copias de seguridad.**

⚠️ No metas datos que te dolería perder hasta que la fase 2 esté hecha.

## Entorno

- JDK 21 (Temurin) en `~/opt/jdk-21.0.12+8`; variables en `~/.archivo-env.sh`
- Android SDK en `~/Android/Sdk` (command-line tools, sin Android Studio)
- Gradle 9.7.0 vía wrapper
- Se prueba en móvil físico por ADB, no en emulador

```bash
source ~/.archivo-env.sh
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
```

## Stack

Kotlin 2.3.20 · Compose (BOM 2026.08.00) · Room 2.8.4 · Hilt 2.60.1 · KSP 2.3.11
Navigation 3 1.1.6 · DataStore
`compileSdk 37` · `targetSdk 36` · `minSdk 31`

Trampas de versiones ya resueltas, **no las reintroduzcas**:

- **No apliques `org.jetbrains.kotlin.android`**: AGP 9 trae Kotlin integrado y
  aplicarlo es un error de build.
- **KSP no comparte versión con Kotlin** desde la línea 2.3 (versionado propio).
- **Hilt < 2.60 no funciona con AGP 9.**
- **`compileSdk` 37 lo imponen las dependencias del Compose BOM**, no Play.
- **Room 3.0 (`androidx.room3`) está en alpha**: quédate en 2.8.x.

## Arquitectura

Tres capas con flujo de datos unidireccional. **La base de datos es la única
fuente de verdad**: a partir de la fase 3, la red escribirá en Room y la UI
observará Room, nunca al revés.

```
feature/   pantallas: Route (con estado) + Screen (sin estado, previsualizable)
data/      repositorios y mappers
core/      model · database · designsystem · di · util
```

### Modelo de datos — la decisión que no se toca

`work` (la obra: título, autoría, año — objetiva, enriquecible desde APIs) está
separada de `entry` (tu registro: estado, nota, fechas, texto libre). Relación
1:N. Eso es lo que permite releer algo sin machacar lo que pensaste la primera
vez, y que las estadísticas anuales no cuenten dos veces.

Los tres tipos comparten tabla con un discriminador `MediaType`. Añadir un tipo
nuevo es una constante más en el enum.

## Reglas

1. Los composables no conocen Room, Retrofit ni `Context`.
2. Cada capa tiene su modelo; los mappers viven en la capa que consume.
3. Los dispatchers se inyectan, nunca se referencian directamente.
4. `fallbackToDestructiveMigration()` prohibido fuera de debug.
5. El esquema de Room en `app/schemas/` se versiona en git. Siempre.
6. Cada cambio de esquema: sube `version`, añade `@AutoMigration` y **su test**
   con `MigrationTestHelper`.
7. Toda escritura destructiva lleva `Snackbar` con acción *Deshacer*.
8. Las proyecciones de DAO devuelven `String` para los enums; la conversión
   ocurre en `Mappers.kt`. No metas TypeConverters nullable.
9. Los nombres de métodos `@Provides` no pueden ser palabras reservadas de Java
   (`default`, `class`, `new`…): rompen el código que genera Dagger.

## Patrón de pantalla

Toda feature nueva sigue esta plantilla, sin excepciones:

```
XxxUiState.kt     data class inmutable + sealed interface XxxAction
XxxViewModel.kt   @HiltViewModel, expone un único StateFlow<XxxUiState>
                  con stateIn(WhileSubscribed(5_000))
XxxScreen.kt      XxxRoute (conoce el ViewModel) + XxxScreen (no lo conoce)
                  + un @Preview por estado
```

## Qué NO hacer

- No añadir dependencias sin preguntar.
- No cambiar el esquema de la BD, las capas ni la estructura de paquetes.
- No bajar el `minSdk` ni subir a Room 3.0.
- No usar `fallbackToDestructiveMigration()` para esquivar un error de Room.
- No meter analytics, crash reporting remoto ni nada que envíe datos fuera.

## Navegación

Navigation 3: la pila es una `SnapshotStateList<NavKey>` que poseemos nosotros
(`navigation/NavKeys.kt`, `navigation/ArchivoApp.kt`). Navegar es `backStack.add`.
Los ViewModels con argumento usan **inyección asistida**:
`@HiltViewModel(assistedFactory = …)` + `hiltViewModel(creationCallback = …)`.
Es obligatorio incluir `rememberViewModelStoreNavEntryDecorator()` en
`entryDecorators` o cada recomposición crearía un ViewModel nuevo.

## Deuda conocida (fase 2+)

- Warnings pendientes: `!!` innecesarios en `RatingStars.kt`, icono `StarHalf`
  deprecado, target de anotación en `LibraryRepositoryImpl`.
- La búsqueda es `LIKE` provisional; FTS4 llega en la fase 5.
- `LibraryLayout.LIST` se persiste pero la rejilla aún no lo respeta.
- Las notas se guardan en cada pulsación: falta un debounce (~400 ms).
- No hay pantalla de progreso (página / SxxEyy) todavía.
