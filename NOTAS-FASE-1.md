# Notas de verificación — Fase 1 (MVP local)

Verificado el 2026-08-16 sobre móvil físico Xiaomi `rodin` (Android 16 / API 36),
sin Android Studio, todo por ADB. Rama `fase-1`.

## Cómo llegó y qué hubo que tocar

Fase 1 se aplicó desde `fase1.patch` (`git am`, commit `feat: fase 1 — MVP local`).
El parche **no compilaba tal cual**; dos arreglos mínimos (commit `fix(fase-1):
compilar el patch de fase 1`), sin tocar arquitectura:

1. `LibraryRepositoryImpl.updateWork` devolvía `Unit?` en vez de `Unit` (el bloque
   de la transacción terminaba en `work.currentEntry?.let { … }`). Se añadió `Unit`
   al final del bloque.
2. `ArchivoApp`: `dropUnlessResumed` solo envuelve `() -> Unit`; no sirve para
   callbacks con argumento. `onOpenWork` / `onAddWork` / `onEdit` pasaron a lambdas
   tipadas simples (como los `onBack`/`onDone` de al lado) y se quitó el import.

Las 4 dependencias nuevas que el autor marcó como riesgo **resolvieron sin cambios**:
Navigation 3 `1.1.6`, `lifecycle-viewmodel-navigation3` `2.11.0`,
`hilt-lifecycle-viewmodel-compose` `1.4.0`, `kotlinx-serialization-json` `1.9.0`,
más el plugin `kotlin.plugin.serialization`.

## Qué se verificó (automatizado, por ADB)

- **Tests unitarios: 11/11 OK** (5 `SortTitleTest` + 6 `AddEditViewModelTest`).
- `assembleDebug` compila; APK instalado y arrancando sin crashes.
- **0 fatales en logcat** durante todo el recorrido.
- **Alta end-to-end**: FAB → hoja de tipo → *Libro* → título "Dune" → *Guardar* →
  la tarjeta aparece en la biblioteca **sin refrescar** (valida Room → Flow → UI).
- Formulario con campo específico por tipo (*Páginas* en libro), estado
  preseleccionado en *Terminado* y fecha de hoy autorrellenada.
- **Ficha de detalle**: abre con estado / valoración / fechas / notas editables y
  acciones Editar / Eliminar / Favorito / Volver. La barra de navegación
  desaparece en la ficha (comportamiento deliberado).
- Tres pestañas (Biblioteca / Estadísticas / Ajustes) y filtros de tipo
  (Todo / Libros / Películas / Series).

## Pendiente de validación humana (requiere tacto/vista, no por ADB)

- Estrellas: vibración en cada media estrella; volver a pulsar la misma quita la nota.
- Deshacer: borrar y recuperar desde el Snackbar.
- Tema oscuro + apagar color dinámico (paleta verde tinta) y que **persista** al
  reabrir (DataStore).
- Relectura: bloque Historial con dos vueltas.

## Estado del dispositivo

- Quedó una obra de prueba **"Dune"** metida a mano. Borrarla (sirve para probar
  el *Deshacer*).

## Deuda conocida a tener presente en Fase 2+ (ver `CLAUDE.md` §Deuda)

- ⚠️ **No hay copias de seguridad todavía**: una desinstalación se lleva todo.
  No meter datos irremplazables hasta cerrar Fase 2.
- Warnings pendientes (Fase 6, `allWarningsAsErrors`): `!!` en `RatingStars.kt`,
  iconos `StarHalf`/`Sort` deprecados, target de anotación en
  `LibraryRepositoryImpl` / `SettingsRepository`.
- `LibraryLayout.LIST` se persiste pero la rejilla aún no lo respeta.
- Las notas se guardan en cada pulsación: falta un debounce (~400 ms).
- Sin pantalla de progreso (página / SxxEyy) todavía.

## Kickoff Fase 2 — Blindaje de datos (~6–8 h)

Objetivo: poder desinstalar, reinstalar y recuperarlo todo. Alcance del informe §10:

1. Export/import formato `.archivo` (JSON + CSV + portadas) vía SAF.
2. Auto-backup semanal con WorkManager + rotación.
3. Test de ida y vuelta (informe §7.3) y **primer test de migración** con
   `MigrationTestHelper`.

Recordatorios de arquitectura al abordarla (de `CLAUDE.md`):
- La BD es la única fuente de verdad; el esquema en `app/schemas/` se versiona en git.
- Cualquier cambio de esquema: sube `version`, añade `@AutoMigration` **y su test**.
- Nada de `fallbackToDestructiveMigration()` fuera de debug.
- No añadir dependencias sin preguntar (WorkManager habrá que consultarlo).
