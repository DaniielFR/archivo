# CAMBIOS-SETUP.md

Registro de ajustes hechos durante el montaje de la Fase 0 (por Claude Code).
Solo se tocaron **versiones y errores de compilación**, nunca la arquitectura
(esquema de BD, capas ni estructura de paquetes).

## Entorno usado

- **JDK**: Temurin **21.0.12** local en `~/opt/jdk-21.0.12+8` (el sistema solo
  tenía JDK 11; se instaló en el home para no usar `sudo apt`).
  `JAVA_HOME`/`PATH` en `~/.archivo-env.sh`.
- **Android SDK**: `~/Android/Sdk` vía command-line tools. Instalado
  `platform-tools`, `platforms;android-36`, `platforms;android-37.0`,
  `build-tools;36.0.0`, `build-tools;37.0.0`.
- **Gradle**: **9.7.0** (última estable a fecha de montaje), compatible con AGP 9.3.
  El wrapper se generó con esa versión.

## Versiones y ajustes hechos

### 1. `gradle/libs.versions.toml`

| Clave | Antes | Ahora | Motivo |
|---|---|---|---|
| `ksp`  | `2.3.20-2.0.0` | `2.3.11` | El artefacto `2.3.20-2.0.0` no existe. KSP cambió de esquema de versionado: desde la línea 2.3 usa versión propia (`2.3.0`…`2.3.11`) en vez del viejo `<kotlin>-<ksp>`. KSP `2.3.11` está construido contra Kotlin **2.3.20** (verificado en su POM), así que casa con el catálogo. **La regla de la guía "KSP debe compartir prefijo con Kotlin" quedó obsoleta.** |
| `hilt` | `2.57` | `2.60.1` | Hilt 2.57 fallaba al aplicar el plugin con *"Android BaseExtension not found"*: esa API de AGP desapareció en AGP 9. La 2.60.1 (última) ya es compatible con AGP 9. |

### 2. `build.gradle.kts` (raíz) y `app/build.gradle.kts` — plugin Kotlin

Se **quitó la aplicación del plugin `org.jetbrains.kotlin.android`** en ambos
ficheros (en la raíz estaba con `apply false`, en `app` aplicado).

- **Motivo**: AGP 9.0+ trae *built-in Kotlin*; aplicar `kotlin-android` ahora
  es un **error de build** (*"The 'org.jetbrains.kotlin.android' plugin is no
  longer required for Kotlin support since AGP 9.0"*).
- Se **mantienen** el plugin de Compose (`org.jetbrains.kotlin.plugin.compose`)
  y KSP (`com.google.devtools.ksp`), que sí siguen aplicándose por separado.
- El bloque `kotlin { compilerOptions { … } }` sigue siendo válido y no se tocó.
- La entrada `kotlin-android` del catálogo se dejó (sin usar) para no ampliar el
  diff; la versión `kotlin = "2.3.20"` la sigue usando el plugin de Compose.
- Ref.: https://developer.android.com/build/releases/agp-9-0-0-release-notes#android-gradle-plugin-built-in-kotlin

### 3. `app/build.gradle.kts` — `compileSdk`

| Antes | Ahora |
|---|---|
| `compileSdk = 36` | `compileSdk = 37` |

- **Motivo**: las librerías que arrastra el Compose BOM `2026.08.00`
  (p. ej. `compose-ui 1.12.0`, `lifecycle 2.11.0`) **exigen compilar contra
  API 37+**; con `compileSdk = 36` AGP aborta el build.
- Se subió **solo `compileSdk`**. `targetSdk` (36) y `minSdk` (31) **no se
  tocaron**, así que el comportamiento en tiempo de ejecución no cambia.

### 4. `core/di/DispatchersModule.kt` — nombre de método (arreglo de codegen)

Se renombró el método proveedor `fun default()` → `fun defaultDispatcher()`.

- **Motivo**: Dagger (vía KSP) usa el nombre del método como identificador en el
  código Java que genera, y `default` es **palabra reservada de Java** →
  *"The name 'default' cannot be used because it is a Java keyword"*.
- **No cambia la arquitectura ni el binding**: Hilt liga por tipo de retorno +
  qualifier (`@DefaultDispatcher`), no por el nombre del método.

## Resultado

- `./gradlew :app:testDebugUnitTest` → **5/5 tests OK** (`SortTitleTest`).
- `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**, APK en
  `app/build/outputs/apk/debug/app-debug.apk` (applicationId
  `com.dfuentes.archivo.debug`).
- Esquema de Room exportado en
  `app/schemas/com.dfuentes.archivo.core.database.ArchivoDatabase/1.json`.

Quedan solo *warnings* (asserts `!!` innecesarios en `RatingStars.kt`, iconos
`StarHalf`/`hiltViewModel` deprecados, target de anotación en
`LibraryRepositoryImpl.kt`). No rompen el build y no se tocaron por estar fuera
del alcance de "montar la fase 0".
