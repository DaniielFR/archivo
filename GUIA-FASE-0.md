# Guía de arranque — Fase 0

Esto es el esqueleto completo de la fase 0 del informe: proyecto configurado,
sistema de diseño, Room con esquema exportado, navegación y CI.

**Antes de nada, una advertencia honesta:** este código está escrito, revisado y
razonado, pero **no lo he podido compilar** — el entorno donde trabajo no tiene
acceso al SDK de Android ni a los repositorios de Maven. Trátalo como una
revisión de código muy buena, no como un artefacto verificado. La sección §5
lista los tres o cuatro puntos donde es más probable que Gradle proteste y cómo
resolverlos en un minuto. Ninguno afecta al diseño; son versiones y nombres de
artefacto.

---

## 1. Por qué empezamos por el asistente de Android Studio

El ZIP **no incluye el Gradle wrapper** (`gradlew`, `gradlew.bat` y
`gradle/wrapper/`). Es deliberado: el `.jar` del wrapper es un binario y la
versión de Gradle debe casar exactamente con tu AGP y tu JDK. Inventármela sería
la forma más rápida de que pierdas media hora.

Así que el orden es: el asistente crea una base garantizada, y encima copiamos
lo que sí aporta valor.

```
1. Android Studio → New Project → Empty Activity (Compose)
     Name:            Archivo
     Package name:    com.dfuentes.archivo
     Language:        Kotlin
     Minimum SDK:     API 31
     Build config:    Kotlin DSL + Gradle version catalogs   ← marca esta opción

2. Deja que sincronice una vez y comprueba que arranca en el emulador.
   Este es tu punto de retorno seguro. Haz commit aquí.

       git init
       git add .
       git commit -m "chore: proyecto base del asistente"

3. Descomprime este ZIP encima de la carpeta del proyecto, sobrescribiendo.
   NO sobrescribas: gradlew, gradlew.bat, gradle/wrapper/

4. Sincroniza. Ve a la §5 si algo se queja.
```

Si prefieres hacerlo a mano: copia primero `gradle/wrapper/`, `gradlew` y
`gradlew.bat` del proyecto del asistente a la carpeta del ZIP, y trabaja ahí.

---

## 2. Qué hay dentro y por qué

```
gradle/libs.versions.toml     Catálogo de versiones: la única fuente de verdad.
                              Ninguna versión se escribe en un build.gradle.

app/build.gradle.kts          minSdk 31 (umbral del color dinámico de Material You),
                              targetSdk 36, R8 activado en release, opt-ins
                              centralizados y exportación del esquema de Room.

core/model/                   Kotlin puro, sin una sola anotación de Android.
                              Es la parte del proyecto que sobrevivirá a todo.

core/database/                Entidades, DAOs, converters, base de datos.
                              El esquema del informe §4.3, tal cual.

core/designsystem/            Color, tipografía, formas y componentes compartidos.

data/                         Repositorio y mappers.

feature/                      Una carpeta por pantalla, siempre con el mismo
                              patrón: UiState + Action + ViewModel + Route + Screen.
```

### Las tres decisiones que verás en el código y conviene que reconozcas

**a) `Work` y `Entry` están separados.** Una obra es la cosa (título, autor,
año); un registro es tu experiencia con ella (estado, nota, fechas, texto). Una
obra puede tener varios registros, y por eso releer algo no machaca lo que
pensaste la primera vez. Es la decisión con mayor coste de reversión de todo el
proyecto, y es la razón de que la consulta de biblioteca tenga ese `LEFT JOIN`
con subconsulta: toma el registro vigente, no todos.

**b) Los tres tipos comparten tabla.** `MediaType` es un discriminador, no una
jerarquía. Añadir videojuegos algún día es literalmente una constante más en el
enum. El precio son unas cuantas columnas nulas, que en SQLite no cuestan nada.

**c) En la fase 0 no hay librería de navegación.** Tres pestañas hermanas sin
pila de retroceso entre ellas se resuelven con un `when` sobre una variable de
estado. Navigation 3 entra en la fase 1, cuando aparezca la ficha de detalle y
con ella una pila real y las transiciones de portada. Meter la dependencia hoy
sería configuración sin beneficio.

---

## 3. Verificación: cómo saber que la fase 0 está hecha

Marca las cinco y pasamos a la fase 1.

- [ ] **Compila.** `./gradlew :app:assembleDebug` termina sin errores.
- [ ] **Arranca.** Se ven tres pestañas y el estado vacío de la biblioteca.
- [ ] **El tema responde.** Cambia el fondo de pantalla del móvil a algo de otro
      color y reabre la app: los acentos deben cambiar con él. Si no, el color
      dinámico no está entrando.
- [ ] **El ciclo de datos funciona.** Pulsa *Añadir datos de ejemplo* en el
      estado vacío. Deben aparecer tres tarjetas **sin refrescar nada**: eso
      demuestra que Room → Flow → StateFlow → recomposición está conectado de
      punta a punta, que es todo lo que la fase 0 tenía que demostrar.
- [ ] **El esquema se exportó.** Existe `app/schemas/…/1.json`. **Haz commit de
      ese fichero.** Sin él no hay migraciones automáticas ni tests de migración,
      y lo descubrirás el día que ya sea tarde.

Y `./gradlew :app:testDebugUnitTest` debe pasar: hay cinco tests de la clave de
ordenación (`sortTitleOf`), que es la función más fácil de romper sin enterarse.

---

## 4. Qué toca a continuación (fase 1, 10–14 h)

Por orden. Lo primero es lo que menos apetece y lo que más importa.

1. **Alta manual.** Un formulario con los tres tipos. Feo si hace falta, pero
   completo. Reutiliza `RatingStars`, que ya está hecho y con háptica.
2. **Navigation 3** y la ficha de detalle: biblioteca → ficha → editar.
3. **Edición en la ficha**: estado, nota, fechas, texto libre, todo en línea.
4. **Borrado con deshacer.** `Snackbar` con acción, 5 segundos. Patrón
   obligatorio para cualquier escritura destructiva, desde la primera.
5. **Preferencias en DataStore**: tema y disposición de la rejilla, que ahora
   viven en memoria y se pierden al cerrar.
6. **Quita `SampleDataRequested`** de `LibraryAction` y su rama del ViewModel.

Y entonces —esto es lo importante— **empieza a usarla de verdad**, con las
lecturas de este año, aunque no haya búsqueda ni portadas ni estadísticas. Los
datos reales revelan en una semana problemas de diseño que ninguna especulación
anticipa, y es mucho más barato descubrirlos antes de construir las fases 3 a 6
encima.

Una excepción a "úsala ya": **no metas nada que te dolería perder hasta tener la
fase 2**, la de copias de seguridad. Es una app local; hasta que exportar
funcione, una desinstalación accidental se lo lleva todo.

---

## 5. Si Gradle protesta

Estos son los puntos donde es más probable que haya que ajustar algo, ordenados
por probabilidad. Todos se arreglan en el catálogo de versiones y ninguno afecta
al diseño.

**«Plugin/dependencia no encontrada» en `agp`, `kotlin` o `ksp`.**
Quédate con las versiones que generó el asistente. `ksp` debe compartir prefijo
con `kotlin`: si Kotlin es `2.3.20`, KSP es `2.3.20-x.y.z`. Descuadrar esos dos
es el error más común del ecosistema.

**`hilt = "2.57"` no resuelve.**
Es la versión de la que menos seguro estoy. Consulta la última en
[github.com/google/dagger/releases](https://github.com/google/dagger/releases) y
cámbiala. `androidxHilt` (1.4.0) es independiente y va aparte.

**`material3-adaptive-navigation-suite` reclama una versión.**
Está declarado sin versión asumiendo que lo gobierna el BOM de Compose. Si no
fuera así, dale versión explícita:

```toml
androidx-compose-material3-adaptive-navigation-suite = { group = "androidx.compose.material3", name = "material3-adaptive-navigation-suite", version.ref = "material3Adaptive" }
```

**`NavigationSuiteScaffold` pide un opt-in.**
Ya está cubierto en `app/build.gradle.kts` con `-opt-in=…`. Si el nombre del
marcador hubiera cambiado, Kotlin solo emite un *warning* por el opt-in
desconocido; añade el `@OptIn` que te sugiera el IDE en `ArchivoApp.kt` y listo.

**Room se queja de un TypeConverter.**
No debería: las proyecciones devuelven `String` a propósito y la conversión a
enum ocurre en `Mappers.kt`, justo para evitar los casos nullable ambiguos. Si
aun así protesta, dime el mensaje exacto.

**El emulador arranca en blanco.**
Casi siempre es Hilt: falta `@AndroidEntryPoint` en `MainActivity` o
`@HiltAndroidApp` en `ArchivoApplication`, o `android:name=".ArchivoApplication"`
no está en el manifiesto. Los tres están puestos, pero es lo primero que miro.

---

## 6. Cosas que dejo apuntadas para no olvidarlas

- **Fuentes reales.** `Type.kt` está preparado para Fraunces (títulos) e Inter
  (interfaz), con las instrucciones dentro. Son cinco minutos y es lo que más
  cambia el aspecto de la app por unidad de esfuerzo. La estructura serif/sans
  ya está, solo faltan los `.ttf`.
- **Icono provisional.** El de `ic_launcher_foreground.xml` es un marcador de
  libro que dibujé a mano; cámbialo cuando te apetezca.
- **Paleta afinada.** Si quieres los tonos exactos en vez de mis aproximaciones,
  genera `Color.kt` con Material Theme Builder usando la semilla `#2F4A3E`. Los
  nombres de los slots son los mismos, se sustituye el fichero entero.
- **Keystore.** Cuando llegues a la fase 6 y firmes el APK: guárdalo fuera del
  repositorio y con copia de seguridad. Perderlo significa no poder volver a
  actualizar la app instalada.
