# Archivo — App personal de lecturas, películas y series

**Informe técnico de diseño e implementación**
Agosto 2026 · Android · Kotlin + Jetpack Compose · 100 % local

---

## 0. Resumen ejecutivo

| Decisión | Elección | Motivo en una línea |
|---|---|---|
| Plataforma | Android nativo, `minSdk 31`, `targetSdk 36` | Único destino → cero coste de abstracción multiplataforma |
| Lenguaje / UI | Kotlin 2.3.x + Jetpack Compose (BOM `2026.08.00`) | Material 3 Expressive de fábrica: se ve bien sin ser diseñador |
| Persistencia | Room 2.8.x sobre SQLite + FTS4 | SSOT local, migraciones versionadas, búsqueda instantánea |
| Arquitectura | 3 capas (UI / Domain / Data), UDF, MVVM+ | Recomendación oficial de Google, testeable de arriba abajo |
| DI | Hilt (KSP) | Verificación en compilación, integración `hiltViewModel()` |
| Red | Retrofit + OkHttp + kotlinx.serialization | Solo para *enriquecer*; la app funciona sin internet |
| Metadatos libros | Google Books (primaria) + Open Library (fallback/portadas) | Google Books cubre mucho mejor el catálogo en español |
| Metadatos cine/TV | TMDB (`language=es-ES`) | Gratis para uso personal, catálogo y pósters excelentes |
| Modelo de datos | `work` (obra) ⟂ `entry` (registro personal) | Permite relecturas, "pendientes" y estadísticas correctas |
| Backup | ZIP (JSON + portadas) vía SAF + auto-backup semanal | El riesgo real de una app local es perder los datos |
| Distribución | APK firmado en GitHub Releases + Obtainium | Actualizaciones automáticas sin Play Store |

**Esfuerzo estimado**: 55–80 h de trabajo efectivo hasta una v1.0 sólida, repartidas en 7 fases entregables por separado. El MVP usable (fase 1) cae en 10–14 h.

---

## 1. Definición del producto

### 1.1 El problema real

No es "gestionar una biblioteca". Es **dejar rastro**. El valor de la app se mide en dos preguntas que hoy no puedes responder:

1. *¿Qué leí/vi en 2023?*
2. *¿Qué me pareció aquello?*

Todo lo que no sirva a esas dos preguntas es peso muerto. Esto tiene una consecuencia de diseño dura: **el coste de registrar algo debe ser menor que el coste de no registrarlo**. Si añadir un libro cuesta más de ~15 segundos, dejarás de hacerlo en tres semanas y la app muere. Este es el criterio que domina todas las decisiones de UX de este documento.

### 1.2 Requisitos funcionales

**Imprescindibles (v1.0)**

- **RF-01** Registrar libro / película / serie con: título, autor-director, portada, año, estado, fecha de fin, nota (0–5 en medias estrellas) y un texto libre de impresiones.
- **RF-02** Alta asistida: buscar por título → un toque → todos los metadatos rellenos. Alta manual siempre disponible como escape.
- **RF-03** Estados: `Pendiente` · `En curso` · `Terminado` · `Abandonado`.
- **RF-04** Vista de biblioteca filtrable por tipo y estado, ordenable por fecha / nota / título / autor.
- **RF-05** Búsqueda instantánea sobre título, autor y **contenido de las notas**.
- **RF-06** Ficha de detalle editable con historial de registros (relecturas/revisionados).
- **RF-07** Exportación e importación completa (backup manual + automático).
- **RF-08** Funcionamiento íntegro sin conexión, incluidas las portadas.

**Deseables (v1.1+)**

- **RF-09** Escaneo de ISBN con la cámara.
- **RF-10** Etiquetas libres del usuario.
- **RF-11** Estadísticas anuales (recuento, páginas, minutos, histograma de notas, géneros).
- **RF-12** Progreso ligero: página actual en libros, `SxxEyy` en series.
- **RF-13** Widget de pantalla de inicio con lo que tienes en curso.
- **RF-14** Recepción de intents `share` (compartir desde el navegador → añadir).
- **RF-15** Listas personalizadas ("Para el verano", "Top 2026").

**Explícitamente fuera de alcance**

- Cuentas de usuario, social, comentarios, seguidores.
- Sincronización con servidor propio (la sustituye el backup en la nube del usuario).
- Seguimiento episodio a episodio con marcado individual — demasiado coste de mantenimiento para el beneficio.
- Lector de ebooks integrado.

### 1.3 Requisitos no funcionales

| Código | Requisito | Verificación |
|---|---|---|
| RNF-01 | Arranque en frío a primer frame útil < 500 ms | Macrobenchmark + Baseline Profile |
| RNF-02 | Scroll de la rejilla a 120 fps sin jank | `JankStats` en debug, perfil de release |
| RNF-03 | Cero pérdida de datos ante desinstalación accidental | Backup automático verificado con test de restauración |
| RNF-04 | La app funciona indefinidamente si TMDB/Google Books desaparecen | Portadas y metadatos copiados a almacenamiento interno |
| RNF-05 | Ningún dato sale del dispositivo salvo por acción explícita | Sin analytics, sin crash reporting remoto |
| RNF-06 | Migraciones de esquema sin pérdida | Esquemas exportados a git + `MigrationTestHelper` |
| RNF-07 | APK release < 12 MB | R8 + shrinkResources |

---

## 2. Elección del stack: justificación

### 2.1 Nativo (Kotlin/Compose) vs Flutter vs React Native

La comparativa habitual gira en torno a "un código, dos plataformas". **Ese eje no aplica aquí**: solo hay Android. Al eliminarlo, el balance queda así:

| Criterio | Kotlin + Compose | Flutter | React Native / Expo |
|---|---|---|---|
| Reutilización iOS | Irrelevante (0 valor) | Alta | Alta |
| Aspecto nativo Android 16 | Material 3 Expressive nativo, *dynamic color* real | Material 3 reimplementado, siempre por detrás | Depende de librerías de terceros |
| Persistencia local | Room (1ª parte, madura) | Drift / Isar (3ª parte) | WatermelonDB / op-sqlite (3ª parte) |
| Escaneo ISBN | ML Kit + CameraX oficial | Plugin comunitario | Plugin comunitario |
| Widget de inicio | Glance (oficial) | Requiere código Kotlin igualmente | Requiere código Kotlin igualmente |
| Tamaño APK | ~6–10 MB | ~18–25 MB | ~25–40 MB |
| Riesgo de dependencias | Bajo (AndroidX) | Medio | Alto (npm) |
| Curva desde Python/C++ | Kotlin es cómodo, tipado fuerte, corrutinas ≈ asyncio | Dart es fácil pero es un ecosistema nuevo entero | JS/TS + toolchain frágil |
| Iteración | Live Edit + `@Preview` | Hot reload (algo mejor) | Fast Refresh |

**Veredicto: Kotlin + Jetpack Compose.**

El argumento decisivo no es el rendimiento, es el **presupuesto de diseño**. Has pedido que "no sea horrible visualmente" pero no eres diseñador y no quieres serlo. Material 3 Expressive te da, gratis y por defecto: paleta derivada del fondo de pantalla del usuario, tipografía escalable coherente, componentes con física de movimiento ya afinada, modo oscuro correcto, estados de foco y accesibilidad. En Flutter o RN reimplementas o aproximas todo eso, y la diferencia entre "aproximado" y "nativo" es exactamente lo que hace que una app parezca amateur.

Un contraargumento honesto: **Openreads**, la mejor app libre de este tipo, está escrita en Flutter y funciona muy bien. Flutter es una elección defendible, sobre todo si algún día quisieras iOS. Pero para un objetivo Android-only con integración profunda (widget, escáner, SAF, share intents), nativo gana sin discusión.

### 2.2 Versiones de referencia (agosto 2026)

```toml
# gradle/libs.versions.toml  — extracto
[versions]
agp                = "9.3.0"          # jul-2026
kotlin             = "2.3.20"
ksp                = "2.3.20-2.0.0"   # debe casar con la versión de Kotlin
composeBom         = "2026.08.00"     # material3 1.4.0 estable
room               = "2.8.4"          # NO uses room3 3.0.x: está en alpha
hilt               = "2.5x"           # última estable de Dagger Hilt
androidxHilt       = "1.4.0"          # jul-2026 (integración WorkManager/Nav)
navigation3        = "1.0.0"          # Nav3, estable desde nov-2025
coil               = "3.3.0"
retrofit           = "3.0.0"
okhttp             = "5.1.0"
serialization      = "1.9.0"
datastore          = "1.2.0"
workmanager        = "2.11.0"
mlkitBarcode       = "17.3.0"
camerax            = "1.5.0"
turbine            = "1.2.0"

[versions.sdk]
compile = "36"
target  = "36"
min     = "31"
```

Notas de versionado que importan:

- **`compileSdk 36` / `targetSdk 36`** (Android 16). Es el requisito de Play Store desde el 31 de agosto de 2026; aunque no publiques en Play, alinearse evita comportamientos legacy.
- **`minSdk 31`** (Android 12). Es el umbral del *dynamic color* de Material You. Bajar a 26 te obliga a mantener una paleta de respaldo y a probar dos aspectos distintos; no compensa para una app personal.
- **Compose 1.12** exigirá `compileSdk 37` y AGP 9 — tenlo presente para la próxima subida.
- **Room 2.8.x** requiere Kotlin 2.0+ y KSP2, y genera Kotlin (no Java) por defecto.
- **Room 3.0 existe pero está en alpha** (marzo 2026). Cambia las coordenadas a `androidx.room3:*`, obliga a que todos los DAO sean `suspend` o devuelvan `Flow`, elimina KAPT y añade objetivos JS/Wasm. Nada de eso te aporta hoy, y sí te expone a APIs inestables. **Quédate en 2.8.x**; si escribes los DAO con `suspend`/`Flow` desde el principio —que es lo que recomienda este documento— la migración futura será casi mecánica.

### 2.3 Inyección de dependencias: Hilt vs Koin

Para un proyecto de un solo desarrollador, Koin es tentador (sin codegen, sin build lento, Kotlin puro). Recomiendo **Hilt** de todos modos:

- El grafo se valida **en tiempo de compilación**. Un error de cableado en Koin explota en runtime, típicamente en el dispositivo, típicamente en una pantalla que no estabas mirando.
- `hiltViewModel()` en Compose y `@HiltWorker` en WorkManager son integraciones de primera parte que no tienes que resolver tú.
- Es lo que asume la documentación oficial y la mayoría de ejemplos, lo que reduce la fricción cuando pidas ayuda o generes código.

Coste real: ~2–4 s extra de build incremental. Aceptable.

---

## 3. Arquitectura

### 3.1 Vista general

Tres capas, flujo de datos unidireccional, base de datos como **única fuente de verdad**. La red nunca alimenta la UI directamente: escribe en Room, y la UI observa Room.

```
┌──────────────────────────────────────────────────────────────┐
│  CAPA UI                                                     │
│                                                              │
│  Composables (stateless)  ←── UiState (immutable)            │
│         │                                                    │
│         └── eventos ──→  ViewModel                           │
│                             │  StateFlow<UiState>            │
└─────────────────────────────┼────────────────────────────────┘
                              │  llama a
┌─────────────────────────────┼────────────────────────────────┐
│  CAPA DOMINIO (opcional, solo donde aporta)                  │
│                                                              │
│  UseCases: GetLibraryUseCase, LogEntryUseCase,               │
│            ComputeYearStatsUseCase, ExportBackupUseCase      │
│  Modelos de dominio puros (sin anotaciones Room ni JSON)     │
└─────────────────────────────┼────────────────────────────────┘
                              │  llama a
┌─────────────────────────────┼────────────────────────────────┐
│  CAPA DATOS                                                  │
│                                                              │
│  LibraryRepository ──┬── LocalDataSource   (Room DAOs) ★SSOT │
│                      ├── RemoteBookSource  (Books/OpenLib)   │
│                      ├── RemoteMediaSource (TMDB)            │
│                      └── CoverStore        (ficheros)        │
│  SettingsRepository ──── DataStore Preferences               │
│  BackupRepository   ──── SAF + zip + JSON                    │
└──────────────────────────────────────────────────────────────┘
```

**Reglas que no se negocian:**

1. Un `Flow` de Room llega a la UI; nunca un callback de red.
2. Los `@Composable` no conocen Room, Retrofit ni `Context`. Reciben `UiState` y emiten lambdas.
3. Cada capa tiene su propio modelo: `WorkEntity` (Room) → `Work` (dominio) → `WorkUi` (presentación). Los mappers viven en la capa que consume.
4. Ninguna suspensión bloquea el hilo principal; `Dispatchers.IO` se inyecta, no se referencia directamente (así los tests pueden sustituirlo).

### 3.2 Modularización: qué hacer y qué no

La tentación es copiar la estructura de *Now in Android* con 15 módulos Gradle. **No lo hagas al principio.** Ese proyecto es un escaparate para equipos grandes; en solitario el coste de configuración de módulos supera con creces el ahorro de build.

**Fase 1–5: un único módulo `:app` con paquetes estrictos.**

```
com.dfuentes.archivo/
├── ArchivoApplication.kt          @HiltAndroidApp
├── MainActivity.kt
├── core/
│   ├── database/
│   │   ├── ArchivoDatabase.kt
│   │   ├── entity/               WorkEntity, EntryEntity, PersonEntity…
│   │   ├── dao/                  WorkDao, EntryDao, StatsDao, SearchDao
│   │   ├── converter/            Converters (Instant, enums)
│   │   └── migration/            Migration_1_2.kt…
│   ├── network/
│   │   ├── books/                GoogleBooksApi, OpenLibraryApi, dto/
│   │   ├── tmdb/                 TmdbApi, dto/
│   │   └── NetworkModule.kt
│   ├── datastore/                SettingsRepository, UserPreferences
│   ├── files/                    CoverStore, BackupFileManager
│   ├── model/                    Work, Entry, MediaType, Status, Rating…
│   └── designsystem/
│       ├── theme/                Color.kt, Type.kt, Shape.kt, Theme.kt
│       └── component/            RatingStars, PosterCard, StatusChip,
│                                 EmptyState, SectionHeader, CoverImage
├── data/
│   ├── repository/               LibraryRepositoryImpl, SearchRepositoryImpl…
│   └── mapper/
├── domain/
│   └── usecase/
└── feature/
    ├── library/                  LibraryScreen, LibraryViewModel, LibraryUiState
    ├── detail/
    ├── addflow/                  SearchScreen, QuickLogSheet, ManualEntryScreen
    ├── scanner/
    ├── stats/
    └── settings/
```

**Cuándo partir en módulos Gradle:** cuando el build incremental supere ~45 s, o cuando quieras tests de pantalla aislados por feature. La ruta natural es `:core:database`, `:core:designsystem`, `:core:network`, y luego `:feature:*`. Que los paquetes ya estén separados hace que el corte sea mecánico.

**Truco barato para mantener la disciplina sin modularizar:** una regla de Konsist o un test de arquitectura que falle si `feature.*` importa `core.database.dao.*`. 20 líneas, te ahorra la erosión.

### 3.3 Patrón de pantalla

Cada feature sigue exactamente esta plantilla. La uniformidad es lo que hace un proyecto "bien construido"; la creatividad va en el producto, no en la fontanería.

```kotlin
// 1) Estado: inmutable, exhaustivo, sin nullables ambiguos
data class LibraryUiState(
    val items: List<WorkCardUi> = emptyList(),
    val filter: LibraryFilter = LibraryFilter(),
    val layout: LibraryLayout = LibraryLayout.Grid,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
)

// 2) Eventos entrantes: un sealed interface, sin lambdas sueltas
sealed interface LibraryAction {
    data class TypeFilterChanged(val type: MediaType?) : LibraryAction
    data class StatusFilterChanged(val status: Status?) : LibraryAction
    data class SortChanged(val sort: SortOrder) : LibraryAction
    data object LayoutToggled : LibraryAction
}

// 3) ViewModel: combina flows de Room + preferencias, expone un solo StateFlow
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibrary: GetLibraryUseCase,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter())

    val uiState: StateFlow<LibraryUiState> =
        combine(filter, settings.layout) { f, layout -> f to layout }
            .flatMapLatest { (f, layout) ->
                getLibrary(f).map { works ->
                    LibraryUiState(
                        items = works.map(Work::toCardUi),
                        filter = f,
                        layout = layout,
                        isLoading = false,
                        isEmpty = works.isEmpty(),
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LibraryUiState(),
            )

    fun onAction(action: LibraryAction) { /* … */ }
}

// 4) Composable de ruta (con estado) + composable de contenido (sin estado, previsualizable)
@Composable
fun LibraryRoute(vm: LibraryViewModel = hiltViewModel(), onOpen: (Long) -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    LibraryScreen(state = state, onAction = vm::onAction, onOpen = onOpen)
}
```

El `LibraryScreen` puro es lo que renderizan los `@Preview` y lo que capturan los tests de captura de pantalla. Esta separación es la que hace que puedas iterar el diseño sin arrancar la app.

### 3.4 Navegación

**Navigation 3** (estable desde noviembre de 2025) sustituye al grafo de rutas con strings por una **pila de estado que tú posees**: una `SnapshotStateList<NavKey>` con claves serializables. Ventajas concretas para este proyecto: las transiciones de elemento compartido (portada de la rejilla → portada de la ficha) dejan de ser un truco, y el estado de navegación se guarda y restaura sin ceremonia.

```kotlin
@Serializable data object LibraryKey : NavKey
@Serializable data class DetailKey(val workId: Long) : NavKey
@Serializable data class AddKey(val type: MediaType) : NavKey
@Serializable data object StatsKey : NavKey
@Serializable data object SettingsKey : NavKey
```

Envoltura raíz: `NavigationSuiteScaffold`, que elige automáticamente `NavigationBar` en móvil y `NavigationRail` en tablet/plegable abierto. Coste: cero. Beneficio: la app no se ve rota en horizontal.

---

## 4. Modelo de datos

Esta es la decisión con mayor coste de reversión. Vale la pena media hora extra de reflexión.

### 4.1 La decisión central: separar *obra* de *registro*

El error frecuente es una sola tabla `libro` con columnas `nota` y `fecha_lectura`. Rompe en cuanto:

- Relees un libro (¿sobrescribes la nota de 2019?).
- Guardas algo como pendiente (¿qué fecha pones?).
- Quieres estadísticas de "libros terminados en 2026" (una fila con dos lecturas cuenta una vez).

Solución: **`work`** describe la cosa (objetiva, comparable, enriquecible desde APIs) y **`entry`** describe tu experiencia con ella (subjetiva, repetible, con fechas). Relación 1:N.

Coste: una unión más en las consultas. Beneficio: el modelo no miente, y las estadísticas salen de una sola consulta agregada.

### 4.2 La segunda decisión: una tabla para los tres tipos

Libros, películas y series comparten ~80 % de sus campos (título, año, portada, sinopsis, autoría, géneros). Las alternativas son:

| Opción | Pros | Contras |
|---|---|---|
| Tres tablas separadas | Sin columnas nulas | Triplica DAOs, la vista "todo mezclado" necesita `UNION`, la búsqueda global es un dolor |
| **Una tabla + discriminador `type` + columnas específicas anulables** | Un DAO, una búsqueda, un feed unificado; añadir "videojuegos" es una línea de enum | Algunas columnas nulas (`page_count` en una película) |
| Tabla base + tablas de extensión (herencia) | Normalizado puro | Complejidad de joins desproporcionada para 3 subtipos |

**Elección: tabla única con discriminador.** Con 3 subtipos y ~4 columnas específicas cada uno, la normalización pura es sobreingeniería. Y la funcionalidad más valiosa de la app —"enséñame todo lo de 2026 ordenado por nota"— se vuelve trivial.

### 4.3 Esquema SQL

```sql
-- ─────────────────────────────────────────────────────────────
-- OBRA: la cosa en sí. Enriquecible desde APIs externas.
-- ─────────────────────────────────────────────────────────────
CREATE TABLE work (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    type              TEXT    NOT NULL,        -- BOOK | MOVIE | SERIES
    title             TEXT    NOT NULL,
    original_title    TEXT,
    sort_title        TEXT    NOT NULL,        -- sin artículos, para ordenar
    year              INTEGER,                 -- publicación / estreno
    synopsis          TEXT,
    language          TEXT,                    -- ISO 639-1

    -- portadas: URL remota + copia local (la local manda)
    cover_url         TEXT,
    cover_path        TEXT,                    -- relativo a filesDir/covers
    backdrop_path     TEXT,
    dominant_color    INTEGER,                 -- ARGB, extraído para el gradiente

    -- específicos de BOOK
    page_count        INTEGER,
    publisher         TEXT,
    isbn13            TEXT,
    isbn10            TEXT,

    -- específicos de MOVIE
    runtime_minutes   INTEGER,

    -- específicos de SERIES
    season_count      INTEGER,
    episode_count     INTEGER,
    episode_runtime   INTEGER,

    -- identificadores externos (para volver a enriquecer sin ambigüedad)
    source            TEXT,                    -- GOOGLE_BOOKS | OPENLIBRARY | TMDB | MANUAL
    source_id         TEXT,
    tmdb_id           INTEGER,
    imdb_id           TEXT,
    openlibrary_id    TEXT,

    created_at        INTEGER NOT NULL,
    updated_at        INTEGER NOT NULL
);

CREATE INDEX idx_work_type       ON work(type);
CREATE INDEX idx_work_sort_title ON work(sort_title);
CREATE UNIQUE INDEX idx_work_source ON work(source, source_id)
    WHERE source_id IS NOT NULL;             -- evita duplicados al re-importar

-- ─────────────────────────────────────────────────────────────
-- REGISTRO: tu experiencia. Una obra puede tener varios.
-- ─────────────────────────────────────────────────────────────
CREATE TABLE entry (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    work_id         INTEGER NOT NULL REFERENCES work(id) ON DELETE CASCADE,
    status          TEXT    NOT NULL,          -- PENDING|IN_PROGRESS|FINISHED|ABANDONED
    rating          INTEGER,                   -- 0..10  (medias estrellas). NULL = sin nota
    started_on      INTEGER,                   -- epoch day, no timestamp: la hora no importa
    finished_on     INTEGER,
    notes           TEXT,                      -- Markdown ligero
    format          TEXT,                      -- PAPER|EBOOK|AUDIO|CINEMA|STREAMING|TV
    is_favourite    INTEGER NOT NULL DEFAULT 0,
    progress_value  INTEGER,                   -- página actual | episodios vistos
    progress_season INTEGER,                   -- temporada actual (SERIES)
    round           INTEGER NOT NULL DEFAULT 1, -- 1ª lectura, 2ª lectura…
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL
);

CREATE INDEX idx_entry_work     ON entry(work_id);
CREATE INDEX idx_entry_status   ON entry(status);
CREATE INDEX idx_entry_finished ON entry(finished_on);

-- ─────────────────────────────────────────────────────────────
-- PERSONAS (autor, director, creador) — normalizado para poder
-- responder "todo lo de Sanderson" sin LIKE '%…%'
-- ─────────────────────────────────────────────────────────────
CREATE TABLE person (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL,
    sort_name  TEXT NOT NULL,
    UNIQUE(name)
);

CREATE TABLE work_person (
    work_id    INTEGER NOT NULL REFERENCES work(id)   ON DELETE CASCADE,
    person_id  INTEGER NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    role       TEXT    NOT NULL,   -- AUTHOR|DIRECTOR|CREATOR|TRANSLATOR|ILLUSTRATOR
    position   INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (work_id, person_id, role)
);

-- ─────────────────────────────────────────────────────────────
-- GÉNEROS (de la API) y ETIQUETAS (tuyas). Tablas distintas a
-- propósito: los géneros se re-sincronizan, tus tags no se tocan.
-- ─────────────────────────────────────────────────────────────
CREATE TABLE genre (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE);
CREATE TABLE work_genre (
    work_id  INTEGER NOT NULL REFERENCES work(id)  ON DELETE CASCADE,
    genre_id INTEGER NOT NULL REFERENCES genre(id) ON DELETE CASCADE,
    PRIMARY KEY (work_id, genre_id)
);

CREATE TABLE tag (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL UNIQUE,
    color INTEGER
);
CREATE TABLE work_tag (
    work_id INTEGER NOT NULL REFERENCES work(id) ON DELETE CASCADE,
    tag_id  INTEGER NOT NULL REFERENCES tag(id)  ON DELETE CASCADE,
    PRIMARY KEY (work_id, tag_id)
);

-- ─────────────────────────────────────────────────────────────
-- BÚSQUEDA: tabla virtual FTS4 sincronizada por triggers
-- ─────────────────────────────────────────────────────────────
CREATE VIRTUAL TABLE work_fts USING fts4(
    title, original_title, creators, notes,
    content=`work`, tokenize=unicode61 "remove_diacritics=2"
);
```

`remove_diacritics=2` es lo que hace que buscar `cronica` encuentre *Crónica*. Sin eso, la búsqueda en español es frustrante.

### 4.4 Room: entidades y consultas clave

```kotlin
@Entity(
    tableName = "work",
    indices = [
        Index("type"),
        Index("sort_title"),
        Index(value = ["source", "source_id"], unique = true),
    ]
)
data class WorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: MediaType,
    val title: String,
    @ColumnInfo(name = "original_title") val originalTitle: String? = null,
    @ColumnInfo(name = "sort_title")     val sortTitle: String,
    val year: Int? = null,
    // …
)

/** Proyección de lectura: solo lo que pinta la tarjeta. */
data class WorkCard(
    val id: Long,
    val type: MediaType,
    val title: String,
    val year: Int?,
    @ColumnInfo(name = "cover_path") val coverPath: String?,
    @ColumnInfo(name = "dominant_color") val dominantColor: Int?,
    val creators: String?,      // agregado con GROUP_CONCAT
    val status: Status?,
    val rating: Int?,
    @ColumnInfo(name = "finished_on") val finishedOn: Long?,
)

@Dao
interface LibraryDao {

    /**
     * Biblioteca filtrada. Un único query parametrizado en vez de N variantes:
     * los filtros nulos se neutralizan en el WHERE.
     * Toma el registro MÁS RECIENTE de cada obra (MAX(e.round)).
     */
    @Query("""
        SELECT  w.id, w.type, w.title, w.year, w.cover_path, w.dominant_color,
                (SELECT GROUP_CONCAT(p.name, ', ')
                   FROM work_person wp JOIN person p ON p.id = wp.person_id
                  WHERE wp.work_id = w.id AND wp.role IN ('AUTHOR','DIRECTOR','CREATOR')
                  ORDER BY wp.position)                     AS creators,
                e.status, e.rating, e.finished_on
        FROM    work w
        LEFT JOIN entry e ON e.id = (
                    SELECT id FROM entry
                     WHERE work_id = w.id
                     ORDER BY round DESC, id DESC LIMIT 1)
        WHERE  (:type   IS NULL OR w.type   = :type)
          AND  (:status IS NULL OR e.status = :status)
          AND  (:year   IS NULL OR CAST(strftime('%Y', e.finished_on * 86400,
                                        'unixepoch') AS INTEGER) = :year)
        ORDER BY
            CASE WHEN :sort = 'RECENT' THEN e.finished_on END DESC,
            CASE WHEN :sort = 'RATING' THEN e.rating      END DESC,
            CASE WHEN :sort = 'TITLE'  THEN w.sort_title  END ASC,
            w.updated_at DESC
    """)
    fun library(type: MediaType?, status: Status?, year: Int?, sort: String): Flow<List<WorkCard>>

    /** Búsqueda full-text sobre título, autoría y notas. */
    @Query("""
        SELECT w.id, w.type, w.title, w.year, w.cover_path, w.dominant_color,
               NULL AS creators, e.status, e.rating, e.finished_on
        FROM   work_fts f
        JOIN   work  w ON w.rowid = f.rowid
        LEFT JOIN entry e ON e.work_id = w.id
        WHERE  work_fts MATCH :query || '*'
        LIMIT  60
    """)
    fun search(query: String): Flow<List<WorkCard>>

    /** Ficha completa en una sola llamada. */
    @Transaction
    @Query("SELECT * FROM work WHERE id = :id")
    fun workDetail(id: Long): Flow<WorkWithRelations?>
}

data class WorkWithRelations(
    @Embedded val work: WorkEntity,
    @Relation(parentColumn = "id", entityColumn = "work_id")
    val entries: List<EntryEntity>,
    @Relation(
        parentColumn = "id", entityColumn = "id",
        associateBy = Junction(WorkPersonEntity::class,
            parentColumn = "work_id", entityColumn = "person_id")
    )
    val people: List<PersonEntity>,
    @Relation(
        parentColumn = "id", entityColumn = "id",
        associateBy = Junction(WorkTagEntity::class,
            parentColumn = "work_id", entityColumn = "tag_id")
    )
    val tags: List<TagEntity>,
)
```

### 4.5 Política de migraciones

Esto es lo que separa un proyecto serio de uno que un día te obliga a borrar los datos.

1. **Exportar esquemas a git desde el commit 1**:
   ```kotlin
   plugins { id("androidx.room") version "2.8.4" }
   room { schemaDirectory("$projectDir/schemas") }
   ```
   Los JSON en `app/schemas/` se versionan. Sin ellos no hay migraciones automáticas ni tests.
2. **`@AutoMigration` por defecto**; migración manual solo para renombrados y cambios de tipo (con `@RenameColumn` / `@DeleteColumn` como especificaciones).
3. **`fallbackToDestructiveMigration()` prohibido en release.** Solo en el `buildType` debug.
4. **Test de migración obligatorio por versión** con `MigrationTestHelper`: crea vN, migra a vN+1, verifica que las filas siguen ahí. Es un test de 15 líneas que te salva de un desastre silencioso.

---

## 5. Diseño de la interfaz

### 5.1 Principio rector

> Lo que ya has visto se consulta; lo que estás viendo se actualiza; lo que vas a ver se decide.

Tres modos de uso con pesos muy distintos: consultar (60 %), registrar (35 %), decidir (5 %). La navegación debe reflejarlos.

### 5.2 Mapa de pantallas

```
NavigationBar (3 destinos) + FAB
│
├─ 📚 Biblioteca            ← destino por defecto
│   ├─ Barra superior: buscador embebido + acciones (orden, layout)
│   ├─ Fila de filtros: [Todo][Libros][Pelis][Series]  ·  [Estado ▾][Año ▾]
│   ├─ Sección "En curso" (carrusel horizontal, solo si hay algo)
│   └─ Rejilla de portadas 3 col. (o lista densa, conmutable)
│        └─ toque → Ficha
│
├─ 📊 Estadísticas
│   ├─ Selector de año
│   ├─ Tarjetas: nº terminados · páginas · horas · nota media
│   ├─ Mapa de calor de actividad anual
│   ├─ Histograma de notas
│   └─ Top géneros / top autores
│
├─ ⚙️ Ajustes
│   ├─ Tema (sistema / claro / oscuro) · color dinámico on/off
│   ├─ Copia de seguridad: exportar · importar · auto-backup + carpeta
│   ├─ Idioma de metadatos, claves de API
│   └─ Gestión de etiquetas · Acerca de (+ atribución TMDB)
│
├─ FAB (Menú expandible M3)
│   ├─ 📖 Añadir libro     → Buscador (+ botón escáner ISBN)
│   ├─ 🎬 Añadir película  → Buscador TMDB
│   ├─ 📺 Añadir serie     → Buscador TMDB
│   └─ ✍️ Entrada manual
│
└─ Ficha de detalle
    ├─ Cabecera: backdrop desenfocado + portada + título + autoría + año
    ├─ Fila de acciones: [Estado ▾] [★ Nota] [♥] [Editar]
    ├─ Bloque "Mi registro": fechas, formato, progreso
    ├─ Bloque "Mis notas": texto libre, editable en línea
    ├─ Etiquetas (chips) + Géneros
    ├─ Sinopsis (colapsable)
    └─ Historial (si hay más de un registro)
```

### 5.3 El flujo de alta — la pantalla que decide si la app sobrevive

Objetivo: **3 toques y < 15 segundos** desde abrir la app hasta tener un libro terminado registrado con nota.

```
FAB  →  [Añadir libro]
   ↓
┌─────────────────────────────────────┐
│ 🔍 la sombra del vi|                │  búsqueda con debounce 300 ms
├─────────────────────────────────────┤
│ ▭  La sombra del viento             │  resultados con portada
│    C. Ruiz Zafón · 2001 · 576 pp    │
│ ▭  …                                │
└─────────────────────────────────────┘
   ↓  toque en un resultado
┌─────────────────────────────────────┐   ← ModalBottomSheet, NO pantalla nueva
│      ▭   La sombra del viento       │
│          C. Ruiz Zafón · 2001       │
│                                     │
│  ( Pendiente )( En curso )(●Leído)  │   chips; "Leído" preseleccionado
│                                     │
│      ★ ★ ★ ★ ☆    (arrastrable)     │
│                                     │
│  Terminado el:  [ 16 ago 2026 ]     │   hoy por defecto
│                                     │
│  ┌───────────────────────────────┐  │
│  │ Cuatro ideas…                 │  │   opcional, expandible
│  └───────────────────────────────┘  │
│                                     │
│            [   Guardar   ]          │
└─────────────────────────────────────┘
```

Detalles que marcan la diferencia:

- El estado **`Leído` viene preseleccionado**: el 90 % de los registros son de cosas ya consumidas.
- La fecha por defecto es hoy y se edita con un toque.
- La nota se pone arrastrando el dedo sobre las estrellas, con *haptic feedback* a cada media estrella.
- El campo de notas está colapsado a una línea; expandirlo es opcional. Nunca bloquea el guardado.
- Al guardar: `Snackbar` con acción **Deshacer** durante 5 s (patrón obligatorio para cualquier escritura y cualquier borrado).
- La portada se descarga y se cachea **en segundo plano**; el guardado no espera a la red.

### 5.4 Sistema visual

**Color.** Base Material 3 con `dynamicDarkColorScheme` / `dynamicLightColorScheme` cuando el sistema lo ofrece, y un esquema propio generado desde una semilla como respaldo y como opción explícita en ajustes. Semilla sugerida: un verde-tinta profundo (`#2F4A3E`) o un burdeos apagado (`#6B2D3C`) — colores de encuadernación, no de app de productividad.

Un acento adicional **por tipo de medio**, usado con moderación (solo en el borde del chip de tipo y en el icono):

```
BOOK   → tertiary   (cálido)
MOVIE  → secondary  (frío)
SERIES → primary
```

**Tipografía.** Aquí es donde una app se distingue por 20 líneas de código. La pila por defecto de Material es correcta pero anónima. Propuesta:

| Rol | Fuente | Uso |
|---|---|---|
| `displayLarge/Medium`, `headlineLarge` | **Fraunces** o **Lora** (serif variable) | Títulos de obra, cabeceras de sección |
| `titleMedium` … `bodySmall`, `label*` | **Inter** o Roboto Flex | Todo lo demás |

Serif para los títulos de obras, sans para la interfaz. Es el vocabulario visual de la industria editorial y hace que la app "sepa de libros" sin ilustraciones ni adornos. Ambas están en Google Fonts y se empaquetan como recursos (no uses el proveedor descargable: añade latencia en el primer frame).

**Forma y densidad.**

- Portadas de libro `4:6`, pósters de cine `2:3`, series `2:3`. **No las recortes al mismo aspecto**: la mezcla de proporciones en la rejilla es informativa y estéticamente viva. Usa `ContentScale.Crop` con altura fija por tipo.
- Esquinas `12.dp` en tarjetas, `20.dp` en sheets y diálogos, `full` en chips.
- Elevación por color de superficie (`surfaceContainer*`), no por sombras: es lo correcto en M3 y evita el aspecto "Android 2018".

**Movimiento.**

- Transición de elemento compartido portada→ficha (`SharedTransitionLayout`, estable). Es el único efecto "caro" que merece la pena: convierte una app funcional en una app agradable.
- `MaterialTheme.motionScheme.expressive` para muelles de resorte en chips y FAB.
- Nada de fades de 400 ms. Todo por debajo de 300 ms.

**Estados no felices** (lo que casi nadie hace y lo que más se nota):

- Vacío por tipo: ilustración mínima + una frase + botón directo a añadir.
- Vacío por filtro: "Ningún libro terminado en 2021" + botón *Quitar filtros*.
- Carga: *skeletons* con la forma exacta de las tarjetas, no un spinner centrado.
- Sin red durante una búsqueda: banner discreto + "puedes añadirlo a mano".
- Portada rota: placeholder con la inicial del título sobre el color dominante.

### 5.5 Accesibilidad y detalles de plataforma

- `contentDescription` en todas las portadas (`"Portada de {título}"`), y `null` en iconos puramente decorativos.
- Objetivos táctiles ≥ 48 dp. La fila de estrellas necesita cuidado especial.
- Edge-to-edge (obligatorio en `targetSdk 36`) con `WindowInsets` bien aplicados: la rejilla debe hacer scroll bajo la barra de estado.
- `predictiveBack` habilitado.
- Soporte de fuente grande sin recortes: prueba con 200 % de escala tipográfica.
- Widget con Glance mostrando lo que tienes en curso; toque → ficha directa.

---

## 6. Metadatos externos

### 6.1 Libros: estrategia de dos fuentes

Ninguna API es suficiente por sí sola para un lector en español.

| | Google Books | Open Library |
|---|---|---|
| Cobertura en español | **Buena** | Irregular, con lagunas notables |
| Clave de API | Opcional (recomendada) | No requiere |
| Límites | ~1 000 peticiones/día por proyecto | 1 req/s anónimo · **3 req/s** identificándote con `User-Agent` |
| Portadas | `imageLinks` (baja resolución, `zoom=` ajustable) | API de portadas por ISBN, S/M/L |
| Nº de páginas, editorial | Sí, bastante fiable | Parcial |
| Términos | Uso no comercial permitido, atribución | Prohibido el scraping masivo; "no es un backend de alto tráfico" |

**Diseño**: `BookSearchRepository` consulta Google Books como primaria (`q=…&langRestrict=es&country=ES&maxResults=20`); si un resultado no trae portada o le faltan páginas/editorial, se completa contra Open Library por ISBN. Las respuestas de búsqueda se cachean 24 h en OkHttp para no gastar cuota repitiendo consultas.

Open Library exige identificarse para conseguir 3 req/s:

```kotlin
.addInterceptor { chain ->
    chain.proceed(
        chain.request().newBuilder()
            .header("User-Agent", "Archivo/1.0 (danifuentes.lgl@gmail.com)")
            .build()
    )
}
```

### 6.2 Cine y TV: TMDB

TMDB es la elección obvia: catálogo completo, pósters y backdrops en varias resoluciones, sinopsis y títulos **en español** con `language=es-ES`, y una API gratuita para uso personal. Los límites operativos rondan las 40–50 peticiones por segundo (los límites duros originales se retiraron en 2019), muy por encima de lo que una app personal necesita.

Obligaciones que sí debes cumplir:

- Registrar una cuenta y solicitar la clave desde los ajustes de tu perfil.
- Mostrar el **logo de TMDB** en la pantalla "Acerca de".
- Incluir el texto exigido: *"Este producto usa la API de TMDB pero no está avalado ni certificado por TMDB."*
- No redistribuir el dataset. Cachear para tu propia app está bien.

Endpoints que necesitas (tres, nada más):

```
GET /3/search/multi?query={q}&language=es-ES&include_adult=false
GET /3/movie/{id}?language=es-ES&append_to_response=credits
GET /3/tv/{id}?language=es-ES&append_to_response=credits
```

`search/multi` devuelve películas, series y personas en una sola llamada — filtras `media_type` en el cliente y así una única caja de búsqueda cubre ambos tipos.

Las URLs de imagen se construyen con el `base_url` de `/3/configuration` (cachéalo, cambia muy rara vez): `{base}/w500{poster_path}` para pósters y `{base}/w780{backdrop_path}` para fondos.

### 6.3 Escaneo de ISBN

ML Kit *Barcode Scanning* en modo **agrupado** (`com.google.mlkit:barcode-scanning`, el modelo va dentro del APK): funciona sin red y sin la descarga inicial de Google Play Services. Configura el escáner solo para `FORMAT_EAN_13`, valida el prefijo `978`/`979`, y verifica el dígito de control antes de consultar la API. Combínalo con CameraX y `MlKitAnalyzer`, que resuelve el ciclo de vida y la rotación por ti.

Detalle de UX: al detectar un código, vibra y muestra el resultado en un sheet **sin cerrar la cámara** — así puedes escanear una pila de libros seguidos.

### 6.4 Gestión de claves de API

Las claves van en `local.properties` (fuera de git) y se inyectan como `BuildConfig`:

```kotlin
// app/build.gradle.kts
val props = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
android.defaultConfig {
    buildConfigField("String", "TMDB_KEY", "\"${props.getProperty("TMDB_KEY", "")}\"")
    buildConfigField("String", "BOOKS_KEY", "\"${props.getProperty("BOOKS_KEY", "")}\"")
}
```

Hay que ser honesto: **una clave dentro de un APK es extraíble**, siempre. Para una app personal cuyas claves son gratuitas y revocables, es un riesgo aceptable y la alternativa (un proxy propio) contradice el requisito de "sin servidores". Mitigación razonable: ofrecer en Ajustes un campo para que la clave la introduzca el usuario y se guarde en DataStore, dejando el `BuildConfig` como valor por defecto.

### 6.5 Independencia de las APIs (RNF-04)

Regla dura: **una obra guardada nunca vuelve a necesitar la red.** Al guardar:

1. Se descarga la portada y se escribe en `filesDir/covers/{workId}.jpg` (JPEG calidad 85, ancho máx. 600 px).
2. Se extrae el color dominante con Palette y se guarda como entero en `work.dominant_color`.
3. `cover_url` se conserva solo para poder re-descargar en mayor calidad si algún día quieres.
4. Coil lee de `cover_path`; la URL remota es únicamente el respaldo.

Esto también resuelve el rendimiento: la rejilla lee del disco local, no de la red.

---

## 7. Copias de seguridad — la parte que no puedes escatimar

En una app 100 % local, **el fallo catastrófico no es un crash: es perder cinco años de registros**. Este apartado merece tanto esfuerzo como la UI.

### 7.1 Formato

Un ZIP con extensión propia `.archivo`:

```
backup-2026-08-16.archivo   (zip)
├── manifest.json      { schemaVersion, appVersion, exportedAt, counts }
├── data.json          volcado completo y legible de todas las tablas
├── data.csv           exportación plana para interoperar (Goodreads-like)
└── covers/            *.jpg
```

JSON como formato canónico (fidelidad total, versionado, restauración exacta) y CSV como cortesía para hojas de cálculo o para migrar a otra app. Nunca copies el `.db` binario como formato principal: te ata a la versión exacta del esquema y no es inspeccionable.

Si algún día exportas el `.db` (útil para depurar), haz `PRAGMA wal_checkpoint(FULL)` antes o copiarás una base incompleta.

### 7.2 Mecánica

- **Exportación manual**: `ACTION_CREATE_DOCUMENT` (SAF) → el usuario elige destino, típicamente una carpeta sincronizada con Drive/Nextcloud/Syncthing. No necesitas permisos de almacenamiento.
- **Auto-backup**: el usuario concede una vez un `ACTION_OPEN_DOCUMENT_TREE`; se persiste el permiso (`takePersistableUriPermission`) y un `PeriodicWorkRequest` semanal (con restricción de batería no baja) escribe ahí, rotando las últimas 5 copias.
- **Importación**: `ACTION_OPEN_DOCUMENT` → previsualización ("142 obras, 168 registros, 3 de ellos ya existen") → elección entre **Fusionar** (por `source_id` o ISBN) o **Reemplazar todo** (con confirmación escrita).
- **`android:allowBackup="true"`** con `dataExtractionRules` que incluya la base y las portadas: la copia de Google es un segundo cinturón gratuito.
- Recordatorio no intrusivo si han pasado > 60 días sin copia.

### 7.3 Prueba de fuego

Un test instrumentado que debe existir desde la fase 2:

```
poblar BD con datos sintéticos → exportar → borrar BD → importar
→ afirmar igualdad campo a campo (incluidas relaciones y ficheros de portada)
```

Si este test no está verde, la app no es "robusta" por muy bonita que se vea.

---

## 8. Calidad, pruebas y tooling

### 8.1 Pirámide de pruebas realista para un proyecto en solitario

| Nivel | Qué | Herramienta | Cobertura objetivo |
|---|---|---|---|
| Unitarias | Mappers, cálculo de estadísticas, validación de ISBN, `sort_title`, lógica de fusión de backup | JUnit5 + kotlin.test | Alta (es barata y es donde están los bugs de verdad) |
| ViewModel | Emisiones de `StateFlow` ante acciones | Turbine + `TestDispatcher` | Media |
| DAO | Consultas complejas (la de biblioteca, la FTS) | Room in-memory, Robolectric | Alta en las 5–6 consultas críticas |
| Migración | vN → vN+1 | `MigrationTestHelper` | **Todas** |
| Captura de pantalla | Cada `@Preview` de los composables de contenido | Compose Preview Screenshot Testing (AGP) o Roborazzi | Todas las pantallas y estados vacíos |
| E2E | 3 recorridos: añadir, editar, exportar-importar | Compose UI Test | Solo esos tres |

Los **tests de captura de pantalla** merecen un comentario. Con `ComposablePreviewScanner` o el plugin de AGP generas capturas automáticamente desde tus `@Preview`, y el CI falla si un cambio altera un píxel sin que lo esperases. Para alguien que quiere que la app "se vea bien" y no tiene diseñador, esto es una red de seguridad enorme: convierte "creo que no he roto nada" en un diff visual.

### 8.2 Herramientas de proyecto

```
gradle/libs.versions.toml     catálogo de versiones (única fuente)
build-logic/                  convention plugins  (opcional, fase 6)
.editorconfig                 estilo unificado
detekt.yml + ktlint           análisis estático, ejecutado en CI
LeakCanary                    solo debugImplementation
StrictMode                    activado en Application, solo debug
Baseline Profile              generado con Macrobenchmark, fase 6
```

**CI en GitHub Actions**, tres jobs en paralelo:

```yaml
# .github/workflows/ci.yml
jobs:
  static:   # detekt + ktlint + lint de Android
  test:     # unit tests + Robolectric + screenshot tests
  build:    # assembleRelease firmado con secretos del repo
```

Y un `release.yml` disparado por tag `v*` que compila, firma y publica el APK en GitHub Releases. A partir de ahí, Obtainium en tu móvil detecta la nueva release y actualiza.

### 8.3 Compilación de release

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                      "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}
```

Reglas ProGuard necesarias: `@Serializable` de kotlinx (el plugin las aporta), y los DTO si usas reflexión. Room y Hilt generan sus propias reglas. Prueba **siempre** el APK minificado antes de etiquetar: los fallos de R8 aparecen en release, nunca en debug.

---

## 9. Distribución: lo que ha cambiado en 2026

Contexto que afecta directamente a este proyecto: Google está desplegando la **verificación de desarrolladores** para apps instaladas fuera de Play. Las protecciones de cara al usuario empiezan el **30 de septiembre de 2026** en Brasil, Indonesia, Singapur y Tailandia, y se extienden globalmente a lo largo de **2027**.

Qué significa para ti:

- **Instalación por ADB: sigue permitida.** Durante el desarrollo, nada cambia.
- Existe una **cuenta gratuita para aficionados y estudiantes**: sin documento de identidad, solo correo electrónico, con distribución limitada a **20 dispositivos**. Lanzamiento global previsto para agosto de 2026. Para una app personal es exactamente lo que necesitas.
- La alternativa completa (Android Developer Console, ~25 USD, con verificación de identidad) solo tiene sentido si algún día quieres publicarla.

**Ruta recomendada, por orden:**

1. **Desarrollo**: `./gradlew installDebug` por ADB o Wi-Fi.
2. **Uso diario**: APK de release firmado con tu propio keystore, publicado en un repositorio privado de GitHub, instalado y actualizado con **Obtainium** (apunta a la URL de releases y se actualiza solo).
3. **Si un día lo abres al público**: F-Droid (requiere licencia libre y build reproducible) o Play Store con canal de pruebas internas.

**Guarda el keystore y sus contraseñas fuera del repositorio y con copia de seguridad.** Si lo pierdes, no puedes volver a actualizar la app instalada; solo desinstalar y reinstalar perdiendo los datos (salvo que tengas backup, que lo tendrás).

---

## 10. Plan de trabajo por fases

Cada fase termina en un APK instalable y usable. Nunca hay un estado "a medias" de varias semanas.

### Fase 0 — Cimientos · 4–6 h
- Proyecto con AGP 9 / Kotlin 2.3, catálogo de versiones, Hilt, `MainActivity` con Compose y tema.
- Sistema de diseño: `Color.kt` con esquema dinámico + fallback, `Type.kt` con Fraunces/Inter, `Shape.kt`.
- Room configurado con exportación de esquemas y el plugin de Room.
- `NavigationSuiteScaffold` con 3 destinos vacíos y navegación Nav3 funcionando.
- CI mínimo (compila + detekt).

**Hecho cuando**: la app arranca, cambia entre tres pantallas vacías y respeta el tema del sistema.

### Fase 1 — MVP local · 10–14 h
- Entidades `work`, `entry`, `person`, `work_person` + DAOs + repositorio.
- Alta **manual** completa (los tres tipos).
- Rejilla de biblioteca con filtros de tipo y estado.
- Ficha de detalle con edición de estado, nota, fechas y texto libre.
- Borrado con deshacer.

**Hecho cuando**: puedes registrar a mano todo lo que leíste este año y consultarlo. **A partir de aquí, empieza a usarla de verdad**, aunque falte todo lo demás. Los datos reales revelan los problemas de diseño que ninguna especulación anticipa.

### Fase 2 — Blindaje de datos · 6–8 h
- Exportación e importación `.archivo` (JSON + CSV + portadas) vía SAF.
- Auto-backup semanal con WorkManager y rotación.
- Test de ida y vuelta (§7.3) y primer test de migración.

**Hecho cuando**: puedes desinstalar la app, reinstalarla y recuperarlo todo. Antes de esto, no metas datos que te dolería perder.

### Fase 3 — Metadatos de libros · 8–10 h
- Retrofit + OkHttp + kotlinx.serialization, interceptores de caché y `User-Agent`.
- Búsqueda en Google Books con complemento de Open Library.
- `CoverStore`: descarga, redimensionado, color dominante con Palette.
- Sheet de alta rápida (§5.3) con estados de carga y error.

**Hecho cuando**: añadir un libro con portada cuesta tres toques.

### Fase 4 — Cine y series · 5–7 h
- Cliente TMDB con `search/multi`, detalle de película y de serie, configuración de imágenes.
- Reutilización íntegra del sheet de alta (aquí se cobra el dividendo del modelo unificado).
- Progreso ligero de series (`SxxEyy`) y pantalla "Acerca de" con la atribución de TMDB.

**Hecho cuando**: la biblioteca mezcla los tres tipos con coherencia visual.

### Fase 5 — Búsqueda, etiquetas y estadísticas · 8–10 h
- FTS4 con triggers de sincronización, `remove_diacritics=2`, resaltado de coincidencias.
- Etiquetas del usuario con gestión y filtrado.
- Pantalla de estadísticas: tarjetas de resumen, mapa de calor anual, histograma de notas, top géneros/autores.

**Hecho cuando**: encuentras cualquier cosa en menos de 3 segundos, incluida por lo que escribiste en la nota.

### Fase 6 — Acabado · 8–12 h
- Transiciones de elemento compartido portada→ficha.
- Escáner de ISBN (ML Kit + CameraX).
- Widget con Glance de lo que tienes en curso.
- `share intent`: compartir una URL de una película desde el navegador y que la app la resuelva.
- Estados vacíos ilustrados, skeletons, revisión completa de accesibilidad.
- Baseline Profile, R8 verificado, tests de captura de pantalla en CI.

### Fase 7 — Opcionales, según apetezca
- Listas personalizadas y ordenación manual.
- Importación desde CSV de Goodreads / Letterboxd / Openreads.
- Recomendaciones locales sencillas ("no has terminado nada desde hace 3 semanas").
- Exportación a Markdown para Obsidian.
- Migración a Kotlin Multiplatform si algún día quieres escritorio (Room y Compose ya lo soportan).

---

## 11. Riesgos y decisiones a revisar

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| Abandono por fricción de registro | **Alta** | Fatal | Fase 1 usable ya, sheet de 3 toques en fase 3, medir tu propio uso |
| Sobreingeniería antes de tener datos reales | **Alta** | Alto | Un módulo hasta fase 5; usar la app desde fase 1 |
| Pérdida de datos | Media | Fatal | Fase 2 antes que cualquier feature vistosa |
| Metadatos pobres en literatura española | Media | Medio | Dos fuentes + edición manual siempre disponible |
| Cambio de términos de TMDB | Baja | Medio | Portadas y metadatos copiados en local (RNF-04) |
| Endurecimiento del sideloading | Media | Bajo | Cuenta gratuita de aficionado (20 dispositivos); ADB sigue disponible |
| Deuda de migraciones | Media | Alto | Esquemas en git y test por versión desde el día 1 |

**Decisiones deliberadamente aplazadas** (no las resuelvas ahora):

- Seguimiento episodio a episodio → solo si al usar la app lo echas de menos de verdad.
- Modularización Gradle → solo cuando el build moleste.
- Sincronización multidispositivo → el backup en carpeta sincronizada cubre el 95 % del caso.
- Soporte de tablet más allá de `NavigationSuiteScaffold` → cuando tengas una tablet.

---

## 12. Cómo arrancar mañana

```bash
# 1. Proyecto nuevo: Android Studio → Empty Activity (Compose)
#    Nombre: Archivo · Paquete: com.dfuentes.archivo
#    minSdk 31 · Kotlin DSL · version catalog

# 2. Primer commit antes de tocar nada
git init && git add . && git commit -m "chore: proyecto base"

# 3. Orden de trabajo recomendado dentro de la fase 0
#    a) libs.versions.toml completo
#    b) theme/ (Color, Type, Shape, Theme)  ← ver la app en tu color antes que nada
#    c) Room + Hilt + esquema exportado
#    d) Nav3 + NavigationSuiteScaffold
```

Tres consejos operativos:

1. **Escribe las entidades y los DAOs antes que ninguna pantalla.** El modelo de datos es el contrato; la UI es negociable.
2. **Crea un `@Preview` para cada composable de contenido desde el principio**, con datos falsos. Iterar el diseño en el panel de previsualización es 10× más rápido que desplegando al móvil, y esos previews son gratis los tests de captura de pantalla de la fase 6.
3. **Un commit por unidad funcional, mensajes convencionales** (`feat:`, `fix:`, `refactor:`). Cuando en tres meses vuelvas al proyecto, el historial es la única documentación que habrá sobrevivido.

---

## Fuentes

- [Guía de arquitectura de apps — Android Developers](https://developer.android.com/topic/architecture)
- [Compose Material 3 — notas de versión](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Compose BOM y mapeo de versiones](https://developer.android.com/develop/ui/compose/bom)
- [Novedades de Jetpack Compose, abril 2026](https://android-developers.googleblog.com/2026/04/jetpack-compose-april-2026-updates.html)
- [Room — notas de versión](https://developer.android.com/jetpack/androidx/releases/room)
- [Room 3.0: Modernizing Room (alpha)](https://android-developers.googleblog.com/2026/03/room-30-modernizing-room.html)
- [Hilt (androidx) — notas de versión](https://developer.android.com/jetpack/androidx/releases/hilt)
- [Android Gradle plugin 9.3.0 — notas de versión](https://developer.android.com/build/releases/agp-9-3-0-release-notes)
- [Jetpack Navigation 3 es estable](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html)
- [Navigation 3 — notas de versión](https://developer.android.com/jetpack/androidx/releases/navigation3)
- [ML Kit Barcode Scanning en Android](https://developers.google.com/ml-kit/vision/barcode-scanning/android)
- [ML Kit Analyzer con CameraX](https://developer.android.com/media/camera/camerax/mlkitanalyzer)
- [APIs de Open Library (límites y términos)](https://openlibrary.org/developers/api)
- [Open Library Covers API](https://openlibrary.org/dev/docs/api/covers)
- [Google Books API — uso y cuotas](https://developers.google.com/books/docs/v1/using)
- [TMDB — límites de tasa](https://developer.themoviedb.org/docs/rate-limiting)
- [TMDB — términos de uso de la API](https://www.themoviedb.org/api-terms-of-use)
- [Verificación de desarrolladores de Android — blog oficial](https://android-developers.googleblog.com/2026/03/android-developer-verification-rolling-out-to-all-developers.html)
- [Cómo afectan las reglas de verificación al sideloading](https://android.gadgethacks.com/news/how-android-sideloading-verification-rules-affect-f-droid-and-privacy-tools/)
- [Requisitos de target API level en Google Play](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Kotlin 2.3.20 — JetBrains Blog](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/)
- [Openreads — referencia de app libre de seguimiento de lecturas](https://github.com/mateusz-bak/openreads)
- [Obtainium — actualizaciones de APKs desde GitHub](https://github.com/ImranR98/Obtainium)
- [Coil — carga de imágenes en Compose](https://github.com/coil-kt/coil)
- [ComposablePreviewScanner — tests de captura desde previews](https://github.com/sergio-sastre/ComposablePreviewScanner)
