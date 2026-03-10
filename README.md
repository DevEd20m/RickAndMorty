# Rick & Morty Characters — Android Challenge

> Challenge técnico para **Arkamo** — puesto de Android Developer.

---

## Resumen

El objetivo del challenge era crear una pantalla Android que consuma la API pública de Rick & Morty y muestre una lista de personajes. Se tomaron decisiones adicionales de arquitectura para reflejar cómo construiría una feature en un entorno de producción real.

---

## Stack técnico

| Categoría | Librería / Herramienta |
|---|---|
| Lenguaje | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Clean Architecture |
| Inyección de dependencias | Hilt 2.51.1 |
| Networking | Retrofit 2.11 + OkHttp 4.12 |
| Serialización | kotlinx.serialization 1.7.3 |
| Carga de imágenes | Coil 2.7.0 |
| Paginación | Paging 3 (3.3.4) |
| Configuración remota | Firebase Remote Config |
| Analytics | Firebase Analytics |
| Asincronía | Coroutines + Flow + StateFlow |
| Testing | JUnit 4 · MockK · Turbine · coroutines-test · paging-testing |
| Build system | Gradle Kotlin DSL + Version Catalog + Convention Plugins |

---

## Arquitectura

El proyecto sigue **MVVM + Clean Architecture** en una estructura **multi-módulo**:

```
app/
core/
  network/        ← Retrofit, OkHttp, DTOs, NetworkModule
  ui/             ← Tema, colores, tipografía (design system compartido)
  sdui/           ← Firebase Remote Config → ScreenConfig, mapper, use case
  presentation/   ← BaseViewModel con manejo de errores en coroutines
feature/
  characters/
    data/         ← CharacterRepositoryImpl, CharacterPagingSource, mapper
    domain/       ← Modelo Character, interfaz CharacterRepository, GetCharactersUseCase
    presentation/ ← CharacterListViewModel, ScreenState
    ui/           ← CharacterListScreen, CharacterCard, LoadingSkeleton, ErrorView, StateDemoBottomSheet
build-logic/
  convention/     ← Plugins Gradle custom: AndroidLibrary, AndroidFeature, Compose, Hilt
```

**Responsabilidades por capa:**

- `data` — llama a la API, implementa interfaces de repositorio, es dueño del `PagingSource`.
- `domain` — Kotlin puro. Define modelos y use cases. Cero dependencias de Android.
- `presentation` — el ViewModel combina múltiples `StateFlow` en un único `ScreenState`. Expone `Flow<PagingData<Character>>` por separado, cacheado con `cachedIn(viewModelScope)`.
- `ui` — Composables sin estado que observan `ScreenState` y `LazyPagingItems`.

**`BaseViewModel`** provee `launchWithErrorHandling {}`, un wrapper de coroutines que re-lanza `CancellationException` (cancelación cooperativa correcta) y redirige cualquier otra excepción a un lambda `onError` opcional, evitando un error común con coroutines.

---

## Manejo de estado

El ViewModel expone dos flujos:

```kotlin
// Flujo de paginación — scroll infinito manejado por la librería
val characters: Flow<PagingData<Character>> = getCharactersUseCase().cachedIn(viewModelScope)

// Estado de UI — 5 flows combinados en un único snapshot
val screenState: StateFlow<ScreenState> = combine(
    _isRefreshing, _screenConfig, _isDemoLoading, _isDemoError, _isDarkTheme
) { ... }.stateIn(SharingStarted.WhileSubscribed(5_000), ScreenState.default())
```

`ScreenState` contiene:

| Campo | Propósito |
|---|---|
| `screenConfig` | Configuración de UI obtenida desde Firebase |
| `isDemoLoading` | Activa el skeleton overlay para demo |
| `isDemoError` | Activa la vista de error para demo |
| `isRefreshing` | Indicador de pull-to-refresh |
| `isDarkTheme` | Controla el tema dark/light de toda la app |

El `LoadState` de Paging (cargando, error, fin de lista) se maneja directamente en el `LazyColumn` via `characters.loadState`, manteniendo el ViewModel libre de boilerplate de paginación.

Desde el FAB se puede abrir un bottom sheet para forzar cualquier estado de forma interactiva:
- **Simular carga** — muestra el shimmer skeleton animado
- **Simular error** — muestra la vista de error con botón Reintentar
- **Restaurar datos** — vuelve a los datos reales
- **Toggle dark/light** — cambia el tema de la app al instante

---

## Networking

`core:network` centraliza toda la lógica de red:

- **Retrofit** con conversor `kotlinx.serialization` (sin dependencia de Gson).
- **OkHttp** configurado con caché de disco de 10 MB, logging a nivel `BODY` solo en debug, y timeouts explícitos (conexión: 15s, lectura: 30s, escritura: 15s).
- `CharacterDto` → `Character` mapeado en `data/mapper/`. La capa de dominio nunca ve DTOs de red.
- `Json` configurado con `ignoreUnknownKeys = true` y `coerceInputValues = true` para ser resiliente ante cambios en la API.

---

## Paginación

Implementada con **Paging 3**:

- `CharacterPagingSource` extiende `PagingSource<Int, Character>`, mapea páginas de la API a `LoadResult.Page` y propaga errores como `LoadResult.Error`.
- `CharacterRepositoryImpl` crea un `Pager` con `pageSize = 20` y `prefetchDistance = 5`, expuesto como `Flow<PagingData<Character>>`.
- `GetCharactersUseCase` es un delegado ligero — el ViewModel cachea el flow con `cachedIn(viewModelScope)` para sobrevivir cambios de configuración.
- La UI usa `collectAsLazyPagingItems()` y maneja el scroll infinito de forma nativa — sin seguimiento manual de páginas.

---

## Carga de imágenes

Imágenes cargadas con **Coil** dentro de `CharacterCard`:

- `crossfade(300ms)` para transiciones suaves en la lista.
- `memoryCacheKey` y `diskCacheKey` definidos explícitamente para evitar entradas duplicadas en caché.
- `placeholder` y `error` usan `ColorPainter` — sin recursos drawable adicionales.
- `ContentScale.Crop` con un slot de imagen fijo de 96 dp garantiza altura consistente en las cards independientemente del aspect ratio.

---

## Server-Driven UI (SDUI)

`core:sdui` obtiene un JSON `ScreenConfig` desde **Firebase Remote Config** al iniciar. Esta configuración controla:

- Título y visibilidad del TopBar
- Shape de imagen en la card (`CIRCLE` / `RECTANGLE`), visibilidad del chip de estado, labels de estado
- Visibilidad y texto del banner
- Título y label de reintento de la vista de error
- Duración de animaciones y número de skeletons de carga
- Labels del bottom sheet de demo

Si la obtención falla (sin red, error de Firebase), se usa un `fallbackConfig()` hardcodeado — la app siempre funciona sin conexión.

---

## Tema dark / light

La app incluye un esquema de color **dark** (espacio sci-fi) y **light** (menta-teal) personalizado. El tema activo se almacena en `ScreenState.isDarkTheme` y se cambia desde el FAB. `MainActivity` pasa `isDarkTheme` a `RickAndMortyTheme(darkTheme = ...)` para que todo el árbol Compose se re-renderice al instante. Todos los colores de los componentes usan `MaterialTheme.colorScheme` — sin valores hardcodeados en la capa UI.

---

## Estructura del proyecto

```
build-logic/convention/     4 plugins Gradle de convención (AndroidLibrary, Compose, Feature, Hilt)
core/network/               RickMortyApi, DTOs, módulo DI de OkHttp/Retrofit
core/ui/                    RickAndMortyTheme, Color, Typography
core/sdui/                  Integración Firebase Remote Config, modelo ScreenConfig + mapper
core/presentation/          BaseViewModel
feature/characters/
  data/paging/              CharacterPagingSource
  data/repository/          CharacterRepositoryImpl
  data/mapper/              CharacterDto → Character
  domain/model/             Character, CharacterStatus
  domain/repository/        CharacterRepository (interfaz)
  domain/usecase/           GetCharactersUseCase
  presentation/state/       ScreenState, ScreenConfigDefaults
  presentation/viewmodel/   CharacterListViewModel
  ui/components/            CharacterCard, LoadingSkeleton, ErrorView, StateDemoBottomSheet
  ui/screen/                CharacterListScreen
  test/                     Tests unitarios e integración (MockK, Turbine, Paging testing)
```

---

## Decisiones técnicas

**Multi-módulo + Convention Plugins** — Los módulos imponen límites estrictos de dependencias (una feature no puede acceder a otra, `domain` no tiene imports de Android). Los convention plugins (`rickandmorty.android.feature`, etc.) eliminan boilerplate en los archivos `build.gradle.kts` y centralizan los defaults del proyecto en un único lugar.

**Paging 3 en lugar de paginación manual** — La abstracción `PagingSource` maneja casos edge (retry, refresh, `getRefreshKey` para restaurar posición tras muerte del proceso) que una implementación manual con `page++` requeriría reimplementar.

**kotlinx.serialization en lugar de Gson** — Seguro en tiempo de compilación, sin reflection, mejor integración con la nulabilidad de Kotlin.

**`StateFlow.combine` para estado de UI** — En lugar de mutaciones anidadas en un `data class`, cada responsabilidad (`isRefreshing`, `screenConfig`, `isDemoLoading`, `isDemoError`, `isDarkTheme`) es un `MutableStateFlow` independiente. `combine` los fusiona reactivamente y la UI observa un único `StateFlow<ScreenState>` — fácil de testear, fácil de extender.

**Firebase Remote Config para SDUI** — Demuestra conocimiento de patrones de producción donde las decisiones de diseño necesitan poder cambiar post-release sin publicar una nueva versión.

---

## Testing

El módulo `feature:characters` tiene tests unitarios y de integración que cubren:

- `CharacterPagingSource` — verificado con `TestPager` de la librería paging-testing, comprobando estructura correcta de `LoadResult.Page` y propagación de errores.
- `GetCharactersUseCase` — verifica la delegación a `CharacterRepository.getCharactersPaged()` usando MockK.
- `CharacterListViewModel` — tests unitarios con `TestCoroutineScheduler` y `UnconfinedTestDispatcher`, verificando transiciones de estado de `isDemoLoading`, `isDemoError` e `isDarkTheme`.
- `CharacterListViewModelIntegrationTest` — usa un `CharacterRepository` fake que devuelve `flowOf(PagingData.from(...))` para testear el flujo completo de inicialización del ViewModel.

---

## Qué no se implementó

- **Navegación** — challenge de una sola pantalla, sin `NavController`.
- **Pantalla de detalle** — fuera del scope.
- **Caché offline con Room** — OkHttp provee caché a nivel HTTP básico pero no hay capa de base de datos.
- **Tests instrumentados / UI tests** — no se configuró suite de Espresso/Compose UI tests.

---

## Qué mejoraría con más tiempo

- **Base de datos Room** como fuente única de verdad, usando `RemoteMediator` con Paging 3 para soporte offline completo.
- **Pantalla de detalle** con transiciones de elementos compartidos.
- **Navigation Compose** con rutas type-safe.
- **Screenshot testing** con Paparazzi para fijar visualmente el tema dark/light.
- **Pipeline de CI** (GitHub Actions) que ejecute lint, tests unitarios y build en cada PR.
- **Baseline Profiles** para mejorar rendimiento de arranque y scroll.

---

## Uso de IA

Se utilizó asistencia de IA (Claude vía Windsurf/Cascade) como herramienta de pair programming durante el desarrollo:

- Scaffolding de la estructura multi-módulo de Gradle y los convention plugins.
- Migración de paginación manual a Paging 3 (`PagingSource`, `Pager`, `cachedIn`).
- Implementación de la capa SDUI con Firebase Remote Config.
- Reescritura de tests unitarios tras el cambio de contrato de API (de `suspend getCharacters(page)` a `getCharactersPaged(): Flow<PagingData>`).
- Restauración de archivos de build de Gradle tras un incidente con `git rm -r --cached` durante una limpieza del historial.

Todas las decisiones arquitectónicas, revisión del código y decisiones finales de implementación fueron tomadas y validadas por el desarrollador. La IA se utilizó como acelerador, no como sustituto del criterio de ingeniería.

---

## Ejecutar el proyecto

```bash
# Compilar
./gradlew assembleDebug

# Tests unitarios
./gradlew :feature:characters:testDebugUnitTest
```

**SDK mínimo:** 24 (Android 7.0)
**SDK objetivo:** 36
**Kotlin:** 2.0.21
**Jetpack Compose BOM:** 2024.09.00
