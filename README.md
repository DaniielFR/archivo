# Archivo

Aplicación personal de Android para archivar lecturas, películas y series.
100 % local: los datos viven en el dispositivo y salen de él únicamente en las
copias de seguridad que exportas tú.

## Estado

**Fase 3 — metadatos de libros.** Ver `GUIA-FASE-0.md` para arrancar y
`informe-tecnico.md` para el diseño completo y el plan por fases.

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Proyecto, tema, Room, navegación, CI | ✅ |
| 1 | MVP: alta manual, biblioteca, ficha de detalle | ✅ |
| 2 | Exportación/importación y copia automática | ✅ |
| 3 | Metadatos de libros (Google Books + Open Library) | ✅ |
| 4 | Cine y series (TMDB) | ⬜ |
| 5 | Búsqueda FTS, etiquetas, estadísticas | ⬜ |
| 6 | Acabado: transiciones, escáner ISBN, widget, tests visuales | ⬜ |

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · Hilt · minSdk 31 · targetSdk 36

## Arquitectura

Tres capas con flujo de datos unidireccional. La base de datos es la única
fuente de verdad: la red (a partir de la fase 3) escribe en Room y la UI observa
Room, nunca al revés.

```
feature/   pantallas: Route (con estado) + Screen (sin estado, previsualizable)
data/      repositorios y mappers
core/      model · database · designsystem · di · util
```

## Comandos

```bash
./gradlew :app:assembleDebug        # compilar
./gradlew :app:installDebug         # instalar por ADB
./gradlew :app:testDebugUnitTest    # tests unitarios
./gradlew :app:lintDebug            # lint
```

## Reglas del proyecto

1. Los composables no conocen Room, Retrofit ni `Context`.
2. Cada capa tiene su modelo; los mappers viven en la capa que consume.
3. Los dispatchers se inyectan, nunca se referencian directamente.
4. `fallbackToDestructiveMigration()` está prohibido fuera de debug.
5. El esquema de Room en `app/schemas/` se versiona en git. Siempre.
