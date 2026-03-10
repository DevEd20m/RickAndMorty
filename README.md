# Rick & Morty — Android Senior Demo

> Aplicación Android de producción que consume la [Rick and Morty API](https://rickandmortyapi.com/api/character) y demuestra arquitectura limpia, SDUI con Firebase Remote Config, manejo de estados robusto y un diseño dark sci-fi personalizado.

---

## ¿Por qué deberías contratarme?

Este proyecto no es un tutorial ni un CRUD básico. Cada decisión técnica tiene una razón:

- **Arquitectura multi-módulo real** — `core:network`, `core:ui`, `core:sdui`, `core:presentation`, `feature:characters`. Así es como se construyen apps grandes: sin que una feature toque el código de otra.
- **Server-Driven UI (SDUI)** — La pantalla entera es configurable desde Firebase Remote Config sin deployar una nueva versión. Cambias el título, los labels de estado, el shape de la imagen o los textos del panel de demo en tiempo real.
- **BaseViewModel con manejo centralizado de errores** — Nada de `runCatching` suelto en cada ViewModel. Un solo punto de control para coroutines y errores, escalable a cualquier número de features.
- **UseCase que devuelve `Result<T>`** — La capa de dominio comunica éxito/error de forma explícita. El ViewModel usa `Result.fold()` — sin excepciones silenciosas.
- **Paginación infinita sin Paging 3** — Implementada manualmente con `LazyListState` + `snapshotFlow` para demostrar comprensión del mecanismo, no solo uso de librerías.
- **Tests de verdad** — Unit tests con MockK + Turbine, integration tests con fake repository, UI tests con Compose Testing.
- **Diseño propio** — Paleta dark sci-fi extraída de un proyecto de referencia web (React/TSX), adaptada al sistema de colores de Material 3.

---

## Arquitectura

El proyecto sigue **MVVM + Clean Architecture** con separación estricta por capas:

```
┌──────────────────────────────────────────┐
│              Capa UI                     │
│  CharacterListScreen, CharacterCard,     │
│  LoadingSkeleton, ErrorView,             │
│  StateDemoBottomSheet                    │
└────────────────────┬─────────────────────┘
                     │ observa StateFlow
┌────────────────────▼─────────────────────┐
│          Capa de Presentación            │
│  CharacterListViewModel : BaseViewModel  │
│  CharacterUiState (sealed class)         │
│  ScreenState (ui + refresh + sdui)       │
└────────────────────┬─────────────────────┘
                     │ invoca
┌────────────────────▼─────────────────────┐
│            Capa de Dominio               │
│  GetCharactersUseCase → Result<T>        │
│  CharacterRepository (interfaz)          │
│  Character, CharacterStatus (modelos)    │
└────────────────────┬─────────────────────┘
                     │ implementado por
┌────────────────────▼─────────────────────┐
│            Capa de Datos                 │
│  CharacterRepositoryImpl                 │
│  RickMortyApi (Retrofit)                 │
│  CharacterDto → CharacterMapper          │
└──────────────────────────────────────────┘
```

---

## Módulos y decisiones de diseño

### `core:network`
Configura Retrofit + OkHttp con interceptor de logging. Está separado para que cualquier feature pueda consumir la red sin duplicar configuración ni crear dependencias circulares.

### `core:ui`
Contiene el sistema de diseño: `Color.kt`, `Typography.kt`, `Theme.kt`. Al estar en su propio módulo, cualquier feature importa el mismo tema sin duplicar. El dark theme es forzado — paleta inspirada en la estética sci-fi del diseño de referencia.

### `core:sdui`
**La pieza más diferenciadora del proyecto.** Implementa Server-Driven UI:
- `ScreenConfigDto` — deserializa el JSON de Firebase Remote Config
- `ScreenConfigMapper` — convierte DTO → modelo de dominio limpio
- `ScreenConfigRepository` — abstrae la fuente (Firebase o fallback local)
- `DemoConfig`, `CardConfig`, `TopBarConfig`, etc. — cada sección de la pantalla es configurable

> **¿Por qué?** En apps reales, el negocio necesita cambiar textos, flags y comportamiento sin esperar un release. SDUI lo resuelve sin código adicional en el cliente.

### `core:presentation`
Contiene `BaseViewModel` — una clase base que centraliza el lanzamiento de coroutines con manejo de errores. Evita que cada ViewModel repita el mismo bloque `try/catch` o `runCatching`.

```kotlin
fun launchWithErrorHandling(
    onError: (Throwable) -> Unit,
    block: suspend CoroutineScope.() -> Unit
) {
    viewModelScope.launch {
        try { block() }
        catch (e: CancellationException) { throw e } // no swallow
        catch (e: Throwable) { onError(e) }
    }
}
```

> **¿Por qué no `runCatching`?** Porque `runCatching` atrapa `CancellationException`, rompiendo el mecanismo de cancelación de coroutines — un bug sutil en producción.

### `feature:characters`
La única feature actual. Organizada internamente en capas:
- `data/` — repositorio, API, DTOs, mappers
- `domain/` — modelos, interfaz del repositorio, use case
- `presentation/` — ViewModel, estado, defaults
- `ui/` — pantalla, componentes, tema

---

## Manejo de estados

```kotlin
sealed class CharacterUiState {
    object Loading : CharacterUiState()
    data class Success(val characters: ImmutableList<Character>, val hasNextPage: Boolean) : CharacterUiState()
    data class LoadingMore(val characters: ImmutableList<Character>) : CharacterUiState()
    data class Error(val message: String) : CharacterUiState()
}
```

El ViewModel combina tres `StateFlow` en un `ScreenState` mediante `combine()`, garantizando una única fuente de verdad para toda la UI.

### Panel de demo de estados
Presiona el botón **⚙** en la pantalla para abrir el panel y forzar cualquier estado:
- **Simular carga** — shimmer skeleton animado
- **Simular error** — vista de error con botón Reintentar
- **Restaurar datos** — recarga desde página 1

Los textos del panel vienen de Firebase Remote Config (`demo` block en `screen_config`).

---

## SDUI — JSON de Firebase Remote Config

Parámetro: `screen_config`

```json
{
  "topBar": { "title": "Rick & Morty", "visible": true },
  "card": {
    "imageShape": "circle",
    "showStatusChip": true,
    "statusLabels": { "alive": "Vivo", "dead": "Muerto", "unknown": "Desconocido" }
  },
  "errorView": { "title": "Algo salió mal", "retryLabel": "Reintentar" },
  "list": { "skeletonCount": 6, "animationDurationMs": 300 },
  "demo": {
    "title": "Demo de estados",
    "subtitle": "Fuerza un estado de UI para verificar cada escenario.",
    "loadingLabel": "Simular carga",
    "loadingDescription": "Muestra el skeleton animado",
    "restoreLabel": "Restaurar datos",
    "restoreDescription": "Recarga desde la página 1",
    "errorLabel": "Simular error",
    "errorDescription": "Muestra la vista de error con Reintentar"
  }
}
```

---

## Stack técnico

| Librería | Versión | Decisión |
|---|---|---|
| **Jetpack Compose** | BOM 2024.09 | UI declarativa — menos código, mejor gestión de estado |
| **Hilt** | 2.51.1 | DI estándar en Android; integración nativa con Compose y ViewModel |
| **Retrofit + OkHttp** | 2.11 / 4.12 | Cliente HTTP de facto; soporte nativo de coroutines |
| **Kotlinx Serialization** | 1.7.3 | Más rápido que Gson/Moshi con Kotlin; necesario para el módulo SDUI |
| **Coil** | 2.7.0 | Image loader Kotlin-first; `AsyncImage` nativo, caché memoria + disco |
| **Firebase Remote Config** | BoM 33 | SDUI — configuración en tiempo real sin nuevo release |
| **ImmutableCollections** | 0.3.8 | Evita recomposiciones innecesarias en `LazyColumn` con listas grandes |
| **MockK + Turbine** | 1.13 / 1.1 | Testing de coroutines y Flow sin boilerplate |
| **KSP** | 2.0.21 | Procesador de anotaciones; más rápido que KAPT |

---

## Tests

### Unit tests
| Clase | Qué cubre |
|---|---|
| `CharacterListViewModelTest` | Loading → Success → Error → retry → refresh |
| `CharacterRepositoryImplTest` | Mapeo DTO → dominio para los 3 estados; propagación de error |
| `GetCharactersUseCaseTest` | Delegación al repositorio; `Result<T>` correcto |
| `CharacterListViewModelIntegrationTest` | Flujo completo con fake repository (sin mocks) |

### UI tests (Compose)
| Test | Qué cubre |
|---|---|
| Skeleton visible en Loading | Shimmer presente; nombres de personajes ausentes |
| Card renderiza correctamente | Nombre y status visibles |
| Vista de error + Reintentar | Mensaje y botón presentes |
| Callback de retry | Click en botón invoca el lambda |

---

## Estructura de módulos

```
RickAndMorty/
├── app/                          # Entry point, Hilt setup, navegación
├── core/
│   ├── network/                  # Retrofit, OkHttp, interceptores
│   ├── ui/                       # Design system (Color, Theme, Typography)
│   ├── sdui/                     # Server-Driven UI (Firebase RC → dominio)
│   └── presentation/             # BaseViewModel
├── feature/
│   └── characters/
│       ├── data/                 # API, DTOs, mappers, repositorio impl
│       ├── domain/               # Modelos, interfaz repo, use case
│       ├── presentation/         # ViewModel, UiState, ScreenState
│       └── ui/                   # Pantalla, componentes, tema
└── build-logic/                  # Convention plugins de Gradle
```

---

## Ejecutar el proyecto

```bash
# Compilar
./gradlew assembleDebug

# Tests unitarios
./gradlew test

# Tests de UI (requiere emulador o dispositivo)
./gradlew connectedAndroidTest
```

**SDK mínimo:** 24 (Android 7.0)  
**SDK objetivo:** 36  
**Kotlin:** 2.0.21  
**Jetpack Compose BOM:** 2024.09

---

## Qué añadiría con más tiempo

1. **Room + Paging 3** — caché offline con `RemoteMediator`; scroll infinito verdadero
2. **Pantalla de detalle** — perfil completo del personaje con `NavController`
3. **Búsqueda** — `SearchBar` con debounce y filtrado reactivo
4. **CI/CD** — GitHub Actions con lint + tests en cada PR
5. **Crashlytics** — reporte de errores en producción
